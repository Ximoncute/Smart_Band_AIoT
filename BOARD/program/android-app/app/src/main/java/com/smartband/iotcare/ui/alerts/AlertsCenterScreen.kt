package com.smartband.iotcare.ui.alerts

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartband.iotcare.ui.theme.*
import com.smartband.iotcare.viewmodel.IoTViewModel

@Composable
fun AlertsCenterScreen(
    viewModel: IoTViewModel
) {
    val device by viewModel.device.collectAsState()
    val alerts by viewModel.alerts.collectAsState()
    val context = LocalContext.current
    val isEmergency = device.safety.status == "emergency"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate900)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Red Emergency Banner if emergency is active
        if (isEmergency) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CrimsonRed)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "BÁO ĐỘNG KHẨN CẤP ĐANG BẬT!",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Sự cố: ${device.safety.lastEmergency ?: "Té ngã"} tại ${device.gps.locationName}",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Call Emergency Phone Button
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:${device.person.emergencyPhone}")
                                    }
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = CrimsonRed, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Gọi cứu hộ", color = CrimsonRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            // Confirm Safe Button
                            Button(
                                onClick = { viewModel.resetEmergencyState() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D))
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Đã an toàn", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "NHẬT KÝ CẢNH BÁO TỔNG HỢP (${alerts.size})",
                color = Slate400,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        items(alerts.size) { index ->
            val alert = alerts[index]
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Slate800)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = alert.title,
                            color = if (alert.type == "emergency") CrimsonRed else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        Text(
                            text = alert.timestamp,
                            color = Slate400,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = alert.message,
                        color = Slate400,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Tọa độ: ${alert.coordinates}",
                        color = Cyan400,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
