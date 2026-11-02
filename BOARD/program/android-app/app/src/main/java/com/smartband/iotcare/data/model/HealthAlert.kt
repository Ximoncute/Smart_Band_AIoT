package com.smartband.iotcare.data.model

data class HealthAlert(
    val id: String,
    val type: String, // emergency, warning, info
    val title: String,
    val message: String,
    val timestamp: String,
    val coordinates: String,
    val isRead: Boolean = false,
    val isAcknowledged: Boolean = false
)

data class ReminderItem(
    val id: String,
    val title: String,
    val time: String,
    val repeat: String = "Hàng ngày",
    val priority: String = "Cao",
    val status: String = "Đã gửi",
    val oledText: String
)

data class IMUSensorPoint(
    val time: String,
    val ax: Float,
    val ay: Float,
    val az: Float,
    val gx: Float,
    val gy: Float,
    val gz: Float
)
