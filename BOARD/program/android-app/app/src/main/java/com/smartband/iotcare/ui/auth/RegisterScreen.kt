package com.smartband.iotcare.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartband.iotcare.ui.theme.*

import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.input.VisualTransformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import kotlin.random.Random

private const val EMAILJS_PUBLIC_KEY = "xm9UhmwYORUwVFhvM"
private const val EMAILJS_SERVICE_ID = "service_073uc4m"
private const val EMAILJS_TEMPLATE_ID = "template_8c6g64f"

private data class OtpSendResult(val success: Boolean, val message: String)

private fun sendOtpWithEmailJs(email: String, code: String, name: String): OtpSendResult {
    val payload = JSONObject().apply {
        put("service_id", EMAILJS_SERVICE_ID)
        put("template_id", EMAILJS_TEMPLATE_ID)
        put("user_id", EMAILJS_PUBLIC_KEY)
        put("template_params", JSONObject().apply {
            put("to_email", email)
            put("name", name.ifBlank { "Quý khách" })
            put("otp", code)
            put("purpose", "Xác nhận đăng ký tài khoản Tận Tâm")
            put("time", "5 phút")
        })
    }.toString()

    val connection = (URL("https://api.emailjs.com/api/v1.0/email/send").openConnection() as HttpURLConnection)
    return try {
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.connectTimeout = 8_000
        connection.readTimeout = 8_000
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }

        val responseCode = connection.responseCode
        val responseText = (if (responseCode in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (responseCode in 200..299) {
            OtpSendResult(true, "Đã gửi OTP. Kiểm tra Gmail của bạn.")
        } else {
            OtpSendResult(false, "EmailJS lỗi ($responseCode). ${responseText.take(160)}")
        }
    } catch (error: Exception) {
        OtpSendResult(false, when (error) {
            is java.net.SocketTimeoutException -> "EmailJS phản hồi quá lâu. Kiểm tra mạng rồi thử lại."
            else -> "Không gửi được OTP: ${error.message ?: "lỗi kết nối"}"
        })
    } finally {
        connection.disconnect()
    }
}
@Composable
fun RegisterScreen(
    onRegisterSuccess: (String) -> Unit,
    onBackToLogin: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var otp by rememberSaveable { mutableStateOf("") }
    var generatedOtp by rememberSaveable { mutableStateOf("") }
    var status by rememberSaveable { mutableStateOf("") }
    var sendingOtp by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun sendDemoOtp() {
        val targetEmail = email.trim()
        if (!targetEmail.contains("@") || !targetEmail.contains(".")) {
            status = "Vui lòng nhập Gmail hợp lệ trước."
            return
        }
        val code = Random.nextInt(100000, 1000000).toString()
        sendingOtp = true
        status = "Đang gửi mã OTP..."
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                sendOtpWithEmailJs(targetEmail, code, name.trim())
            }
            if (result.success) generatedOtp = code
            sendingOtp = false
            status = result.message
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate900)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Slate800)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Đăng Ký Tài Khoản",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Tạo tài khoản quản lý người thân",
                    color = Slate400,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Họ và Tên") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Cyan400) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = otp,
                        onValueChange = { otp = it.filter(Char::isDigit).take(6) },
                        label = { Text("Mã OTP") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = ::sendDemoOtp,
                        enabled = !sendingOtp,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (sendingOtp) "Đang gửi" else "Gửi mã")
                    }
                }

                if (status.isNotBlank()) {
                    Text(
                        text = status,
                        color = if (status.startsWith("Đã")) Color(0xFF7DFFB2) else Color(0xFFFFB4AB),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Gmail nhận mã OTP") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Cyan400) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Tạo mật khẩu") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Cyan400) },
                    trailingIcon = {
                        val icon = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        val desc = if (passwordVisible) "Ẩn mật khẩu" else "Hiện mật khẩu"
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = icon, contentDescription = desc, tint = Slate400)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val finalEmail = email.trim().ifEmpty { "user@gmail.com" }
                        if (generatedOtp.isBlank()) {
                            status = "Hãy bấm Gửi mã trước."
                        } else if (otp != generatedOtp) {
                            status = "Mã OTP chưa đúng."
                        } else {
                            onRegisterSuccess(finalEmail)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                ) {
                    Text("Đăng Ký & Tiếp Tục", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = onBackToLogin) {
                    Text(
                        "Đã có tài khoản? Đăng nhập",
                        color = Slate400,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
