#pragma once

#include <Arduino.h>
#include <TinyGPS++.h>

#include "system_data.h"

class GpsManager {
 public:
  void begin();
  void update();
 GpsData data() const;

 private:
  TinyGPSPlus gps_;
  GpsData data_;
};
