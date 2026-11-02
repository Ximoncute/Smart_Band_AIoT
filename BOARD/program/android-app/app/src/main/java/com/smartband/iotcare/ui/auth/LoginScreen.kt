package com.smartband.iotcare.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartband.iotcare.ui.theme.*

import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.input.VisualTransformation

@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit,
    onNavigateToRegister: () -> Unit,
    onBackToOnboarding: () -> Unit,
    onNavigateToForgotPassword: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

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
                // App Logo
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(ElectricBlue, Indigo600))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Tận Tâm",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                Text(
                    text = "Gửi trọn quan tâm đến cha mẹ",
                    fontSize = 12.sp,
                    color = Slate400,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Gmail Google") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Cyan400) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = Slate700
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Mật khẩu") },
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
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = Slate700
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onNavigateToForgotPassword) {
                        Text("Quên mật khẩu?", color = Cyan400, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val finalEmail = email.trim().ifEmpty { "user@gmail.com" }
                        onLoginSuccess(finalEmail)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                ) {
                    Text("Đăng Nhập Vào Hệ Thống", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(onClick = onNavigateToRegister) {
                    Text(
                        "Chưa có tài khoản? Đăng ký ngay",
                        color = Cyan400,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = onBackToOnboarding) {
                    Text(
                        "← Xem lại thông tin sức khỏe & thiết bị",
                        color = Slate400,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
