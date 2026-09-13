#include "imu_manager.h"

#include "config.h"
#include "signal_processing.h"

namespace {
constexpr uint8_t BMI160_REG_CHIP_ID = 0x00;
constexpr uint8_t BMI160_REG_DATA = 0x0C;
constexpr uint8_t BMI160_REG_ACC_CONF = 0x40;
constexpr uint8_t BMI160_REG_ACC_RANGE = 0x41;
constexpr uint8_t BMI160_REG_GYR_CONF = 0x42;
constexpr uint8_t BMI160_REG_GYR_RANGE = 0x43;
constexpr uint8_t BMI160_REG_CMD = 0x7E;
constexpr uint8_t BMI160_CHIP_ID = 0xD1;

constexpr float ACC_SCALE_16G = 2048.0f;
constexpr float GYRO_SCALE_2000_DPS = 16.384f;
}

bool ImuManager::begin() {
  Wire.begin(Config::BMI160_SDA, Config::BMI160_SCL);
  Wire.setClock(400000);

  if (probeAddress(Config::BMI160_ADDRESS_PRIMARY)) {
    address_ = Config::BMI160_ADDRESS_PRIMARY;
  } else if (probeAddress(Config::BMI160_ADDRESS_SECONDARY)) {
    address_ = Config::BMI160_ADDRESS_SECONDARY;
  } else {
    lastError_ = "BMI160 not found on 0x68/0x69";
    return false;
  }

  if (!writeRegister(BMI160_REG_CMD, 0xB6)) {
    lastError_ = "BMI160 soft reset failed";
    return false;
  }
  delay(100);

  if (!writeRegister(BMI160_REG_CMD, 0x11)) {
    lastError_ = "BMI160 accel normal mode failed";
    return false;
  }
  delay(60);
  if (!writeRegister(BMI160_REG_CMD, 0x15)) {
    lastError_ = "BMI160 gyro normal mode failed";
    return false;
  }
  delay(80);

  writeRegister(BMI160_REG_ACC_CONF, 0x28);
  writeRegister(BMI160_REG_ACC_RANGE, 0x0C);
  writeRegister(BMI160_REG_GYR_CONF, 0x28);
  writeRegister(BMI160_REG_GYR_RANGE, 0x00);

  lastError_ = "ok";
  return true;
}

bool ImuManager::calibrate() {
  MotionSample sample;
  float axSum = 0.0f;
  float aySum = 0.0f;
  float azSum = 0.0f;
  float gxSum = 0.0f;
  float gySum = 0.0f;
  float gzSum = 0.0f;
  float peakLinearG = 0.0f;
  float peakGyroDps = 0.0f;

  axOffsetG_ = 0.0f;
  ayOffsetG_ = 0.0f;
  azOffsetG_ = 0.0f;
  gxOffsetDps_ = 0.0f;
  gyOffsetDps_ = 0.0f;
  gzOffsetDps_ = 0.0f;

  for (size_t i = 0; i < Config::CALIBRATION_SAMPLES; ++i) {
    if (!readSample(sample)) {
      lastError_ = "calibration read failed";
      return false;
    }
    axSum += sample.ax;
    aySum += sample.ay;
    azSum += sample.az;
    gxSum += sample.gx;
    gySum += sample.gy;
    gzSum += sample.gz;
    peakLinearG = max(peakLinearG, sample.linearAccelerationG);
    peakGyroDps = max(peakGyroDps, sample.gyroMagnitudeDps);
    delay(5);
  }

  if (peakLinearG > Config::CALIBRATION_MAX_LINEAR_G || peakGyroDps > Config::CALIBRATION_MAX_GYRO_DPS) {
    lastError_ = "device moved during calibration";
    return false;
  }

  const float invCount = 1.0f / static_cast<float>(Config::CALIBRATION_SAMPLES);
  const float avgAx = axSum * invCount;
  const float avgAy = aySum * invCount;
  const float avgAz = azSum * invCount;

  axOffsetG_ = avgAx;
  ayOffsetG_ = avgAy;
  azOffsetG_ = avgAz >= 0.0f ? (avgAz - 1.0f) : (avgAz + 1.0f);
  gxOffsetDps_ = gxSum * invCount;
  gyOffsetDps_ = gySum * invCount;
  gzOffsetDps_ = gzSum * invCount;

  lastError_ = "ok";
  return true;
}

bool ImuManager::readSample(MotionSample& sample) {
  uint8_t buffer[12] = {0};
  if (!readRegisters(BMI160_REG_DATA, buffer, sizeof(buffer))) {
    lastError_ = "BMI160 data read failed";
    return false;
  }

  const int16_t rawGx = readInt16LE(buffer, 0);
  const int16_t rawGy = readInt16LE(buffer, 2);
  const int16_t rawGz = readInt16LE(buffer, 4);
  const int16_t rawAx = readInt16LE(buffer, 6);
  const int16_t rawAy = readInt16LE(buffer, 8);
  const int16_t rawAz = readInt16LE(buffer, 10);

  sample.timestampMs = millis();
  sample.ax = (static_cast<float>(rawAx) / ACC_SCALE_16G) - axOffsetG_;
  sample.ay = (static_cast<float>(rawAy) / ACC_SCALE_16G) - ayOffsetG_;
  sample.az = (static_cast<float>(rawAz) / ACC_SCALE_16G) - azOffsetG_;
  sample.gx = (static_cast<float>(rawGx) / GYRO_SCALE_2000_DPS) - gxOffsetDps_;
  sample.gy = (static_cast<float>(rawGy) / GYRO_SCALE_2000_DPS) - gyOffsetDps_;
  sample.gz = (static_cast<float>(rawGz) / GYRO_SCALE_2000_DPS) - gzOffsetDps_;
  SignalProcessing::enrichSample(sample);
  return true;
}

const char* ImuManager::lastError() const {
  return lastError_;
}

uint8_t ImuManager::address() const {
  return address_;
}

bool ImuManager::probeAddress(uint8_t candidateAddress) {
  Wire.beginTransmission(candidateAddress);
  if (Wire.endTransmission() != 0) {
    return false;
  }

  Wire.beginTransmission(candidateAddress);
  Wire.write(BMI160_REG_CHIP_ID);
  if (Wire.endTransmission(false) != 0) {
    return false;
  }

  if (Wire.requestFrom(candidateAddress, static_cast<uint8_t>(1)) != 1) {
    return false;
  }

  return Wire.read() == BMI160_CHIP_ID;
}

bool ImuManager::writeRegister(uint8_t reg, uint8_t value) {
  Wire.beginTransmission(address_);
  Wire.write(reg);
  Wire.write(value);
  return Wire.endTransmission() == 0;
}

bool ImuManager::readRegisters(uint8_t startReg, uint8_t* buffer, size_t len) {
  Wire.beginTransmission(address_);
  Wire.write(startReg);
  if (Wire.endTransmission(false) != 0) {
    return false;
  }

  const size_t readCount = Wire.requestFrom(address_, static_cast<uint8_t>(len));
  if (readCount != len) {
    return false;
  }

  for (size_t i = 0; i < len; ++i) {
    buffer[i] = Wire.read();
  }
  return true;
}

int16_t ImuManager::readInt16LE(const uint8_t* buffer, size_t index) const {
  return static_cast<int16_t>((static_cast<uint16_t>(buffer[index + 1]) << 8) | buffer[index]);
}
