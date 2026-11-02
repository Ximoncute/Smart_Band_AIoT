package com.smartband.iotcare.viewmodel

import androidx.lifecycle.ViewModel
import com.smartband.iotcare.BuildConfig
import androidx.lifecycle.viewModelScope
import com.smartband.iotcare.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: String, // "user" or "ai"
    val text: String,
    val isLoading: Boolean = false
)

class IoTViewModel : ViewModel() {

    // Onboarding state: defaults to false so 3-step intro shows first
    private val _hasCompletedOnboarding = MutableStateFlow(false)
    val hasCompletedOnboarding: StateFlow<Boolean> = _hasCompletedOnboarding.asStateFlow()

    // Auth State
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _currentUserEmail = MutableStateFlow("")
    val currentUserEmail: StateFlow<String> = _currentUserEmail.asStateFlow()

    // AI Health & Assistant Chat History
    private val _chatHistory = MutableStateFlow(
        listOf(
            ChatMessage(
                sender = "ai",
                text = "Chào bạn! Tôi là Trợ Lý AI Thông Minh của hệ thống Tận Tâm. Bạn có thể hỏi tôi bất cứ điều gì từ sức khỏe, phân tích vận động của cha mẹ, đến lập trình hay kiến thức đời sống. Hôm nay bạn cần tôi hỗ trợ gì nào?"
            )
        )
    )
    val chatHistory: StateFlow<List<ChatMessage>> = _chatHistory.asStateFlow()

    // Device & Health Data State
    private val _device = MutableStateFlow(SmartbandDevice())
    val device: StateFlow<SmartbandDevice> = _device.asStateFlow()

    // Alerts History
    private val _alerts = MutableStateFlow(
        listOf(
            HealthAlert(
                id = "alt-001",
                type = "emergency",
                title = "🚨 CẢNH BÁO TÉ NGÃ KHẨN CẤP",
                message = "Phát hiện cú va chạm nhẹ • Cụ đã tự giữ thăng bằng an toàn.",
                timestamp = "14:32:00",
                coordinates = "21.043942, 105.780129",
                isRead = false,
                isAcknowledged = false
            ),
            HealthAlert(
                id = "alt-002",
                type = "warning",
                title = "⚠️ Cảnh báo đi xa Vùng An Toàn",
                message = "Người đeo di chuyển cách ranh giới nhà riêng vượt quá 250m.",
                timestamp = "11:40:00",
                coordinates = "21.044120, 105.781050",
                isRead = false,
                isAcknowledged = true
            ),
            HealthAlert(
                id = "alt-003",
                type = "info",
                title = "ℹ️ Thiết bị Smartband đã kết nối",
                message = "Thiết bị kết nối mạng 4G LTE và cập nhật tọa độ GPS thành công.",
                timestamp = "10:15:00",
                coordinates = "21.043942, 105.780129",
                isRead = true,
                isAcknowledged = true
            )
        )
    )
    val alerts: StateFlow<List<HealthAlert>> = _alerts.asStateFlow()

    // Reminders List
    private val _reminders = MutableStateFlow(
        listOf(
            ReminderItem(
                id = "rem-1",
                title = "Uống thuốc huyết áp",
                time = "08:00",
                repeat = "Hàng ngày",
                priority = "Cao",
                status = "Đã gửi",
                oledText = "UONG THUOC [08:00]"
            ),
            ReminderItem(
                id = "rem-2",
                title = "Uống 1 cốc nước ấm",
                time = "14:00",
                repeat = "Hàng ngày",
                priority = "Bình thường",
                status = "Đã gửi",
                oledText = "UONG NUOC AM [14:00]"
            )
        )
    )
    val reminders: StateFlow<List<ReminderItem>> = _reminders.asStateFlow()

    // 6-Axis IMU Sensor Stream Points (BMI160 50Hz)
    private val _sensorHistory = MutableStateFlow(
        listOf(
            IMUSensorPoint("14:30:00", 0.12f, 0.98f, 9.81f, 1.2f, 0.5f, 0.1f),
            IMUSensorPoint("14:30:05", 0.45f, 1.15f, 9.85f, 4.5f, 2.1f, 1.0f),
            IMUSensorPoint("14:30:10", 0.85f, 1.40f, 9.78f, 12.3f, 5.4f, 3.2f),
            IMUSensorPoint("14:30:15", 0.32f, 1.05f, 9.82f, 2.8f, 1.2f, 0.4f),
            IMUSensorPoint("14:30:20", 0.20f, 0.99f, 9.80f, 1.5f, 0.8f, 0.2f)
        )
    )
    val sensorHistory: StateFlow<List<IMUSensorPoint>> = _sensorHistory.asStateFlow()

    // Auth & Navigation Actions
    fun completeOnboarding() {
        _hasCompletedOnboarding.value = true
    }

    fun reopenOnboarding() {
        _hasCompletedOnboarding.value = false
    }

    fun login(email: String) {
        _currentUserEmail.value = email
        _isAuthenticated.value = true
    }

    fun logout() {
        _isAuthenticated.value = false
    }

    // Safety & Fall Simulation (for live demo)
    fun simulateFall() {
        _device.update { current ->
            current.copy(
                behavior = current.behavior.copy(
                    current = "falling",
                    labelVi = "TÉ NGÃ KHẨN CẤP",
                    confidence = 97.3f,
                    fallingProb = 97.3f
                ),
                safety = current.safety.copy(
                    status = "emergency",
                    fallDetected = true,
                    fallConfidence = 97.3f,
                    lastEmergency = "Té ngã"
                ),
                lastUpdate = "Vừa xong"
            )
        }

        val newAlert = HealthAlert(
            id = "alt-${System.currentTimeMillis()}",
            type = "emergency",
            title = "🚨 BÁO ĐỘNG TÉ NGÃ KHẨN CẤP",
            message = "Phát hiện gia tốc va đập đột ngột tại ${_device.value.gps.locationName}",
            timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
            coordinates = "${_device.value.gps.latitude}, ${_device.value.gps.longitude}",
            isRead = false,
            isAcknowledged = false
        )
        _alerts.update { listOf(newAlert) + it }
    }

    fun simulateSOS() {
        _device.update { current ->
            current.copy(
                safety = current.safety.copy(
                    status = "emergency",
                    sosPressed = true,
                    lastEmergency = "Nút SOS"
                ),
                lastUpdate = "Vừa xong"
            )
        }

        val newAlert = HealthAlert(
            id = "alt-${System.currentTimeMillis()}",
            type = "emergency",
            title = "🚨 NÚT KHẨN CẤP SOS ĐÃ ĐƯỢC NHẤN",
            message = "Người đeo Smartband cần hỗ trợ tại ${_device.value.gps.locationName}",
            timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
            coordinates = "${_device.value.gps.latitude}, ${_device.value.gps.longitude}",
            isRead = false,
            isAcknowledged = false
        )
        _alerts.update { listOf(newAlert) + it }
    }

    fun resetEmergencyState() {
        _device.update { current ->
            current.copy(
                behavior = current.behavior.copy(
                    current = "walking",
                    labelVi = "Đi bộ",
                    confidence = 94.8f
                ),
                safety = current.safety.copy(
                    status = "safe",
                    fallDetected = false,
                    fallConfidence = 0f,
                    sosPressed = false,
                    lastEmergency = null
                )
            )
        }
    }

    fun acknowledgeAlert(id: String) {
        _alerts.update { list ->
            list.map { if (it.id == id) it.copy(isAcknowledged = true) else it }
        }
    }

    fun simulateBehavior(type: String) {
        val (label, conf) = when (type) {
            "walking" -> "Đi bộ" to 94.8f
            "standing" -> "Đứng yên" to 91.2f
            "sitting" -> "Đang ngồi" to 96.4f
            "running" -> "Chạy nhanh" to 89.7f
            else -> "Nghỉ ngơi" to 92.0f
        }
        _device.update { current ->
            current.copy(
                behavior = current.behavior.copy(
                    current = type,
                    labelVi = label,
                    confidence = conf
                )
            )
        }
    }

    fun simulateGPSMove() {
        _device.update { current ->
            val deltaLat = (Math.random() - 0.5) * 0.0006
            val deltaLng = (Math.random() - 0.5) * 0.0006
            current.copy(
                gps = current.gps.copy(
                    latitude = current.gps.latitude + deltaLat,
                    longitude = current.gps.longitude + deltaLng,
                    updatedAt = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                ),
                lastUpdate = "Vừa xong"
            )
        }
    }

    fun toggleDeviceStatus() {
        _device.update { current ->
            val nextStatus = if (current.status == "online") "offline" else "online"
            current.copy(status = nextStatus)
        }
    }

    // Convert Vietnamese Diacritics to Plain ASCII for ESP32 OLED 0.96 inch
    private fun removeVietnameseAccent(str: String): String {
        val nfd = Normalizer.normalize(str, Normalizer.Form.NFD)
        val pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
        return pattern.matcher(nfd).replaceAll("")
            .replace('đ', 'd')
            .replace('Đ', 'D')
            .uppercase()
    }

    fun addReminder(title: String, time: String, priority: String) {
        val oled = "${removeVietnameseAccent(title)} [$time]"
        val newItem = ReminderItem(
            id = "rem-${System.currentTimeMillis()}",
            title = title,
            time = time,
            priority = priority,
            status = "Đã gửi",
            oledText = oled
        )
        _reminders.update { listOf(newItem) + it }
    }

    fun updateProfile(name: String, age: Int, emergencyPhone: String, notes: String) {
        _device.update { current ->
            current.copy(
                person = current.person.copy(
                    name = name,
                    age = age,
                    emergencyPhone = emergencyPhone,
                    medicalNotes = notes
                )
            )
        }
    }

    fun sendChatMessage(prompt: String) {
        val clean = prompt.trim()
        if (clean.isBlank()) return
        val userMsg = ChatMessage(sender = "user", text = clean)
        val loadingMsg = ChatMessage(sender = "ai", text = "", isLoading = true)
        _chatHistory.update { it + userMsg + loadingMsg }

        viewModelScope.launch {
            val reply = callDeepSeekApi(clean)
            _chatHistory.update { current ->
                if (current.isNotEmpty() && current.last().isLoading) {
                    current.dropLast(1) + ChatMessage(sender = "ai", text = reply)
                } else {
                    current + ChatMessage(sender = "ai", text = reply)
                }
            }
        }
    }

    private suspend fun callDeepSeekApi(prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.AI_API_KEY
        if (apiKey.isBlank()) return@withContext "Chưa cấu hình dịch vụ AI."
        try {
            val url = URL("https://platform.beeknoee.com/api/v1/chat/completions")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.doOutput = true
            conn.connectTimeout = 15000
            conn.readTimeout = 20000

            val currentHistory = _chatHistory.value
            val p = _device.value.person
            val sysPrompt = "Bạn là Trợ Lý AI Thông Minh & Bác Sĩ Lão Khoa Tận Tâm trên ứng dụng theo dõi sức khỏe Tận Tâm. Trả lời tự nhiên, thân thiện, chính xác mọi câu hỏi của người dùng (từ lập trình, công nghệ, giải đáp thắc mắc, trò chuyện thường nhật, cho đến y tế). Khi người dùng yêu cầu code, hãy viết mã chuẩn trong markdown. Thông tin người đeo: ${p.name}, ${p.age} tuổi."

            val jsonBody = JSONObject().apply {
                put("model", "deepseek/deepseek-v4-flash")
                val msgs = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", sysPrompt)
                    })
                    val recent = currentHistory.filter { it.text.isNotBlank() && !it.isLoading }.takeLast(6)
                    for (msg in recent) {
                        put(JSONObject().apply {
                            put("role", if (msg.sender == "user") "user" else "assistant")
                            put("content", msg.text)
                        })
                    }
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                }
                put("messages", msgs)
                put("max_tokens", 1000)
                put("temperature", 0.6)
            }

            conn.outputStream.use { os ->
                os.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
            }

            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val jsonObj = JSONObject(responseText)
                val choices = jsonObj.getJSONArray("choices")
                if (choices.length() > 0) {
                    return@withContext choices.getJSONObject(0).getJSONObject("message").getString("content").trim()
                }
            }
            "Dạ kết nối máy chủ AI đang bận, bạn vui lòng thử lại sau giây lát nhé!"
        } catch (e: Exception) {
            "Dạ hiện tại kết nối mạng máy chủ AI đang chập chờn, bạn kiểm tra kết nối internet và thử lại nhé!"
        }
    }
}
