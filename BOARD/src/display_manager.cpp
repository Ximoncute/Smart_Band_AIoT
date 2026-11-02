#include "display_manager.h"

#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>
#include <Wire.h>

#include "config.h"

namespace {
Adafruit_SSD1306 display(Config::OLED_WIDTH, Config::OLED_HEIGHT, &Wire1, -1);
}

bool DisplayManager::begin() {
  Wire1.begin(Config::OLED_SDA, Config::OLED_SCL);
  Wire1.setClock(400000);
  available_ = display.begin(SSD1306_SWITCHCAPVCC, Config::OLED_ADDRESS);
  reset();
  if (available_) display.display();
  return available_;
}

void DisplayManager::showWelcome() {
  reset();
  center("WELCOME", 2, 1);
  center("AI CRASH", 18, 2);
  center("DETECTION", 42, 2);
  if (available_) display.display();
}

void DisplayManager::showSystemReady() {
  reset();
  center("SYSTEM READY", 8, 1);
  center("NORMAL", 28, 2);
  if (available_) display.display();
}

void DisplayManager::showTimeWeather(const tm& timeInfo, bool timeValid, const WeatherData& weather) {
  reset();
  char line[22];
  if (timeValid) {
    strftime(line, sizeof(line), "%H:%M:%S", &timeInfo);
    center(line, 0, 2);
    strftime(line, sizeof(line), "%d/%m/%Y", &timeInfo);
    center(line, 20, 1);
  } else {
    center("TIME SEARCH", 8, 1);
  }
  if (available_) display.drawLine(0, 32, 127, 32, SSD1306_WHITE);
  if (weather.valid) {
    snprintf(line, sizeof(line), "%.1fC  %s", weather.temperatureC, weatherName(weather.weatherCode));
    center(line, 38, 1);
    snprintf(line, sizeof(line), "RAIN TODAY: %u%%", weather.rainProbability);
    center(line, 52, 1);
  } else {
    const char* status = "WEATHER: WAIT GPS";
    if (weather.status == WeatherStatus::WaitWifi) status = "WEATHER: WIFI";
    if (weather.status == WeatherStatus::Fetching) status = "WEATHER: FETCH";
    if (weather.status == WeatherStatus::Error) status = "WEATHER: ERROR";
    center(status, 44, 1);
  }
  if (available_) display.display();
}

void DisplayManager::showAiLocation(const AiPrediction& prediction, const GpsData& gps) {
  reset();
  char line[24];
  snprintf(line, sizeof(line), "AI:%s", motionClassName(prediction.predictedClass));
  for (char* p = line; *p; ++p) *p = toupper(*p);
  center(line, 0, 2);
  center(gps.locationValid ? "GPS: FIX" : "GPS: SEARCH", 22, 1);
  if (gps.locationValid) {
    snprintf(line, sizeof(line), "LAT: %.5f", gps.latitude);
    center(line, 38, 1);
    snprintf(line, sizeof(line), "LON: %.5f", gps.longitude);
    center(line, 52, 1);
  } else {
    center("NO LOCATION", 46, 1);
  }
  if (available_) display.display();
}

void DisplayManager::showGpsStatus(const GpsData& gps) {
  reset();
  center("GPS STATUS", 0, 1);
  char line[22];
  if (gps.charsProcessed == 0) {
    center("NO NMEA DATA", 19, 1);
    center("GPS TX -> GPIO2", 37, 1);
    center("BAUD: 9600", 52, 1);
  } else {
    snprintf(line, sizeof(line), "NMEA:%lu", gps.charsProcessed);
    center(line, 14, 1);
    snprintf(line, sizeof(line), "SAT:%lu HDOP:%.1f", gps.satellites, gps.hdop);
    center(line, 27, 1);
    snprintf(line, sizeof(line), "SPEED:%.1f KM/H", gps.speedKmh);
    center(line, 40, 1);
    snprintf(line, sizeof(line), "ALT:%.0fM %s", gps.altitudeM, gps.locationValid ? "FIX" : "SEARCH");
    center(line, 53, 1);
  }
  if (available_) display.display();
}

void DisplayManager::showCustom(const char* line1, const char* line2, const char* line3) {
  reset();
  center(line1, 2, 1);
  center(line2, 24, 2);
  center(line3, 52, 1);
  if (available_) display.display();
}

void DisplayManager::showCrashDetected(uint8_t secondsRemaining) {
  reset();
  center("CRASH DETECTED", 0, 1);
  center("ARE YOU OK?", 16, 2);
  char line[4];
  snprintf(line, sizeof(line), "%u", secondsRemaining);
  center(line, 42, 2);
  if (available_) display.display();
}

void DisplayManager::showRecovery() {
  reset();
  center("I'M OK", 12, 2);
  center("ALERT CANCELLED", 42, 1);
  if (available_) display.display();
}

void DisplayManager::showEmergency() {
  reset();
  center("EMERGENCY", 10, 2);
  center("NO RESPONSE", 42, 1);
  if (available_) display.display();
}

void DisplayManager::showSos() {
  reset();
  center("SOS", 10, 2);
  center("BUTTON HELD", 42, 1);
  if (available_) display.display();
}

void DisplayManager::showCalibration() {
  reset();
  center("CALIBRATING", 10, 1);
  center("KEEP STILL", 32, 2);
  if (available_) display.display();
}

void DisplayManager::showCalibrationOk() {
  reset();
  center("CALIBRATION", 12, 1);
  center("OK", 30, 2);
  if (available_) display.display();
}

void DisplayManager::showError(const char* title, const char* detail) {
  reset();
  center(title, 12, 1);
  center(detail, 34, 1);
  if (available_) display.display();
}

void DisplayManager::reset() {
  if (!available_) return;
  display.clearDisplay();
  display.setTextColor(SSD1306_WHITE);
  display.setTextWrap(false);
}

void DisplayManager::center(const char* text, int16_t y, uint8_t size) {
  if (!available_) return;
  int16_t x1, y1;
  uint16_t width, height;
  display.setTextSize(size);
  display.getTextBounds(text, 0, y, &x1, &y1, &width, &height);
  display.setCursor(max(0, (128 - static_cast<int>(width)) / 2), y);
  display.print(text);
}

const char* DisplayManager::weatherName(int code) const {
  if (code == 0) return "CLEAR";
  if (code <= 3) return "CLOUDY";
  if (code <= 48) return "FOG";
  if (code <= 67) return "RAIN";
  if (code <= 77) return "SNOW";
  if (code <= 82) return "SHOWER";
  return "STORM";
}
