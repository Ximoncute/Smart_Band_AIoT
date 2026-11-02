from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[1]


class FirmwareContractTest(unittest.TestCase):
    def source(self, path):
        return (ROOT / path).read_text(encoding="utf-8")

    def test_fixed_pins_and_wifi_power(self):
        config = self.source("include/config.h")
        self.assertIn("GPS_TX = 1", config)
        self.assertIn("GPS_RX = 2", config)
        self.assertNotIn('WIFI_PASSWORD =', config)
        self.assertIn('BLE_WIFI_CHAR_UUID', config)
        self.assertIn("WIFI_POWER_8_5dBm", self.source("src/weather_manager.cpp"))

    def test_freertos_tasks_exist(self):
        main = self.source("src/main.cpp")
        self.assertIn("<freertos/FreeRTOS.h>", main)
        self.assertIn("<freertos/task.h>", main)
        self.assertIn("<freertos/semphr.h>", main)
        self.assertIn("xTaskCreatePinnedToCore", main)
        self.assertIn("imuAiTask", main)
        self.assertIn("gpsTask", main)

    def test_buzzer_is_silent_before_confirmed_crash(self):
        main = self.source("src/main.cpp")
        self.assertNotIn("BuzzerMode::PossibleImpact", main)
        self.assertIn("state == SystemState::Analyzing", main)
        self.assertIn("buzzerManager.setMode(BuzzerMode::Off)", main)

    def test_oled_has_time_weather_ai_and_gps_pages(self):
        display = self.source("src/display_manager.cpp")
        self.assertIn("showTimeWeather", display)
        self.assertIn("showAiLocation", display)
        self.assertIn("showGpsStatus", display)

    def test_gps_exposes_nmea_diagnostics(self):
        header = self.source("include/gps_manager.h")
        source = self.source("src/gps_manager.cpp")
        data = self.source("include/system_data.h")
        self.assertIn("TinyGPS++.h", header)
        self.assertIn("HardwareSerial GPS_Serial(1)", source)
        self.assertIn("charsProcessed", data)
        self.assertIn("failedChecksum", data)

    def test_weather_reports_pipeline_state_and_uses_https_client(self):
        data = self.source("include/system_data.h")
        weather = self.source("src/weather_manager.cpp")
        self.assertIn("WeatherStatus", data)
        self.assertIn("WiFiClientSecure", weather)
        self.assertIn("setInsecure", weather)

    def test_serial_and_oled_intervals(self):
        config = self.source("include/config.h")
        main = self.source("src/main.cpp")
        self.assertIn("OLED_PAGE_MS = 6000", config)
        self.assertIn("SERIAL_STATUS_MS = 3000", config)
        self.assertIn("printFullStatus", main)
        self.assertIn("if (datasetMode) return;", main)

    def test_boot_stages_are_visible(self):
        main = self.source("src/main.cpp")
        for stage in ("OLED", "BMI160", "CALIBRATION", "AI", "GPS", "WIFI", "TASKS READY"):
            self.assertIn(stage, main)


if __name__ == "__main__":
    unittest.main()
