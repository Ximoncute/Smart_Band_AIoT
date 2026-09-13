#pragma once

#include <Arduino.h>

class ButtonManager {
 public:
  void begin();
  void update(uint32_t nowMs);
  bool wasPressed();

 private:
  bool stablePressed_ = false;
  bool lastReading_ = false;
  bool pressedEvent_ = false;
  uint32_t lastChangeMs_ = 0;
};
