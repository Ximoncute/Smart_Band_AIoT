package com.smartband.iotcare.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartband.iotcare.ui.theme.*
import com.smartband.iotcare.viewmodel.IoTViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoControllerSheet(
    viewModel: IoTViewModel,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Slate800,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "⚡ BẢNG ĐIỀU KHIỂN GIẢ LẬP SỰ KIỆN (DEMO)",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp
            )
            Text(
                text = "Kích hoạt các sự kiện phần cứng để thuyết trình và chạy thử",
                color = Slate400,
                fontSize = 11.sp
            )

            // Row 1: Fall & SOS
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        viewModel.simulateFall()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed)
                ) {
                    Text("🚨 Giả Lập Té Ngã", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        viewModel.simulateSOS()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkRed)
                ) {
                    Text("🆘 Giả Lập Bấm SOS", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            // Row 2: GPS & Toggle Online
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        viewModel.simulateGPSMove()
                    },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                ) {
                    Text("📍 Bước Di Chuyển GPS", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        viewModel.toggleDeviceStatus()
                    },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Slate700)
                ) {
                    Text("📶 Bật/Tắt 4G", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            // Row 3: Activity Switching
            Text(text = "Đổi hoạt động nhận diện AI:", color = Slate400, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    "walking" to "Đi bộ",
                    "standing" to "Đứng",
                    "sitting" to "Ngồi",
                    "running" to "Chạy"
                ).forEach { (type, label) ->
                    OutlinedButton(
                        onClick = { viewModel.simulateBehavior(type) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
