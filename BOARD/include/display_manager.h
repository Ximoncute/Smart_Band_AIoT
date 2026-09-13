#pragma once

#include <Arduino.h>
#include <time.h>

#include "motion_types.h"
#include "system_data.h"

class DisplayManager {
 public:
  bool begin();
  void showWelcome();
  void showSystemReady();
  void showTimeWeather(const tm& timeInfo, bool timeValid, const WeatherData& weather);
  void showAiLocation(const AiPrediction& prediction, const GpsData& gps);
  void showGpsStatus(const GpsData& gps);
  void showCrashDetected(uint8_t secondsRemaining);
  void showRecovery();
  void showEmergency();
  void showCalibration();
  void showCalibrationOk();
  void showError(const char* title, const char* detail);

 private:
  void reset();
  void center(const char* text, int16_t y, uint8_t size);
  const char* weatherName(int code) const;

  bool available_ = false;
};
