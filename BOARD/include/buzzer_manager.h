#pragma once

#include <Arduino.h>

enum class BuzzerMode : uint8_t {
  Off,
  CrashDetected,
  UserConfirmation,
  Emergency
};

class BuzzerManager {
 public:
  void begin();
  void setMode(BuzzerMode mode);
  void update(uint32_t nowMs);

 private:
  void write(bool on);

  BuzzerMode mode_ = BuzzerMode::Off;
  uint32_t modeStartMs_ = 0;
  bool outputOn_ = false;
};
