#pragma once

#include "motion_types.h"

namespace SignalProcessing {

float magnitude3(float x, float y, float z);
float clamp01(float value);
float mapToScore(float value, float low, float high);
void enrichSample(MotionSample& sample);
float orientationDeltaDeg(float pitchA, float rollA, float pitchB, float rollB);

}
