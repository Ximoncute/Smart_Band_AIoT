package com.smartband.iotcare.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private data class Slide(val badge:String,val title:String,val desc:String,val a:String,val b:String,val icon:ImageVector)

@Composable fun OnboardingScreen(onNavigateToLogin:()->Unit) {
    val slides=listOf(
        Slide("Bảo vệ người thân yêu","Tự động báo khi cha mẹ bị ngã","An tâm hơn mỗi ngày với cảnh báo tức thì","Báo cho con cháu ngay khi cha mẹ bị ngã","Gửi chuông báo kèm vị trí chính xác",Icons.Default.NotificationsActive),
        Slide("Dễ đeo, nhẹ nhàng","Đeo êm ái, pin dùng cả tuần","Dây mềm nhẹ như không đeo, tiện lợi cho người lớn tuổi","Không cần mang theo điện thoại cồng kềnh bên mình","Rửa tay, tắm gội và sinh hoạt thoải mái",Icons.Default.Favorite),
        Slide("Bản đồ quen thuộc","Luôn biết cha mẹ đang ở đâu","Định vị an toàn, rõ ràng và dễ sử dụng","Nhắc con khi cha mẹ đi ra xa khỏi nhà","Nút bấm trợ giúp khẩn cấp một chạm",Icons.Default.LocationOn))
    val pager=rememberPagerState(pageCount={slides.size}); val scope=rememberCoroutineScope(); val s=slides[pager.currentPage]
    Column(Modifier.fillMaxSize().background(Color.White).padding(horizontal=16.dp)) {
        Row(Modifier.fillMaxWidth().padding(top=18.dp,bottom=10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
            Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(Color(0xFFE14B72),Color(0xFF4F63E8)))),contentAlignment=Alignment.Center){Icon(Icons.Default.VolunteerActivism,null,tint=Color.White)};Spacer(Modifier.width(10.dp));Column{Row(verticalAlignment=Alignment.CenterVertically){Text("Tận Tâm",color=Color(0xFF0F172A),fontSize=21.sp,fontWeight=FontWeight.Black);Spacer(Modifier.width(8.dp));Text("v1.0",color=Color(0xFFBE3857),fontWeight=FontWeight.Bold,modifier=Modifier.background(Color(0xFFFFF1F2),RoundedCornerShape(8.dp)).padding(8.dp,4.dp))};Text("Gửi trọn quan tâm đến cha mẹ",color=Color(0xFF64748B),fontSize=13.sp)}}
            TextButton(onClick=onNavigateToLogin){Text("Bỏ qua →",color=Color(0xFF94A3B8),fontWeight=FontWeight.Bold)} }
        HorizontalPager(state=pager,modifier=Modifier.weight(1f).fillMaxWidth()){page->val x=slides[page];Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.SpaceBetween){
            Box(Modifier.fillMaxWidth().height(390.dp).clip(RoundedCornerShape(30.dp)).background(Brush.verticalGradient(listOf(Color(0xFF9CA3AF),Color(0xFF1E293B)))),contentAlignment=Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally){Icon(x.icon,null,tint=Color.White,modifier=Modifier.size(100.dp));Text("SMARTBAND",color=Color.White,fontSize=22.sp,fontWeight=FontWeight.Black);Text(x.badge,color=Color(0xFF172033),fontWeight=FontWeight.Bold,modifier=Modifier.background(Color.White,RoundedCornerShape(30.dp)).padding(18.dp,10.dp))};Column(Modifier.align(Alignment.BottomCenter).padding(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Pill(x.a);Pill(x.b)}}
            Column(Modifier.padding(top=24.dp)){Text(x.title,color=Color(0xFF0F172A),fontSize=25.sp,fontWeight=FontWeight.Black);Text(x.desc,color=Color(0xFF64748B),fontSize=16.sp,modifier=Modifier.padding(top=4.dp));Row(Modifier.fillMaxWidth().padding(top=14.dp).background(Color(0xFFF8FAFC),RoundedCornerShape(16.dp)).padding(14.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Favorite,null,tint=Color(0xFFE14B72));Spacer(Modifier.width(12.dp));Text("Chăm sóc nhẹ nhàng, an tâm mỗi ngày",color=Color(0xFF334155),fontSize=14.sp)}}}}
        Row(Modifier.fillMaxWidth().padding(vertical=18.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){repeat(slides.size){Box(Modifier.height(9.dp).width(if(it==pager.currentPage)28.dp else 9.dp).clip(CircleShape).background(if(it==pager.currentPage)Color(0xFF4F63E8) else Color(0xFFCBD5E1)))}};Button(onClick={if(pager.currentPage==slides.lastIndex)onNavigateToLogin()else scope.launch{pager.animateScrollToPage(pager.currentPage+1)}},shape=RoundedCornerShape(30.dp),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF4F63E8)),contentPadding=PaddingValues(horizontal=28.dp,vertical=14.dp)){Text(if(pager.currentPage==slides.lastIndex)"Đăng nhập →" else "Tiếp tục →",fontSize=16.sp,fontWeight=FontWeight.Black)}}
    }
}
@Composable private fun Pill(text:String){Row(Modifier.fillMaxWidth().background(Color(0xF5FFFFFF),RoundedCornerShape(22.dp)).padding(14.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(28.dp).clip(CircleShape).background(Color(0xFFE0F2FE)));Spacer(Modifier.width(12.dp));Text(text,color=Color(0xFF172033),fontSize=14.sp,fontWeight=FontWeight.Bold)}}
