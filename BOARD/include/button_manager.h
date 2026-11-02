#pragma once

#include <Arduino.h>

class ButtonManager {
 public:
  void begin();
  void update(uint32_t nowMs);
  bool wasPressed();
  bool wasHeld();

 private:
  bool stablePressed_ = false;
  bool lastReading_ = false;
  bool pressedEvent_ = false;
  bool heldEvent_ = false;
  bool holdFired_ = false;
  uint32_t pressedAtMs_ = 0;
  uint32_t lastChangeMs_ = 0;
};
