#include "signal_processing.h"

#include <Arduino.h>
#include <math.h>

namespace SignalProcessing {

float magnitude3(float x, float y, float z) {
  return sqrtf((x * x) + (y * y) + (z * z));
}

float clamp01(float value) {
  if (value < 0.0f) {
    return 0.0f;
  }
  if (value > 1.0f) {
    return 1.0f;
  }
  return value;
}

float mapToScore(float value, float low, float high) {
  if (high <= low) {
    return value >= high ? 1.0f : 0.0f;
  }
  return clamp01((value - low) / (high - low));
}

void enrichSample(MotionSample& sample) {
  sample.accelerationMagnitudeG = magnitude3(sample.ax, sample.ay, sample.az);
  sample.linearAccelerationG = fabsf(sample.accelerationMagnitudeG - 1.0f);
  sample.gyroMagnitudeDps = magnitude3(sample.gx, sample.gy, sample.gz);

  sample.pitchDeg = atan2f(-sample.ax, sqrtf((sample.ay * sample.ay) + (sample.az * sample.az))) * RAD_TO_DEG;
  sample.rollDeg = atan2f(sample.ay, sample.az) * RAD_TO_DEG;
}

float orientationDeltaDeg(float pitchA, float rollA, float pitchB, float rollB) {
  const float pitchDelta = fabsf(pitchB - pitchA);
  const float rollDelta = fabsf(rollB - rollA);
  return sqrtf((pitchDelta * pitchDelta) + (rollDelta * rollDelta));
}

}
