package com.smartband.iotcare.ui.tracking

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Satellite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.smartband.iotcare.ui.theme.*
import com.smartband.iotcare.viewmodel.IoTViewModel

@Composable
fun LiveTrackingScreen(
    viewModel: IoTViewModel
) {
    val device by viewModel.device.collectAsState()
    val isEmergency = device.safety.status == "emergency"

    val currentPosition = remember(device.gps.latitude, device.gps.longitude) {
        LatLng(device.gps.latitude, device.gps.longitude)
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(currentPosition, 16f)
    }

    var mapType by remember { mutableStateOf(MapType.NORMAL) }

    LaunchedEffect(currentPosition) {
        cameraPositionState.position = CameraPosition.fromLatLngZoom(currentPosition, 16f)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Fullscreen Google Maps Compose
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(mapType = mapType),
            uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false)
        ) {
            // Geofence Circle (250m safe zone)
            Circle(
                center = currentPosition,
                radius = 250.0,
                strokeColor = if (isEmergency) CrimsonRed else ElectricBlue,
                strokeWidth = 3f,
                fillColor = (if (isEmergency) CrimsonRed else ElectricBlue).copy(alpha = 0.15f)
            )

            // Current Person Marker
            Marker(
                state = MarkerState(position = currentPosition),
                title = device.person.name,
                snippet = "Vị trí: ${device.gps.locationName}"
            )
        }

        // Top Telemetry & Controls Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900.copy(alpha = 0.92f)),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate700))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "VÙNG AN TOÀN (GEOFENCE)",
                            color = Slate400,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "✓ Trong vùng an toàn",
                            color = Emerald500,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    // Map Type Toggle Buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { mapType = MapType.NORMAL },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (mapType == MapType.NORMAL) ElectricBlue else Slate800)
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = { mapType = MapType.SATELLITE },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (mapType == MapType.SATELLITE) ElectricBlue else Slate800)
                        ) {
                            Icon(Icons.Default.Satellite, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // Bottom Details Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.BottomCenter),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900.copy(alpha = 0.95f)),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate700))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Vị trí người thân hiện tại",
                            color = Slate400,
                            fontSize = 11.sp
                        )
                        Text(
                            text = device.gps.locationName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Emerald500.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${device.gps.speed} km/h",
                            color = Emerald500,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Tọa độ: ${device.gps.latitude}, ${device.gps.longitude}",
                        color = Cyan400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Vệ tinh: ${device.gps.satellites} Sat",
                        color = Slate400,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
