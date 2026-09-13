#pragma once

#include <Arduino.h>

#include "system_data.h"

class WeatherManager {
 public:
  void begin();
  void update(const GpsData& gps);
  WeatherData data() const;

 private:
  WeatherData data_;
  uint32_t lastAttemptMs_ = 0;
};
