#pragma once

#include <Arduino.h>

enum class MotionClass : uint8_t {
  Normal = 0,
  Impact = 1,
  Crash = 2
};

struct MotionSample {
  uint32_t timestampMs = 0;
  float ax = 0.0f;
  float ay = 0.0f;
  float az = 0.0f;
  float gx = 0.0f;
  float gy = 0.0f;
  float gz = 0.0f;
  float accelerationMagnitudeG = 0.0f;
  float linearAccelerationG = 0.0f;
  float gyroMagnitudeDps = 0.0f;
  float pitchDeg = 0.0f;
  float rollDeg = 0.0f;
};

struct AiPrediction {
  float normal = 1.0f;
  float impact = 0.0f;
  float crash = 0.0f;
  MotionClass predictedClass = MotionClass::Normal;
  uint32_t inferenceTimeUs = 0;
  bool trainedModel = false;
};

inline const char* motionClassName(MotionClass motionClass) {
  switch (motionClass) {
    case MotionClass::Normal:
      return "normal";
    case MotionClass::Impact:
      return "impact";
    case MotionClass::Crash:
      return "crash";
  }
  return "unknown";
}
