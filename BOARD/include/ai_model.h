#pragma once

#include "config.h"
#include "motion_types.h"

struct AiFeatures {
  float values[8] = {0.0f};
};

class AiModel {
 public:
  bool begin();
  void addSample(const MotionSample& sample);
  bool windowReady() const;
  AiPrediction runInference();
  AiFeatures lastFeatures() const;
  size_t modelBytes() const;
  bool hasTrainedWeights() const;

 private:
  AiFeatures extractFeatures() const;
  AiPrediction inferWithGeneratedWeights(const AiFeatures& features);
  AiPrediction inferWithBaselineModel(const AiFeatures& features);

  MotionSample window_[Config::AI_WINDOW_SIZE];
  size_t writeIndex_ = 0;
  size_t count_ = 0;
  size_t samplesSinceInference_ = 0;
  AiFeatures lastFeatures_;
};
