#pragma once

#include "motion_types.h"

struct CrashEvidence {
  uint8_t aiScore = 0;
  uint8_t impactScore = 0;
  uint8_t rotationScore = 0;
  uint8_t orientationScore = 0;
  uint8_t inactivityScore = 0;
  uint8_t totalScore = 0;
  float peakAccelerationG = 0.0f;
  float peakGyroDps = 0.0f;
  float orientationDeltaDeg = 0.0f;
  float inactivityRatio = 0.0f;
};

class CrashDetector {
 public:
  void reset();
  bool shouldStartAnalysis(const MotionSample& sample, const AiPrediction& prediction) const;
  void beginAnalysis(const MotionSample& sample, const AiPrediction& prediction);
  void updateAnalysis(const MotionSample& sample, const AiPrediction& prediction);
  bool analysisReady(uint32_t nowMs) const;
  CrashEvidence evidence() const;
  bool crashConfirmed() const;

 private:
  uint8_t weightedScore(float value01, uint8_t weight) const;

  bool analyzing_ = false;
  uint32_t eventStartMs_ = 0;
  float startPitchDeg_ = 0.0f;
  float startRollDeg_ = 0.0f;
  float peakAccelerationG_ = 0.0f;
  float peakGyroDps_ = 0.0f;
  float orientationDeltaDeg_ = 0.0f;
  uint32_t sampleCount_ = 0;
  uint32_t inactiveCount_ = 0;
  float bestAiCrashProbability_ = 0.0f;
  float bestAiImpactProbability_ = 0.0f;
};
