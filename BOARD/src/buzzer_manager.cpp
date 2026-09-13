#include "buzzer_manager.h"

#include "config.h"

void BuzzerManager::begin() {
  pinMode(Config::BUZZER_PIN, OUTPUT);
  write(false);
}

void BuzzerManager::setMode(BuzzerMode mode) {
  if (mode_ == mode) {
    return;
  }
  mode_ = mode;
  modeStartMs_ = millis();
  write(false);
}

void BuzzerManager::update(uint32_t nowMs) {
  bool on = false;
  const uint32_t elapsed = nowMs - modeStartMs_;

  switch (mode_) {
    case BuzzerMode::Off:
      on = false;
      break;
    case BuzzerMode::CrashDetected:
      on = (elapsed % 450UL) < 220UL;
      break;
    case BuzzerMode::UserConfirmation:
      on = (elapsed % 1000UL) < 120UL || (elapsed % 1000UL > 240UL && elapsed % 1000UL < 360UL);
      break;
    case BuzzerMode::Emergency:
      on = (elapsed % 750UL) < 600UL;
      break;
  }

  write(on);
}

void BuzzerManager::write(bool on) {
  if (outputOn_ == on) {
    return;
  }
  outputOn_ = on;
  digitalWrite(Config::BUZZER_PIN, on ? HIGH : LOW);
}
