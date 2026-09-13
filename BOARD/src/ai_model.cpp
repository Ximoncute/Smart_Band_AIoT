#include "ai_model.h"

#include <Arduino.h>
#include <math.h>

#include "signal_processing.h"

#if __has_include("generated_model.h")
#include "generated_model.h"
#else
#define AI_MODEL_HAS_TRAINED_WEIGHTS 0
constexpr int GENERATED_FEATURE_COUNT = 8;
constexpr int GENERATED_CLASS_COUNT = 3;
const float GENERATED_FEATURE_MEAN[GENERATED_FEATURE_COUNT] = {0};
const float GENERATED_FEATURE_SCALE[GENERATED_FEATURE_COUNT] = {1, 1, 1, 1, 1, 1, 1, 1};
const float GENERATED_WEIGHTS[GENERATED_CLASS_COUNT][GENERATED_FEATURE_COUNT] = {{0}};
const float GENERATED_BIAS[GENERATED_CLASS_COUNT] = {0};
#endif

namespace {
constexpr uint8_t FEATURE_ACC_MEAN = 0;
constexpr uint8_t FEATURE_ACC_STD = 1;
constexpr uint8_t FEATURE_ACC_PEAK_LINEAR = 2;
constexpr uint8_t FEATURE_GYRO_MEAN = 3;
constexpr uint8_t FEATURE_GYRO_PEAK = 4;
constexpr uint8_t FEATURE_ORIENTATION_DELTA = 5;
constexpr uint8_t FEATURE_INACTIVITY_RATIO = 6;
constexpr uint8_t FEATURE_JERK_PEAK = 7;

MotionClass classFromProbabilities(float normal, float impact, float crash) {
  if (crash >= normal && crash >= impact) {
    return MotionClass::Crash;
  }
  if (impact >= normal && impact >= crash) {
    return MotionClass::Impact;
  }
  return MotionClass::Normal;
}

void softmax3(float& normal, float& impact, float& crash) {
  const float maxValue = max(normal, max(impact, crash));
  normal = expf(normal - maxValue);
  impact = expf(impact - maxValue);
  crash = expf(crash - maxValue);
  const float sum = normal + impact + crash;
  if (sum <= 0.0f) {
    normal = 1.0f;
    impact = 0.0f;
    crash = 0.0f;
    return;
  }
  normal /= sum;
  impact /= sum;
  crash /= sum;
}
}

bool AiModel::begin() {
  writeIndex_ = 0;
  count_ = 0;
  samplesSinceInference_ = 0;
  return true;
}

void AiModel::addSample(const MotionSample& sample) {
  window_[writeIndex_] = sample;
  writeIndex_ = (writeIndex_ + 1) % Config::AI_WINDOW_SIZE;
  if (count_ < Config::AI_WINDOW_SIZE) {
    ++count_;
  }
  ++samplesSinceInference_;
}

bool AiModel::windowReady() const {
  return count_ == Config::AI_WINDOW_SIZE && samplesSinceInference_ >= Config::AI_WINDOW_STRIDE;
}

AiPrediction AiModel::runInference() {
  const uint32_t startUs = micros();
  samplesSinceInference_ = 0;
  lastFeatures_ = extractFeatures();

#if AI_MODEL_HAS_TRAINED_WEIGHTS
  AiPrediction prediction = inferWithGeneratedWeights(lastFeatures_);
#else
  AiPrediction prediction = inferWithBaselineModel(lastFeatures_);
#endif

  prediction.inferenceTimeUs = micros() - startUs;
  prediction.trainedModel = hasTrainedWeights();
  return prediction;
}

AiFeatures AiModel::lastFeatures() const {
  return lastFeatures_;
}

size_t AiModel::modelBytes() const {
#if AI_MODEL_HAS_TRAINED_WEIGHTS
  return sizeof(GENERATED_FEATURE_MEAN) + sizeof(GENERATED_FEATURE_SCALE) + sizeof(GENERATED_WEIGHTS) + sizeof(GENERATED_BIAS);
#else
  return 0;
#endif
}

bool AiModel::hasTrainedWeights() const {
#if AI_MODEL_HAS_TRAINED_WEIGHTS
  return true;
#else
  return false;
#endif
}

AiFeatures AiModel::extractFeatures() const {
  AiFeatures features;
  if (count_ == 0) {
    return features;
  }

  float accSum = 0.0f;
  float gyroSum = 0.0f;
  float accPeakLinear = 0.0f;
  float gyroPeak = 0.0f;
  float jerkPeak = 0.0f;
  size_t inactiveCount = 0;

  const size_t firstIndex = count_ == Config::AI_WINDOW_SIZE ? writeIndex_ : 0;
  MotionSample firstSample = window_[firstIndex];
  MotionSample prevSample = firstSample;
  MotionSample lastSample = firstSample;

  for (size_t n = 0; n < count_; ++n) {
    const size_t index = (firstIndex + n) % Config::AI_WINDOW_SIZE;
    const MotionSample& sample = window_[index];
    accSum += sample.accelerationMagnitudeG;
    gyroSum += sample.gyroMagnitudeDps;
    accPeakLinear = max(accPeakLinear, sample.linearAccelerationG);
    gyroPeak = max(gyroPeak, sample.gyroMagnitudeDps);

    if (sample.linearAccelerationG <= Config::INACTIVITY_LINEAR_G && sample.gyroMagnitudeDps <= Config::INACTIVITY_GYRO_DPS) {
      ++inactiveCount;
    }

    if (n > 0) {
      const float dt = max(1.0f, static_cast<float>(sample.timestampMs - prevSample.timestampMs));
      const float jerk = fabsf(sample.accelerationMagnitudeG - prevSample.accelerationMagnitudeG) / (dt / 1000.0f);
      jerkPeak = max(jerkPeak, jerk);
    }

    prevSample = sample;
    lastSample = sample;
  }

  const float invCount = 1.0f / static_cast<float>(count_);
  const float accMean = accSum * invCount;
  const float gyroMean = gyroSum * invCount;

  float variance = 0.0f;
  for (size_t n = 0; n < count_; ++n) {
    const size_t index = (firstIndex + n) % Config::AI_WINDOW_SIZE;
    const float delta = window_[index].accelerationMagnitudeG - accMean;
    variance += delta * delta;
  }

  features.values[FEATURE_ACC_MEAN] = accMean;
  features.values[FEATURE_ACC_STD] = sqrtf(variance * invCount);
  features.values[FEATURE_ACC_PEAK_LINEAR] = accPeakLinear;
  features.values[FEATURE_GYRO_MEAN] = gyroMean;
  features.values[FEATURE_GYRO_PEAK] = gyroPeak;
  features.values[FEATURE_ORIENTATION_DELTA] = SignalProcessing::orientationDeltaDeg(
      firstSample.pitchDeg, firstSample.rollDeg, lastSample.pitchDeg, lastSample.rollDeg);
  features.values[FEATURE_INACTIVITY_RATIO] = static_cast<float>(inactiveCount) * invCount;
  features.values[FEATURE_JERK_PEAK] = jerkPeak;

  return features;
}

AiPrediction AiModel::inferWithGeneratedWeights(const AiFeatures& features) {
  float logits[GENERATED_CLASS_COUNT] = {0.0f, 0.0f, 0.0f};

  for (int classIndex = 0; classIndex < GENERATED_CLASS_COUNT; ++classIndex) {
    logits[classIndex] = GENERATED_BIAS[classIndex];
    for (int featureIndex = 0; featureIndex < GENERATED_FEATURE_COUNT; ++featureIndex) {
      const float scale = fabsf(GENERATED_FEATURE_SCALE[featureIndex]) < 0.000001f ? 1.0f : GENERATED_FEATURE_SCALE[featureIndex];
      const float normalized = (features.values[featureIndex] - GENERATED_FEATURE_MEAN[featureIndex]) / scale;
      logits[classIndex] += GENERATED_WEIGHTS[classIndex][featureIndex] * normalized;
    }
  }

  AiPrediction prediction;
  prediction.normal = logits[0];
  prediction.impact = logits[1];
  prediction.crash = logits[2];
  softmax3(prediction.normal, prediction.impact, prediction.crash);
  prediction.predictedClass = classFromProbabilities(prediction.normal, prediction.impact, prediction.crash);
  return prediction;
}

AiPrediction AiModel::inferWithBaselineModel(const AiFeatures& features) {
  const float impactEvidence = SignalProcessing::mapToScore(features.values[FEATURE_ACC_PEAK_LINEAR], 0.6f, 3.2f);
  const float rotationEvidence = SignalProcessing::mapToScore(features.values[FEATURE_GYRO_PEAK], 180.0f, 900.0f);
  const float orientationEvidence = SignalProcessing::mapToScore(features.values[FEATURE_ORIENTATION_DELTA], 20.0f, 90.0f);
  const float inactivityEvidence = SignalProcessing::clamp01(features.values[FEATURE_INACTIVITY_RATIO]);
  const float jerkEvidence = SignalProcessing::mapToScore(features.values[FEATURE_JERK_PEAK], 15.0f, 90.0f);

  AiPrediction prediction;
  prediction.normal = 2.0f - (2.0f * impactEvidence) - (0.6f * rotationEvidence);
  prediction.impact = -0.2f + (2.0f * impactEvidence) + (0.9f * rotationEvidence) + (0.4f * jerkEvidence) - (0.7f * inactivityEvidence);
  prediction.crash = -1.2f + (1.4f * impactEvidence) + (1.0f * rotationEvidence) + (0.8f * orientationEvidence) + (0.9f * inactivityEvidence);
  softmax3(prediction.normal, prediction.impact, prediction.crash);
  prediction.predictedClass = classFromProbabilities(prediction.normal, prediction.impact, prediction.crash);
  return prediction;
}
