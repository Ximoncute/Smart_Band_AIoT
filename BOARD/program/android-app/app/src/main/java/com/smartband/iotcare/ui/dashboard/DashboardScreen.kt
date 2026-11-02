package com.smartband.iotcare.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartband.iotcare.ui.theme.*
import com.smartband.iotcare.viewmodel.IoTViewModel

@Composable
fun DashboardScreen(
    viewModel: IoTViewModel,
    onNavigateToTracking: () -> Unit
) {
    val device by viewModel.device.collectAsState()
    val alerts by viewModel.alerts.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate900)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        // Top 4 Metric Cards Grid (2x2)
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "HÀNH VI AI",
                    value = device.behavior.labelVi,
                    subtext = "Độ tin cậy: ${device.behavior.confidence}%",
                    icon = Icons.Default.DirectionsWalk,
                    accentColor = ElectricBlue
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "VỊ TRÍ GPS",
                    value = "Cầu Giấy",
                    subtext = "±${device.gps.accuracy}m (${device.gps.satellites} Sat)",
                    icon = Icons.Default.LocationOn,
                    accentColor = Cyan400
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "TRẠNG THÁI AN TOÀN",
                    value = if (device.safety.status == "emergency") "NGUY CẤP!" else "An toàn",
                    subtext = if (device.safety.status == "emergency") "Cần cứu trợ lập tức" else "Trong vùng an toàn",
                    icon = if (device.safety.status == "emergency") Icons.Default.Warning else Icons.Default.Shield,
                    accentColor = if (device.safety.status == "emergency") CrimsonRed else Emerald500
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "THIẾT BỊ SMARTBAND",
                    value = if (device.status == "online") "Trực tuyến" else "Ngoại tuyến",
                    subtext = "Pin: ${device.batteryPercent}% • Trực Tuyến",
                    icon = Icons.Default.Sensors,
                    accentColor = if (device.status == "online") Emerald500 else Slate400
                )
            }
        }

        // Live Map Preview Banner Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Slate800),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate700))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Emerald500)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "BẢN ĐỒ VỊ TRÍ THỜI GIAN THỰC",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        TextButton(onClick = onNavigateToTracking) {
                            Text("Xem toàn màn hình →", color = Cyan400, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Simplified Map Preview Container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF0F2027))
                            .border(1.dp, Slate700, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.LocationSearching,
                                contentDescription = null,
                                tint = ElectricBlue,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = device.gps.locationName,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tọa độ: ${device.gps.latitude}, ${device.gps.longitude}",
                                color = Slate400,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        // Recent Alerts List Section
        item {
            Text(
                text = "CẢNH BÁO MỚI NHẤT (${alerts.size})",
                color = Slate400,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, top = 6.dp)
            )
        }

        items(alerts.size) { index ->
            val alert = alerts[index]
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = alert.title,
                            color = if (alert.type == "emergency") CrimsonRed else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = alert.message,
                            color = Slate400,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Lúc: ${alert.timestamp}",
                            color = Slate400,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    if (!alert.isAcknowledged) {
                        Button(
                            onClick = { viewModel.acknowledgeAlert(alert.id) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Đã xử lý", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtext: String,
    icon: ImageVector,
    accentColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Slate800),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate700))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Slate400,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp
            )
            Text(
                text = subtext,
                color = Slate400,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
