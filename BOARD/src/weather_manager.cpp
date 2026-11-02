#include "weather_manager.h"

#include <ArduinoJson.h>
#include <HTTPClient.h>
#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <Preferences.h>

#include "config.h"

void WeatherManager::begin() {
  WiFi.mode(WIFI_STA);
  WiFi.setTxPower(WIFI_POWER_8_5dBm);
  Preferences preferences;
  // Open read/write so the namespace is created on a fresh device.
  preferences.begin("smartband", false);
  String ssid = preferences.isKey("ssid") ? preferences.getString("ssid", "") : "";
  String password = preferences.isKey("password") ? preferences.getString("password", "") : "";
  preferences.end();
  if (ssid.length()) WiFi.begin(ssid.c_str(), password.c_str());
  configTime(7 * 3600, 0, "pool.ntp.org", "time.google.com");
}

void WeatherManager::update(const GpsData& gps) {
  const uint32_t now = millis();
  const uint32_t interval = data_.valid ? Config::WEATHER_REFRESH_MS : Config::WIFI_RETRY_MS;
  if (!gps.locationValid) {
    data_.status = WeatherStatus::WaitGps;
    return;
  }
  if (lastAttemptMs_ != 0 && now - lastAttemptMs_ < interval) return;
  lastAttemptMs_ = now;
  if (WiFi.status() != WL_CONNECTED) {
    data_.status = WeatherStatus::WaitWifi;
    WiFi.disconnect();
    Preferences preferences;
    preferences.begin("smartband", false);
    String ssid = preferences.isKey("ssid") ? preferences.getString("ssid", "") : "";
    String password = preferences.isKey("password") ? preferences.getString("password", "") : "";
    preferences.end();
    if (ssid.length()) WiFi.begin(ssid.c_str(), password.c_str());
    return;
  }
  data_.status = WeatherStatus::Fetching;

  String url = "https://api.open-meteo.com/v1/forecast?latitude=";
  url += String(gps.latitude, 6);
  url += "&longitude=";
  url += String(gps.longitude, 6);
  url += "&current=temperature_2m,weather_code&daily=precipitation_probability_max&forecast_days=1&timezone=Asia%2FHo_Chi_Minh";

  HTTPClient http;
  WiFiClientSecure client;
  client.setInsecure();
  http.setTimeout(5000);
  if (!http.begin(client, url)) {
    data_.status = WeatherStatus::Error;
    return;
  }
  const int status = http.GET();
  if (status == HTTP_CODE_OK) {
    JsonDocument json;
    if (deserializeJson(json, http.getString()) == DeserializationError::Ok) {
      data_.temperatureC = json["current"]["temperature_2m"] | 0.0f;
      data_.weatherCode = json["current"]["weather_code"] | 0;
      data_.rainProbability = json["daily"]["precipitation_probability_max"][0] | 0;
      data_.valid = true;
      data_.status = WeatherStatus::Ready;
    }
  }
  if (!data_.valid) data_.status = WeatherStatus::Error;
  http.end();
}

WeatherData WeatherManager::data() const {
  return data_;
}
