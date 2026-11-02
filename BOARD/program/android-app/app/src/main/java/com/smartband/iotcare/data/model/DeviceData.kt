package com.smartband.iotcare.data.model

data class PersonProfile(
    val name: String = "Cụ Nguyễn Văn An",
    val age: Int = 68,
    val gender: String = "Nam",
    val medicalNotes: String = "Tiền sử cao huyết áp, cần đo huyết áp định kỳ",
    val emergencyContact: String = "Nguyễn Thị Mai (Con gái) - 0988.123.456",
    val emergencyPhone: String = "0988123456",
    val registeredEmail: String = "",
    val safeZoneName: String = "Nhà riêng & Công viên Cầu Giấy (Hà Nội)"
)

data class GPSLocation(
    val latitude: Double = 21.043942,
    val longitude: Double = 105.780129,
    val altitude: Double = 25.2,
    val accuracy: Double = 4.2,
    val satellites: Int = 7,
    val speed: Double = 3.6,
    val locationName: String = "Đường Hoàng Quốc Việt, Cầu Giấy, Hà Nội",
    val updatedAt: String = "14:32:18"
)

data class AIBehavior(
    val current: String = "walking", // walking, standing, sitting, running, falling
    val labelVi: String = "Đi bộ",
    val confidence: Float = 94.8f,
    val detectedBy: String = "Cảm biến Smartband BMI160",
    val walkingProb: Float = 94.8f,
    val standingProb: Float = 2.1f,
    val sittingProb: Float = 0.8f,
    val runningProb: Float = 1.4f,
    val fallingProb: Float = 0.2f
)

data class SafetyState(
    val status: String = "safe", // safe, warning, emergency
    val fallDetected: Boolean = false,
    val fallConfidence: Float = 0.0f,
    val sosPressed: Boolean = false,
    val lastEmergency: String? = null
)

data class SmartbandDevice(
    val deviceId: String = "SMARTBAND-001",
    val firmwareVersion: String = "v2.0.0",
    val status: String = "online", // online, offline
    val batteryPercent: Int = 86,
    val networkType: String = "Mạng Di Động 4G / LTE",
    val signalDbm: Int = -62,
    val lastUpdate: String = "Vừa xong",
    val person: PersonProfile = PersonProfile(),
    val gps: GPSLocation = GPSLocation(),
    val behavior: AIBehavior = AIBehavior(),
    val safety: SafetyState = SafetyState()
)
