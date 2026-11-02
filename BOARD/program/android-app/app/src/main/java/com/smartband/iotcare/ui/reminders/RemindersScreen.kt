package com.smartband.iotcare.ui.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartband.iotcare.ui.theme.*
import com.smartband.iotcare.viewmodel.IoTViewModel

@Composable
fun RemindersScreen(
    viewModel: IoTViewModel
) {
    val reminders by viewModel.reminders.collectAsState()
    val device by viewModel.device.collectAsState()

    var title by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("08:00") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate900)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // OLED 0.96 inch ESP32-S3 Screen Simulation Box
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Slate800)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "MÔ PHỎNG MÀN HÌNH OLED 0.96 INCH TRỰC TIẾP",
                        color = Slate400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Giao diện hiển thị thực tế trên bo mạch đeo tay ESP32-S3",
                        color = Slate400,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // OLED Display Bezel
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black)
                            .border(3.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("OLED 128x64", color = Cyan400, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                                Text("ESP32-S3", color = Cyan400, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "[NHẮC NHỞ HIỆN TẠI]",
                                color = Cyan400,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )

                            val activeOled = reminders.firstOrNull()?.oledText ?: "UONG THUOC [08:00]"
                            Text(
                                text = activeOled,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            Text(
                                text = "TRẠNG THÁI: ${device.behavior.labelVi.uppercase()}",
                                color = Cyan400,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("GPS: OK", color = Cyan400, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                                Text("SOS: READY", color = Cyan400, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        }

        // Create New Reminder Form
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Slate800)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "GỬI NHẮC NHỞ MỚI",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Hệ thống tự động chuyển chữ sang KHÔNG DẤU để truyền xuống Smartband",
                        color = Slate400,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Tiêu đề (VD: Uống thuốc tim mạch, uống nước)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        label = { Text("Giờ hẹn (HH:mm)") },
                        leadingIcon = { Icon(Icons.Default.Alarm, contentDescription = null, tint = Cyan400) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                viewModel.addReminder(title, time, "Cao")
                                title = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Gửi Lời Nhắc Xuống Thiết Bị", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // Reminders List
        item {
            Text(
                text = "DANH SÁCH LỜI NHẮC TRÊN THIẾT BỊ (${reminders.size})",
                color = Slate400,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        items(reminders.size) { index ->
            val rem = reminders[index]
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Slate800)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = rem.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(text = "Hiển thị OLED: ${rem.oledText}", color = Cyan400, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        Text(text = "Lặp lại: ${rem.repeat}", color = Slate400, fontSize = 11.sp)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Emerald500.copy(alpha = 0.2f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(text = rem.time, color = Emerald500, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
