package com.smartband.iotcare.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartband.iotcare.ui.alerts.AlertsCenterScreen
import com.smartband.iotcare.ui.behavior.BehaviorAIScreen
import com.smartband.iotcare.ui.components.DemoControllerSheet
import com.smartband.iotcare.ui.dashboard.DashboardScreen
import com.smartband.iotcare.ui.navigation.BottomTab
import com.smartband.iotcare.ui.profile.ProfileScreen
import com.smartband.iotcare.ui.reminders.RemindersScreen
import com.smartband.iotcare.ui.theme.*
import com.smartband.iotcare.ui.tracking.LiveTrackingScreen
import com.smartband.iotcare.viewmodel.IoTViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: IoTViewModel,
    onLogout: () -> Unit,
    onReopenOnboarding: () -> Unit
) {
    var selectedTab by remember { mutableStateOf<BottomTab>(BottomTab.Dashboard) }
    var showDemoSheet by remember { mutableStateOf(false) }

    val device by viewModel.device.collectAsState()
    val alerts by viewModel.alerts.collectAsState()
    val unacknowledgedCount = alerts.count { it.type == "emergency" && !it.isAcknowledged }
    val isEmergency = device.safety.status == "emergency"

    val tabs = remember {
        listOf(
            BottomTab.Dashboard,
            BottomTab.Tracking,
            BottomTab.Behavior,
            BottomTab.Alerts,
            BottomTab.Profile
        )
    }

    Scaffold(
        containerColor = Slate900,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = selectedTab.title,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp
                        )
                    }
                },
                actions = {
                    // Online Badge
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (device.status == "online") Emerald500.copy(alpha = 0.2f) else Slate700)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (device.status == "online") Emerald500 else CrimsonRed)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (device.status == "online") "Online" else "Offline",
                            color = if (device.status == "online") Emerald500 else Slate400,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Demo Controller Trigger Button
                    IconButton(onClick = { showDemoSheet = true }) {
                        Icon(
                            Icons.Default.Bolt,
                            contentDescription = "Demo Controller",
                            tint = Amber500
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Slate900)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Slate800,
                contentColor = Slate400
            ) {
                tabs.forEach { tab ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (tab == BottomTab.Alerts && unacknowledgedCount > 0) {
                                        Badge(containerColor = CrimsonRed) {
                                            Text(text = "$unacknowledgedCount", color = Color.White)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = when (tab) {
                                        BottomTab.Dashboard -> Icons.Default.Home
                                        BottomTab.Tracking -> Icons.Default.LocationOn
                                        BottomTab.Behavior -> Icons.Default.DirectionsWalk
                                        BottomTab.Alerts -> Icons.Default.Warning
                                        BottomTab.Profile -> Icons.Default.Person
                                    },
                                    contentDescription = tab.title,
                                    tint = if (isSelected) ElectricBlue else Slate400
                                )
                            }
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Slate400
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = ElectricBlue.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Sticky Emergency Alarm Banner
            if (isEmergency && selectedTab != BottomTab.Alerts) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CrimsonRed)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "🚨 BÁO ĐỘNG KHẨN CẤP: ${device.safety.lastEmergency ?: "Té ngã"}",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }

                    Button(
                        onClick = { selectedTab = BottomTab.Alerts },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Xử lý", color = CrimsonRed, fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
                }
            }

            // Current Tab View Container
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    BottomTab.Dashboard -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToTracking = { selectedTab = BottomTab.Tracking }
                    )
                    BottomTab.Tracking -> LiveTrackingScreen(viewModel = viewModel)
                    BottomTab.Behavior -> BehaviorAIScreen(viewModel = viewModel)
                    BottomTab.Alerts -> AlertsCenterScreen(viewModel = viewModel)
                    BottomTab.Profile -> ProfileScreen(
                        viewModel = viewModel,
                        onLogout = onLogout,
                        onReopenOnboarding = onReopenOnboarding
                    )
                }
            }
        }
    }

    if (showDemoSheet) {
        DemoControllerSheet(
            viewModel = viewModel,
            onDismiss = { showDemoSheet = false }
        )
    }
}
