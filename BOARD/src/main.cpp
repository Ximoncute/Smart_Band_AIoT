#include <Arduino.h>
#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>
#include <NimBLEDevice.h>
#include <Preferences.h>
#include <freertos/FreeRTOS.h>
#include <freertos/semphr.h>
#include <freertos/task.h>

#include "ai_model.h"
#include "button_manager.h"
#include "buzzer_manager.h"
#include "config.h"
#include "crash_detector.h"
#include "display_manager.h"
#include "gps_manager.h"
#include "imu_manager.h"
#include "weather_manager.h"

enum class SystemState : uint8_t {
  Normal,
  PossibleImpact,
  Analyzing,
  UserConfirmation,
  Emergency,
  Sos,
  Recovery
};

struct SharedData {
  MotionSample sample;
  AiPrediction prediction;
  GpsData gps;
  WeatherData weather;
  uint32_t sampleSequence = 0;
};

ImuManager imu;
AiModel aiModel;
GpsManager gpsManager;
WeatherManager weatherManager;
CrashDetector crashDetector;
DisplayManager displayManager;
BuzzerManager buzzerManager;
ButtonManager buttonManager;
SharedData sharedData;
SemaphoreHandle_t dataMutex;

SystemState state = SystemState::Normal;
CrashEvidence evidence;
uint32_t stateEnterMs = 0;
uint32_t confirmationStartMs = 0;
uint32_t handledSampleSequence = 0;
uint32_t datasetStopMs = 0;
uint32_t lastDisplayMs = 0;
uint32_t lastSerialStatusMs = 0;
uint32_t lastTelemetryMs = 0;
uint8_t lastPage = 255;
uint8_t lastCountdown = 255;
volatile bool datasetMode = false;
volatile bool wifiProvisioned = false;
uint32_t alertEventMs = 0;
char datasetLabel[Config::DATASET_LABEL_MAX_LEN + 1] = "normal";
String serialBuffer;

class WifiProvisionCallbacks : public NimBLECharacteristicCallbacks {
  void onWrite(NimBLECharacteristic* characteristic, NimBLEConnInfo&) override {
    JsonDocument json;
    const std::string raw = characteristic->getValue();
    if (deserializeJson(json, raw.c_str()) != DeserializationError::Ok) return;
    if (json["screen"].is<JsonObject>()) {
      Preferences preferences;
      if (!preferences.begin("smartband", false)) return;
      preferences.putString("screen1", (const char*) (json["screen"]["line1"] | ""));
      preferences.putString("screen2", (const char*) (json["screen"]["line2"] | ""));
      preferences.putString("screen3", (const char*) (json["screen"]["line3"] | ""));
      preferences.end();
      wifiProvisioned = true;
      return;
    }
    const char* ssid = json["ssid"] | "";
    const char* password = json["password"] | "";
    const char* backendUrl = json["backend_url"] | "";
    if (!ssid[0]) return;
    Serial.printf("[BLE] Received WiFi provisioning, SSID length=%u\n", strlen(ssid));
    Preferences preferences;
    preferences.begin("smartband", false);
    preferences.putString("ssid", ssid);
    preferences.putString("password", password);
    if (backendUrl[0]) preferences.putString("backend", backendUrl);
    preferences.end();
    Serial.println("[BLE] WiFi credentials written to NVS");
    Serial.printf("[BLE] WiFi credentials saved for SSID: %s\n", ssid);
    // Do not delay or restart from the NimBLE callback task. Doing so can
    // starve IDLE1 and trigger the ESP32 interrupt watchdog. The main loop
    // performs the restart after the callback has returned.
    wifiProvisioned = true;
  }
};

void beginBleProvisioning() {
  NimBLEDevice::init(Config::BLE_DEVICE_NAME);
  NimBLEServer* server = NimBLEDevice::createServer();
  NimBLEService* service = server->createService(Config::BLE_SERVICE_UUID);
  NimBLECharacteristic* wifiCharacteristic = service->createCharacteristic(
      Config::BLE_WIFI_CHAR_UUID,
      NIMBLE_PROPERTY::WRITE | NIMBLE_PROPERTY::WRITE_NR | NIMBLE_PROPERTY::NOTIFY);
  wifiCharacteristic->setCallbacks(new WifiProvisionCallbacks());
  service->start();
  server->start();
  NimBLEAdvertising* advertising = NimBLEDevice::getAdvertising();
  advertising->setName(Config::BLE_DEVICE_NAME);
  advertising->addServiceUUID(Config::BLE_SERVICE_UUID);
  advertising->start();
  Serial.println("[BLE] Provisioning ready: Smartband-001");
}

void printCsv(const MotionSample& sample) {
  Serial.printf("%lu,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%s\n", sample.timestampMs, sample.ax, sample.ay,
                sample.az, sample.gx, sample.gy, sample.gz, datasetLabel);
}

void imuAiTask(void*) {
  TickType_t wake = xTaskGetTickCount();
  for (;;) {
    MotionSample sample;
    if (imu.readSample(sample)) {
      aiModel.addSample(sample);
      AiPrediction prediction;
      xSemaphoreTake(dataMutex, portMAX_DELAY);
      prediction = sharedData.prediction;
      xSemaphoreGive(dataMutex);
      if (aiModel.windowReady()) {
        prediction = aiModel.runInference();
      }
      xSemaphoreTake(dataMutex, portMAX_DELAY);
      sharedData.sample = sample;
      sharedData.prediction = prediction;
      ++sharedData.sampleSequence;
      xSemaphoreGive(dataMutex);
      if (datasetMode) {
        printCsv(sample);
      }
    }
    vTaskDelayUntil(&wake, pdMS_TO_TICKS(Config::SAMPLE_INTERVAL_MS));
  }
}

void gpsTask(void*) {
  for (;;) {
    gpsManager.update();
    xSemaphoreTake(dataMutex, portMAX_DELAY);
    sharedData.gps = gpsManager.data();
    xSemaphoreGive(dataMutex);
    vTaskDelay(pdMS_TO_TICKS(10));
  }
}

void weatherTask(void*) {
  for (;;) {
    GpsData gps;
    xSemaphoreTake(dataMutex, portMAX_DELAY);
    gps = sharedData.gps;
    xSemaphoreGive(dataMutex);
    weatherManager.update(gps);
    xSemaphoreTake(dataMutex, portMAX_DELAY);
    sharedData.weather = weatherManager.data();
    xSemaphoreGive(dataMutex);
    vTaskDelay(pdMS_TO_TICKS(1000));
  }
}

void enterState(SystemState next) {
  state = next;
  stateEnterMs = millis();
  lastPage = 255;
  lastDisplayMs = 0;
  if (state == SystemState::Normal || state == SystemState::PossibleImpact || state == SystemState::Analyzing ||
      state == SystemState::Recovery) {
    buzzerManager.setMode(BuzzerMode::Off);
  }
  if (state == SystemState::UserConfirmation) {
    confirmationStartMs = millis();
    lastCountdown = 255;
    buttonManager.wasPressed();
    buzzerManager.setMode(BuzzerMode::UserConfirmation);
    displayManager.showCrashDetected(15);
  }
  if (state == SystemState::Emergency || state == SystemState::Sos) {
    alertEventMs = millis();
    lastTelemetryMs = millis() - Config::TELEMETRY_INTERVAL_MS;
    buzzerManager.setMode(BuzzerMode::Emergency);
    if (state == SystemState::Sos) displayManager.showSos();
    else displayManager.showEmergency();
  }
  if (state == SystemState::Recovery) displayManager.showRecovery();
}

void startDataset(const String& label, uint32_t durationMs) {
  label.toCharArray(datasetLabel, sizeof(datasetLabel));
  datasetStopMs = millis() + durationMs;
  datasetMode = true;
}

void processCommand(String command) {
  command.trim();
  command.toLowerCase();
  if (command == "dataset stop") {
    datasetMode = false;
    return;
  }
  if (command.startsWith("dataset ")) {
    const int separator = command.indexOf(' ', 8);
    if (separator < 0) return;
    const String label = command.substring(8, separator);
    const uint32_t duration = command.substring(separator + 1).toInt();
    if ((label == "normal" || label == "impact" || label == "crash") && duration >= 1000) startDataset(label, duration);
    return;
  }
  if (command == "test crash") enterState(SystemState::UserConfirmation);
  if (command == "test emergency") enterState(SystemState::Emergency);
  if (command == "test sos") {
    enterState(SystemState::Sos);
  }
  if (command == "test normal") enterState(SystemState::Normal);
}

void readSerial() {
  while (Serial.available()) {
    const char c = Serial.read();
    if (c == '\n' || c == '\r') {
      if (serialBuffer.length()) processCommand(serialBuffer);
      serialBuffer = "";
    } else if (serialBuffer.length() < 80) {
      serialBuffer += c;
    }
  }
}

const char* stateName() {
  switch (state) {
    case SystemState::Normal: return "NORMAL";
    case SystemState::PossibleImpact: return "POSSIBLE_IMPACT";
    case SystemState::Analyzing: return "ANALYZING";
    case SystemState::UserConfirmation: return "ARE_YOU_OK";
    case SystemState::Emergency: return "EMERGENCY";
    case SystemState::Sos: return "SOS";
    case SystemState::Recovery: return "RECOVERY";
  }
  return "UNKNOWN";
}

const char* weatherStatusName(WeatherStatus status) {
  switch (status) {
    case WeatherStatus::WaitGps: return "WAIT GPS";
    case WeatherStatus::WaitWifi: return "WAIT WIFI";
    case WeatherStatus::Fetching: return "FETCHING";
    case WeatherStatus::Ready: return "READY";
    case WeatherStatus::Error: return "ERROR";
  }
  return "UNKNOWN";
}

const char* weatherCondition(int code) {
  if (code == 0) return "CLEAR";
  if (code <= 3) return "CLOUDY";
  if (code <= 48) return "FOG";
  if (code <= 67) return "RAIN";
  if (code <= 77) return "SNOW";
  if (code <= 82) return "SHOWER";
  return "STORM";
}

void postTelemetry(uint32_t now, const SharedData& data) {
  if (now - lastTelemetryMs < Config::TELEMETRY_INTERVAL_MS || WiFi.status() != WL_CONNECTED) return;
  lastTelemetryMs = now;
  JsonDocument json;
  json["device_id"] = Config::DEVICE_ID;
  json["timestamp_ms"] = now;
  json["state"] = stateName();
  json["alert_type"] = state == SystemState::Sos ? "SOS" : state == SystemState::Emergency ? "FALL" : "";
  json["alert_event_ms"] = alertEventMs;
  json["ai"] = motionClassName(data.prediction.predictedClass);
  json["ai_normal"] = data.prediction.normal;
  json["ai_impact"] = data.prediction.impact;
  json["ai_crash"] = data.prediction.crash;
  json["gps"]["valid"] = data.gps.locationValid;
  json["gps"]["lat"] = data.gps.latitude;
  json["gps"]["lon"] = data.gps.longitude;
  json["gps"]["speed_kmh"] = data.gps.speedKmh;
  json["gps"]["satellites"] = data.gps.satellites;
  json["gps"]["nmea"] = data.gps.charsProcessed;
  json["gps"]["hdop"] = data.gps.hdop;
  json["gps"]["altitude_m"] = data.gps.altitudeM;
  json["wifi_rssi"] = WiFi.RSSI();
  json["wifi_ssid"] = WiFi.SSID();
  // The app mirrors this payload; it is generated from the same SharedData
  // used by DisplayManager, so the app and the physical OLED stay consistent.
  json["display"]["state"] = stateName();
  json["display"]["ai"] = motionClassName(data.prediction.predictedClass);
  json["display"]["gps_valid"] = data.gps.locationValid;
  json["display"]["lat"] = data.gps.latitude;
  json["display"]["lon"] = data.gps.longitude;
  json["display"]["satellites"] = data.gps.satellites;
  json["display"]["weather_valid"] = data.weather.valid;
  json["display"]["temperature_c"] = data.weather.temperatureC;
  json["display"]["weather_code"] = data.weather.weatherCode;
  json["display"]["rain_probability"] = data.weather.rainProbability;
  json["display"]["page"] = lastPage;
  Preferences screenPreferences;
  screenPreferences.begin("smartband", true);
  json["display"]["custom_line1"] = screenPreferences.getString("screen1", "");
  json["display"]["custom_line2"] = screenPreferences.getString("screen2", "");
  json["display"]["custom_line3"] = screenPreferences.getString("screen3", "");
  screenPreferences.end();
  tm displayTime{};
  if (getLocalTime(&displayTime, 0)) {
    char timeText[20];
    strftime(timeText, sizeof(timeText), "%H:%M:%S", &displayTime);
    json["display"]["time"] = timeText;
    strftime(timeText, sizeof(timeText), "%d/%m/%Y", &displayTime);
    json["display"]["date"] = timeText;
  }
  String body;
  serializeJson(json, body);
  HTTPClient http;
  WiFiClient client;
  Preferences preferences;
  preferences.begin("smartband", true);
  const String backendUrl = preferences.isKey("backend") ? preferences.getString("backend", "") : "";
  preferences.end();
  if (!backendUrl.length()) return;
  if (http.begin(client, backendUrl + "/api/v1/telemetry")) {
    http.addHeader("Content-Type", "application/json");
    http.setTimeout(1500);
    http.POST(body);
    http.end();
  }
}

void printFullStatus(uint32_t now, const SharedData& data) {
  if (datasetMode) return;
  if (now - lastSerialStatusMs < Config::SERIAL_STATUS_MS) return;
  lastSerialStatusMs = now;
  const bool wifiConnected = WiFi.status() == WL_CONNECTED;
  tm timeInfo{};
  const bool timeValid = getLocalTime(&timeInfo, 0);
  char timeText[12] = "SEARCH";
  char dateText[12] = "SEARCH";
  if (timeValid) {
    strftime(timeText, sizeof(timeText), "%H:%M:%S", &timeInfo);
    strftime(dateText, sizeof(dateText), "%d/%m/%Y", &timeInfo);
  }

  Serial.println("========== AIoT STATUS ==========");
  Serial.printf("STATE    : %s\n", stateName());
  Serial.printf("AI       : %s\n", motionClassName(data.prediction.predictedClass));
  Serial.printf("NORMAL   : %.3f\n", data.prediction.normal);
  Serial.printf("IMPACT   : %.3f\n", data.prediction.impact);
  Serial.printf("CRASH    : %.3f\n", data.prediction.crash);
  Serial.printf("WIFI     : %s\n", wifiConnected ? "CONNECTED" : "DISCONNECTED");
  Serial.printf("SSID     : %s\n", WiFi.SSID().c_str());
  Serial.printf("RSSI     : %d dBm\n", wifiConnected ? WiFi.RSSI() : 0);
  Serial.print("IP       : ");
  Serial.println(wifiConnected ? WiFi.localIP().toString() : "0.0.0.0");
  Serial.println("TX POWER : 8.5 dBm");
  Serial.printf("GPS      : %s\n", data.gps.charsProcessed == 0 ? "NO NMEA" : data.gps.locationValid ? "FIX" : "SEARCH");
  Serial.printf("NMEA     : %lu\n", data.gps.charsProcessed);
  Serial.printf("SAT      : %lu\n", data.gps.satellites);
  Serial.printf("HDOP     : %.1f\n", data.gps.hdop);
  Serial.printf("LAT      : %.6f\n", data.gps.latitude);
  Serial.printf("LON      : %.6f\n", data.gps.longitude);
  Serial.printf("SPEED    : %.1f km/h\n", data.gps.speedKmh);
  Serial.printf("ALT      : %.1f m\n", data.gps.altitudeM);
  Serial.printf("WEATHER  : %s\n", weatherStatusName(data.weather.status));
  Serial.printf("TEMP     : %.1f C\n", data.weather.temperatureC);
  Serial.printf("RAIN     : %u %%\n", data.weather.rainProbability);
  Serial.printf("CONDITION: %s\n", data.weather.valid ? weatherCondition(data.weather.weatherCode) : "UNKNOWN");
  Serial.printf("TIME     : %s\n", timeText);
  Serial.printf("DATE     : %s\n", dateText);
  Serial.println("=================================");
}

void updateStateMachine(uint32_t now, const MotionSample& sample, const AiPrediction& prediction) {
  switch (state) {
    case SystemState::Normal:
      if (crashDetector.shouldStartAnalysis(sample, prediction)) {
        crashDetector.beginAnalysis(sample, prediction);
        enterState(SystemState::PossibleImpact);
      }
      break;
    case SystemState::PossibleImpact:
      crashDetector.updateAnalysis(sample, prediction);
      if (now - stateEnterMs >= Config::POSSIBLE_IMPACT_SCREEN_MS) enterState(SystemState::Analyzing);
      break;
    case SystemState::Analyzing:
      crashDetector.updateAnalysis(sample, prediction);
      if (crashDetector.analysisReady(now)) {
        evidence = crashDetector.evidence();
        if (crashDetector.crashConfirmed()) {
          enterState(SystemState::UserConfirmation);
        } else {
          crashDetector.reset();
          enterState(SystemState::Normal);
        }
      }
      break;
    case SystemState::UserConfirmation:
      if (buttonManager.wasPressed()) {
        crashDetector.reset();
        enterState(SystemState::Recovery);
      } else if (now - confirmationStartMs >= Config::CONFIRMATION_TIMEOUT_MS) {
        enterState(SystemState::Emergency);
      }
      break;
    case SystemState::Emergency:
    case SystemState::Sos:
      if (buttonManager.wasPressed()) {
        crashDetector.reset();
        enterState(SystemState::Recovery);
      }
      break;
    case SystemState::Recovery:
      if (now - stateEnterMs >= Config::RECOVERY_SCREEN_MS) enterState(SystemState::Normal);
      break;
  }
}

void showNormalPages(uint32_t now, const SharedData& data) {
  Preferences preferences;
  preferences.begin("smartband", true);
  const String custom1 = preferences.getString("screen1", "");
  const String custom2 = preferences.getString("screen2", "");
  const String custom3 = preferences.getString("screen3", "");
  preferences.end();
  const uint8_t page = (now / Config::OLED_PAGE_MS) % (custom1.length() ? 4 : 3);
  if (page == lastPage && now - lastDisplayMs < Config::DISPLAY_REFRESH_MS) return;
  lastPage = page;
  lastDisplayMs = now;
  if (page == 0) {
    tm timeInfo{};
    const bool valid = getLocalTime(&timeInfo, 0);
    displayManager.showTimeWeather(timeInfo, valid, data.weather);
  } else if (page == 1) {
    displayManager.showAiLocation(data.prediction, data.gps);
  } else if (page == 2) {
    displayManager.showGpsStatus(data.gps);
  } else {
    displayManager.showCustom(custom1.c_str(), custom2.c_str(), custom3.c_str());
  }
}

void updateDisplay(uint32_t now, const SharedData& data) {
  if (state == SystemState::Normal || state == SystemState::PossibleImpact || state == SystemState::Analyzing) {
    showNormalPages(now, data);
    return;
  }
  if (state == SystemState::UserConfirmation) {
    const uint32_t elapsed = now - confirmationStartMs;
    const uint32_t remaining = elapsed >= Config::CONFIRMATION_TIMEOUT_MS ? 0 : Config::CONFIRMATION_TIMEOUT_MS - elapsed;
    const uint8_t seconds = (remaining + 999) / 1000;
    if (seconds != lastCountdown) {
      lastCountdown = seconds;
      displayManager.showCrashDetected(seconds);
    }
  }
}

void setup() {
  Serial.begin(Config::SERIAL_BAUD);
  delay(500);
  buzzerManager.begin();
  buttonManager.begin();
  Serial.println("[BOOT] OLED");
  displayManager.begin();
  displayManager.showWelcome();
  delay(1000);
  Serial.println("[BOOT] BMI160");
  if (!imu.begin()) {
    Serial.println("[ERROR] BMI160");
    displayManager.showError("BMI160 ERROR", "CHECK WIRING");
    for (;;) vTaskDelay(pdMS_TO_TICKS(1000));
  }
  Serial.println("[BOOT] CALIBRATION");
  displayManager.showCalibration();
  imu.calibrate();
  displayManager.showCalibrationOk();
  delay(500);
  Serial.println("[BOOT] AI");
  aiModel.begin();
  Serial.println("[BOOT] GPS");
  gpsManager.begin();
  Serial.println("[BOOT] WIFI");
  beginBleProvisioning();
  weatherManager.begin();
  dataMutex = xSemaphoreCreateMutex();
  displayManager.showSystemReady();
  xTaskCreatePinnedToCore(imuAiTask, "IMU_AI", 6144, nullptr, 2, nullptr, 1);
  xTaskCreatePinnedToCore(gpsTask, "GPS", 4096, nullptr, 2, nullptr, 0);
  xTaskCreatePinnedToCore(weatherTask, "WEATHER", 8192, nullptr, 1, nullptr, 0);
  Serial.println("[BOOT] TASKS READY");
}

void loop() {
  const uint32_t now = millis();
  readSerial();
  if (wifiProvisioned) {
    Serial.println("[BLE] Provisioning complete; restarting safely");
    delay(100);
    ESP.restart();
  }
  buttonManager.update(now);
  if (buttonManager.wasHeld()) {
    enterState(SystemState::Sos);
    Serial.println("[SOS] Long press emergency");
  }
  buzzerManager.update(now);
  if (datasetMode && static_cast<int32_t>(now - datasetStopMs) >= 0) datasetMode = false;

  SharedData data;
  xSemaphoreTake(dataMutex, portMAX_DELAY);
  data = sharedData;
  xSemaphoreGive(dataMutex);
  if (data.sampleSequence != handledSampleSequence) {
    handledSampleSequence = data.sampleSequence;
    updateStateMachine(now, data.sample, data.prediction);
  } else if (state == SystemState::UserConfirmation || state == SystemState::Emergency || state == SystemState::Sos || state == SystemState::Recovery) {
    updateStateMachine(now, data.sample, data.prediction);
  }
  updateDisplay(now, data);
  postTelemetry(now, data);
  printFullStatus(now, data);
  delay(1);
}
