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
import java.net.URLEncoder
import org.json.JSONObject
import kotlin.random.Random

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
        generatedOtp = code
        sendingOtp = true
        status = "Đang gửi mã OTP..."
        scope.launch {
            val sent = withContext(Dispatchers.IO) {
                runCatching {
                    // Demo only: FormSubmit may ask the recipient to activate the form.
                    // FormSubmit expects the recipient email unescaped in the URL path.
                    val endpoint = "https://formsubmit.co/ajax/${URLEncoder.encode(targetEmail, "UTF-8")}"
                    val body = JSONObject().apply {
                        put("_subject", "[Smartband Care] OTP đăng ký tài khoản")
                        put("_captcha", "false")
                        put("_template", "table")
                        put("Người nhận", targetEmail)
                        put("Mã OTP", code)
                        put("Hiệu lực", "5 phút")
                    }.toString()
                    val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        doOutput = true
                        connectTimeout = 15000
                        readTimeout = 15000
                        setRequestProperty("User-Agent", "SmartbandCare-Demo/1.0")
                        setRequestProperty("Accept", "application/json")
                        setRequestProperty("Content-Type", "application/json")
                    }
                    connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                    val responseCode = connection.responseCode
                    val responseText = runCatching {
                        (if (responseCode in 200..299) connection.inputStream else connection.errorStream)
                            ?.bufferedReader()?.use { it.readText() }.orEmpty()
                    }.getOrDefault("")
                    responseCode in 200..299 && !responseText.contains("error", ignoreCase = true)
                }.getOrDefault(false)
            }
            sendingOtp = false
            status = if (sent) "Đã gửi OTP. Kiểm tra Gmail của bạn." else "Không gửi được OTP. Kiểm tra mạng hoặc kích hoạt FormSubmit."
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
