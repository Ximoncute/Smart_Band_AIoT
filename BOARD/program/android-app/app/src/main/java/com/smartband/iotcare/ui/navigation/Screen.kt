package com.smartband.iotcare.ui.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object Register : Screen("register")
    object Main : Screen("main")
}

sealed class BottomTab(val route: String, val title: String, val iconId: String) {
    object Dashboard : BottomTab("tab_dashboard", "Trang chủ", "home")
    object Tracking : BottomTab("tab_tracking", "Định vị", "location")
    object Behavior : BottomTab("tab_behavior", "Hành vi AI", "activity")
    object Alerts : BottomTab("tab_alerts", "Cảnh báo", "warning")
    object Profile : BottomTab("tab_profile", "Hồ sơ", "person")
}
