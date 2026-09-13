#pragma once

#include <Arduino.h>

namespace Config {

constexpr uint8_t BMI160_SDA = 6;
constexpr uint8_t BMI160_SCL = 7;

constexpr uint8_t OLED_SDA = 9;
constexpr uint8_t OLED_SCL = 8;

constexpr uint8_t BUZZER_PIN = 5;
constexpr uint8_t BUTTON_PIN = 4;
constexpr uint8_t GPS_TX = 1;
constexpr uint8_t GPS_RX = 2;
constexpr uint32_t GPS_BAUD = 9600;

constexpr const char* WIFI_SSID = "HieuCute";
constexpr const char* WIFI_PASSWORD = "hieucute";

constexpr uint32_t SERIAL_BAUD = 115200;

constexpr uint8_t OLED_WIDTH = 128;
constexpr uint8_t OLED_HEIGHT = 64;
constexpr uint8_t OLED_ADDRESS = 0x3C;

constexpr uint8_t BMI160_ADDRESS_PRIMARY = 0x68;
constexpr uint8_t BMI160_ADDRESS_SECONDARY = 0x69;

constexpr uint32_t SAMPLE_INTERVAL_MS = 20;
constexpr size_t AI_WINDOW_SIZE = 100;
constexpr size_t AI_WINDOW_STRIDE = 25;

constexpr uint32_t POSSIBLE_IMPACT_SCREEN_MS = 500;
constexpr uint32_t POST_IMPACT_ANALYSIS_MS = 2500;
constexpr uint32_t RECOVERY_SCREEN_MS = 1800;
constexpr uint32_t DISPLAY_REFRESH_MS = 250;
constexpr uint32_t OLED_PAGE_MS = 3000;
constexpr uint32_t SERIAL_STATUS_MS = 2000;
constexpr uint32_t WEATHER_REFRESH_MS = 900000;
constexpr uint32_t WIFI_RETRY_MS = 30000;

constexpr uint32_t CONFIRMATION_TIMEOUT_MS = 15000;

constexpr float POSSIBLE_IMPACT_LINEAR_G = 1.20f;
constexpr float POSSIBLE_IMPACT_GYRO_DPS = 280.0f;
constexpr float IMPACT_THRESHOLD_G = 2.50f;
constexpr float GYRO_THRESHOLD_DPS = 450.0f;
constexpr float ORIENTATION_THRESHOLD_DEG = 45.0f;
constexpr float INACTIVITY_LINEAR_G = 0.18f;
constexpr float INACTIVITY_GYRO_DPS = 35.0f;
constexpr float AI_SUSPECT_PROBABILITY = 0.55f;
constexpr float AI_CRASH_PROBABILITY = 0.70f;

constexpr uint8_t CRASH_SCORE_THRESHOLD = 70;

constexpr uint8_t WEIGHT_AI = 40;
constexpr uint8_t WEIGHT_IMPACT = 25;
constexpr uint8_t WEIGHT_ROTATION = 15;
constexpr uint8_t WEIGHT_ORIENTATION = 10;
constexpr uint8_t WEIGHT_INACTIVITY = 10;

constexpr size_t CALIBRATION_SAMPLES = 200;
constexpr float CALIBRATION_MAX_LINEAR_G = 0.25f;
constexpr float CALIBRATION_MAX_GYRO_DPS = 20.0f;

constexpr size_t DATASET_LABEL_MAX_LEN = 16;

}
