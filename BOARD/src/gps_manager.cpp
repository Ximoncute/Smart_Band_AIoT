#include "gps_manager.h"

#include "config.h"

HardwareSerial GPS_Serial(1);

void GpsManager::begin() {
  GPS_Serial.begin(Config::GPS_BAUD, SERIAL_8N1, Config::GPS_RX, Config::GPS_TX);
}

void GpsManager::update() {
  while (GPS_Serial.available()) {
    const char c = GPS_Serial.read();
    gps_.encode(c);
  }
  data_.locationValid = gps_.location.isValid() && gps_.location.age() < 5000;
  if (gps_.location.isUpdated()) {
    data_.latitude = gps_.location.lat();
    data_.longitude = gps_.location.lng();
  }
  data_.ageMs = gps_.location.isValid() ? gps_.location.age() : UINT32_MAX;
  if (gps_.speed.isValid()) data_.speedKmh = gps_.speed.kmph();
  if (gps_.altitude.isValid()) data_.altitudeM = gps_.altitude.meters();
  if (gps_.satellites.isValid()) data_.satellites = gps_.satellites.value();
  if (gps_.hdop.isValid()) data_.hdop = gps_.hdop.hdop();
  data_.charsProcessed = gps_.charsProcessed();
  data_.sentencesWithFix = gps_.sentencesWithFix();
  data_.failedChecksum = gps_.failedChecksum();
}

GpsData GpsManager::data() const {
  return data_;
}
