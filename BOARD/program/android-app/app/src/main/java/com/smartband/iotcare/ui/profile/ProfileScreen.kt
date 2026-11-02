package com.smartband.iotcare.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartband.iotcare.ui.theme.*
import com.smartband.iotcare.viewmodel.IoTViewModel

@Composable
fun ProfileScreen(
    viewModel: IoTViewModel,
    onLogout: () -> Unit,
    onReopenOnboarding: () -> Unit
) {
    val device by viewModel.device.collectAsState()
    val person = device.person

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate900)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Person Info Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Slate800)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(ElectricBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(text = person.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        Text(text = "${person.age} tuổi • Giới tính: ${person.gender}", color = Slate400, fontSize = 12.sp)
                        Text(text = "ID Thiết bị: ${device.deviceId}", color = Cyan400, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Details Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Slate800)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "THÔNG TIN CHI TIẾT & Y TẾ", color = Slate400, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                    ProfileDetailRow(label = "Liên hệ khẩn cấp", value = person.emergencyContact)
                    ProfileDetailRow(label = "Vùng an toàn", value = person.safeZoneName)
                    ProfileDetailRow(label = "Ghi chú y tế", value = person.medicalNotes)
                    ProfileDetailRow(label = "Tài khoản quản lý", value = person.registeredEmail)
                    ProfileDetailRow(label = "Firmware thiết bị", value = device.firmwareVersion)
                }
            }
        }

        // Action Buttons
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Reopen Onboarding Info
                OutlinedButton(
                    onClick = onReopenOnboarding,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Cyan400)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Xem lại Giới thiệu Sức khỏe & Thiết bị", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                // Logout Button
                Button(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed)
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Đăng Xuất Khỏi Thiết Bị", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun ProfileDetailRow(label: String, value: String) {
    Column {
        Text(text = label, color = Slate400, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(text = value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp))
    }
}
