#pragma once

#include <Arduino.h>
#include <Wire.h>

#include "motion_types.h"

class ImuManager {
 public:
  bool begin();
  bool calibrate();
  bool readSample(MotionSample& sample);
  const char* lastError() const;
  uint8_t address() const;

 private:
  bool probeAddress(uint8_t candidateAddress);
  bool writeRegister(uint8_t reg, uint8_t value);
  bool readRegisters(uint8_t startReg, uint8_t* buffer, size_t len);
  int16_t readInt16LE(const uint8_t* buffer, size_t index) const;

  uint8_t address_ = 0;
  float axOffsetG_ = 0.0f;
  float ayOffsetG_ = 0.0f;
  float azOffsetG_ = 0.0f;
  float gxOffsetDps_ = 0.0f;
  float gyOffsetDps_ = 0.0f;
  float gzOffsetDps_ = 0.0f;
  const char* lastError_ = "not initialized";
};
