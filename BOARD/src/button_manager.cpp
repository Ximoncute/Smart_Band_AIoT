#include "button_manager.h"

#include "config.h"

namespace {
constexpr uint32_t DEBOUNCE_MS = 40;
}

void ButtonManager::begin() {
  pinMode(Config::BUTTON_PIN, INPUT_PULLUP);
  const bool pressed = digitalRead(Config::BUTTON_PIN) == LOW;
  stablePressed_ = pressed;
  lastReading_ = pressed;
  pressedEvent_ = false;
  heldEvent_ = false;
  holdFired_ = false;
  lastChangeMs_ = millis();
}

void ButtonManager::update(uint32_t nowMs) {
  const bool reading = digitalRead(Config::BUTTON_PIN) == LOW;
  if (reading != lastReading_) {
    lastReading_ = reading;
    lastChangeMs_ = nowMs;
  }

  if ((nowMs - lastChangeMs_) >= DEBOUNCE_MS && reading != stablePressed_) {
    stablePressed_ = reading;
    if (stablePressed_) {
      pressedAtMs_ = nowMs;
      holdFired_ = false;
    } else if (!holdFired_) {
      pressedEvent_ = true;
    }
  }
  if (stablePressed_ && !holdFired_ && nowMs - pressedAtMs_ >= 2000) {
    heldEvent_ = true;
    holdFired_ = true;
    pressedEvent_ = false;
  }
}

bool ButtonManager::wasHeld() {
  if (!heldEvent_) return false;
  heldEvent_ = false;
  return true;
}

bool ButtonManager::wasPressed() {
  if (!pressedEvent_) {
    return false;
  }
  pressedEvent_ = false;
  return true;
}
