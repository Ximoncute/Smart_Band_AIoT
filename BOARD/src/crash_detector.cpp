#include "crash_detector.h"

#include <Arduino.h>

#include "config.h"
#include "signal_processing.h"

void CrashDetector::reset() {
  analyzing_ = false;
  eventStartMs_ = 0;
  startPitchDeg_ = 0.0f;
  startRollDeg_ = 0.0f;
  peakAccelerationG_ = 0.0f;
  peakGyroDps_ = 0.0f;
  orientationDeltaDeg_ = 0.0f;
  sampleCount_ = 0;
  inactiveCount_ = 0;
  bestAiCrashProbability_ = 0.0f;
  bestAiImpactProbability_ = 0.0f;
}

bool CrashDetector::shouldStartAnalysis(const MotionSample& sample, const AiPrediction& prediction) const {
  if (sample.linearAccelerationG >= Config::POSSIBLE_IMPACT_LINEAR_G) {
    return true;
  }
  if (sample.gyroMagnitudeDps >= Config::POSSIBLE_IMPACT_GYRO_DPS) {
    return true;
  }
  return prediction.impact >= Config::AI_SUSPECT_PROBABILITY || prediction.crash >= Config::AI_SUSPECT_PROBABILITY;
}

void CrashDetector::beginAnalysis(const MotionSample& sample, const AiPrediction& prediction) {
  reset();
  analyzing_ = true;
  eventStartMs_ = sample.timestampMs;
  startPitchDeg_ = sample.pitchDeg;
  startRollDeg_ = sample.rollDeg;
  updateAnalysis(sample, prediction);
}

void CrashDetector::updateAnalysis(const MotionSample& sample, const AiPrediction& prediction) {
  if (!analyzing_) {
    return;
  }

  ++sampleCount_;
  peakAccelerationG_ = max(peakAccelerationG_, sample.linearAccelerationG);
  peakGyroDps_ = max(peakGyroDps_, sample.gyroMagnitudeDps);
  orientationDeltaDeg_ = max(orientationDeltaDeg_, SignalProcessing::orientationDeltaDeg(
                                                    startPitchDeg_, startRollDeg_, sample.pitchDeg, sample.rollDeg));
  bestAiCrashProbability_ = max(bestAiCrashProbability_, prediction.crash);
  bestAiImpactProbability_ = max(bestAiImpactProbability_, prediction.impact);

  const bool postImpactStable = sample.timestampMs > eventStartMs_ + 700 &&
                                sample.linearAccelerationG <= Config::INACTIVITY_LINEAR_G &&
                                sample.gyroMagnitudeDps <= Config::INACTIVITY_GYRO_DPS;
  if (postImpactStable) {
    ++inactiveCount_;
  }
}

bool CrashDetector::analysisReady(uint32_t nowMs) const {
  return analyzing_ && (nowMs - eventStartMs_) >= Config::POST_IMPACT_ANALYSIS_MS;
}

CrashEvidence CrashDetector::evidence() const {
  CrashEvidence result;
  const float inactivityRatio = sampleCount_ == 0 ? 0.0f : static_cast<float>(inactiveCount_) / static_cast<float>(sampleCount_);
  const float aiEvidence = max(bestAiCrashProbability_, bestAiImpactProbability_ * 0.55f);
  const float impactEvidence = SignalProcessing::mapToScore(peakAccelerationG_, Config::POSSIBLE_IMPACT_LINEAR_G, Config::IMPACT_THRESHOLD_G);
  const float rotationEvidence = SignalProcessing::mapToScore(peakGyroDps_, Config::POSSIBLE_IMPACT_GYRO_DPS, Config::GYRO_THRESHOLD_DPS);
  const float orientationEvidence = SignalProcessing::mapToScore(orientationDeltaDeg_, Config::ORIENTATION_THRESHOLD_DEG * 0.4f, Config::ORIENTATION_THRESHOLD_DEG);
  const float inactivityEvidence = SignalProcessing::clamp01(inactivityRatio * 2.0f);

  result.aiScore = weightedScore(aiEvidence, Config::WEIGHT_AI);
  result.impactScore = weightedScore(impactEvidence, Config::WEIGHT_IMPACT);
  result.rotationScore = weightedScore(rotationEvidence, Config::WEIGHT_ROTATION);
  result.orientationScore = weightedScore(orientationEvidence, Config::WEIGHT_ORIENTATION);
  result.inactivityScore = weightedScore(inactivityEvidence, Config::WEIGHT_INACTIVITY);
  result.totalScore = result.aiScore + result.impactScore + result.rotationScore + result.orientationScore + result.inactivityScore;
  result.peakAccelerationG = peakAccelerationG_;
  result.peakGyroDps = peakGyroDps_;
  result.orientationDeltaDeg = orientationDeltaDeg_;
  result.inactivityRatio = inactivityRatio;
  return result;
}

bool CrashDetector::crashConfirmed() const {
  const CrashEvidence currentEvidence = evidence();
  return currentEvidence.totalScore >= Config::CRASH_SCORE_THRESHOLD &&
         peakAccelerationG_ >= Config::POSSIBLE_IMPACT_LINEAR_G &&
         (bestAiCrashProbability_ >= Config::AI_CRASH_PROBABILITY ||
          peakGyroDps_ >= Config::GYRO_THRESHOLD_DPS ||
          orientationDeltaDeg_ >= Config::ORIENTATION_THRESHOLD_DEG);
}

uint8_t CrashDetector::weightedScore(float value01, uint8_t weight) const {
  const float bounded = SignalProcessing::clamp01(value01);
  return static_cast<uint8_t>(roundf(bounded * static_cast<float>(weight)));
}
