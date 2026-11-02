package com.smartband.iotcare.ui.behavior

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartband.iotcare.ui.theme.*
import com.smartband.iotcare.viewmodel.IoTViewModel

@Composable
fun BehaviorAIScreen(
    viewModel: IoTViewModel
) {
    val device by viewModel.device.collectAsState()
    val sensorData by viewModel.sensorHistory.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate900)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // AI State Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Slate800)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "TRẠNG THÁI HOẠT ĐỘNG AI",
                                color = Slate400,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = device.behavior.labelVi,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 24.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(ElectricBlue.copy(alpha = 0.2f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Độ tin cậy: ${device.behavior.confidence}%",
                                color = Cyan400,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Probability Bars
                    BehaviorProbabilityBar(name = "Đi bộ (Walking)", percent = device.behavior.walkingProb, color = ElectricBlue)
                    Spacer(modifier = Modifier.height(6.dp))
                    BehaviorProbabilityBar(name = "Đứng yên (Standing)", percent = device.behavior.standingProb, color = Cyan400)
                    Spacer(modifier = Modifier.height(6.dp))
                    BehaviorProbabilityBar(name = "Đang ngồi (Sitting)", percent = device.behavior.sittingProb, color = Indigo600)
                    Spacer(modifier = Modifier.height(6.dp))
                    BehaviorProbabilityBar(name = "Té ngã (Falling)", percent = device.behavior.fallingProb, color = CrimsonRed)
                }
            }
        }

        // Realtime BMI160 Sensor Canvas Graph (Accelerometer 3-Axis)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Slate800)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "GIA TỐC 3 TRỤC (BMI160 - 50Hz)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Tín hiệu gia tốc trọng trường g (Ax, Ay, Az)",
                                color = Slate400,
                                fontSize = 11.sp
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LegendDot("Ax", ElectricBlue)
                            LegendDot("Ay", Emerald500)
                            LegendDot("Az", Amber500)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Compose Canvas Waveform Graph
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Slate900)
                            .border(1.dp, Slate700, RoundedCornerShape(12.dp))
                            .padding(8.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            // Draw subtle grid lines
                            drawLine(Color(0xFF334155), Offset(0f, h / 2), Offset(w, h / 2), strokeWidth = 1f)
                            drawLine(Color(0xFF334155), Offset(0f, h / 4), Offset(w, h / 4), strokeWidth = 1f)
                            drawLine(Color(0xFF334155), Offset(0f, 3 * h / 4), Offset(w, 3 * h / 4), strokeWidth = 1f)

                            if (sensorData.size >= 2) {
                                val stepX = w / (sensorData.size - 1)

                                val pathAx = Path()
                                val pathAy = Path()
                                val pathAz = Path()

                                sensorData.forEachIndexed { i, pt ->
                                    val x = i * stepX
                                    val yAx = h / 2 - (pt.ax * 30f)
                                    val yAy = h / 2 - (pt.ay * 30f)
                                    val yAz = h / 2 - ((pt.az - 9.8f) * 30f)

                                    if (i == 0) {
                                        pathAx.moveTo(x, yAx)
                                        pathAy.moveTo(x, yAy)
                                        pathAz.moveTo(x, yAz)
                                    } else {
                                        pathAx.lineTo(x, yAx)
                                        pathAy.lineTo(x, yAy)
                                        pathAz.lineTo(x, yAz)
                                    }
                                }

                                drawPath(pathAx, ElectricBlue, style = Stroke(width = 3f))
                                drawPath(pathAy, Emerald500, style = Stroke(width = 3f))
                                drawPath(pathAz, Amber500, style = Stroke(width = 3f))
                            }
                        }
                    }
                }
            }
        }

        // 3. Interactive AI Chatbot Card (Trò chuyện trực tiếp cùng Trợ Lý AI)
        item {
            val chatHistory by viewModel.chatHistory.collectAsState()
            var inputText by remember { mutableStateOf("") }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Slate800),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate700))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Chatbot Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ElectricBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Trợ Lý AI Thông Minh",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Tư vấn sức khỏe & vận động 24/7",
                                    color = Slate400,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Emerald500.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Miễn phí 100%",
                                color = Emerald500,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Chat messages list container
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 320.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Slate900)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        chatHistory.forEach { msg ->
                            if (msg.sender == "user") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp, 14.dp, 2.dp, 14.dp))
                                            .background(ElectricBlue)
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = msg.text,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp, 14.dp, 14.dp, 2.dp))
                                            .background(Slate800)
                                            .border(1.dp, Slate700, RoundedCornerShape(14.dp, 14.dp, 14.dp, 2.dp))
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        if (msg.isLoading) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(14.dp),
                                                    color = Cyan400,
                                                    strokeWidth = 2.dp
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Trợ lý AI đang suy nghĩ...",
                                                    color = Cyan400,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = msg.text,
                                                color = Color(0xFFE2E8F0),
                                                fontSize = 12.sp,
                                                lineHeight = 17.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick suggestion questions
                    val suggestions = listOf(
                        "Vận động 94.2% NORMAL có tốt không?",
                        "Đi bộ 2km có cần bù nước?",
                        "Tư vấn thực đơn phòng chống ngã"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        suggestions.forEach { prompt ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Slate700)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                TextButton(
                                    onClick = { viewModel.sendChatMessage(prompt) },
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.height(24.dp)
                                ) {
                                    Text(
                                        text = prompt,
                                        color = Cyan400,
                                        fontSize = 10.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // Input & Send button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Hỏi Trợ lý AI về sức khỏe, lập trình...", fontSize = 11.sp, color = Slate400) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = ElectricBlue,
                                unfocusedBorderColor = Slate700
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    viewModel.sendChatMessage(inputText)
                                    inputText = ""
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ElectricBlue)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Gửi",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BehaviorProbabilityBar(name: String, percent: Float, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = name, color = Slate400, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(text = "$percent%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(3.dp))
        LinearProgressIndicator(
            progress = { percent / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = Slate900
        )
    }
}

@Composable
fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, color = Slate400, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
