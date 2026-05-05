package com.chanti.walktalk

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WalkTalkApp(
    store: WalkTalkStore,
    onRequestMic: () -> Unit,
    onSendTestNotification: () -> Unit,
    onSignalEvent: (String) -> Unit
) {
    var selectedTab by remember { mutableStateOf("Talk") }
    val bg = Brush.verticalGradient(listOf(Color(0xFF07100D), Color(0xFF10251E), Color(0xFF06110C)))

    MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF7CFFB2), secondary = Color(0xFFB5FFD0))) {
        Column(Modifier.fillMaxSize().background(bg).padding(16.dp)) {
            Header(store.statusText.value)
            Spacer(Modifier.height(12.dp))
            TabRow(selectedTab, onSelect = { selectedTab = it })
            Spacer(Modifier.height(12.dp))
            when (selectedTab) {
                "Talk" -> TalkScreen(store, onRequestMic, onSendTestNotification)
                "Signal" -> SignalScreen(store)
                "Notify" -> NotificationCenterScreen(store.notifications)
                "Settings" -> SettingsScreen(store)
            }
        }
    }
}

@Composable
private fun Header(status: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text("WalkTalk", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE9FFF2))
            Text("Native Android · Build 1 + Build 2 Starter", color = Color(0xFFA8DABD), fontSize = 13.sp)
        }
        Surface(color = Color(0x2222FF88), shape = RoundedCornerShape(50)) {
            Text(status, color = Color(0xFFB5FFD0), modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 12.sp)
        }
    }
}

@Composable
private fun TabRow(selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Talk", "Signal", "Notify", "Settings").forEach { tab ->
            Button(
                onClick = { onSelect(tab) },
                colors = ButtonDefaults.buttonColors(containerColor = if (selected == tab) Color(0xFF7CFFB2) else Color(0x2233FF99), contentColor = if (selected == tab) Color(0xFF07100D) else Color(0xFFE9FFF2)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            ) { Text(tab, fontSize = 12.sp) }
        }
    }
}

@Composable
private fun TalkScreen(store: WalkTalkStore, onRequestMic: () -> Unit, onSendTestNotification: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StatusCard("Room", store.room.value, "Client: ${store.clientId}")
        Button(onClick = onRequestMic, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) { Text("Request Microphone Permission") }
        Button(onClick = { store.sendPing() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) { Text("Send Ping") }
        Button(onClick = { store.sendAlert() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5E5E)), shape = RoundedCornerShape(18.dp)) { Text("Send Emergency Alert") }
        Button(onClick = onSendTestNotification, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) { Text("Show Local Android Notification") }
        Surface(color = Color(0x1711FF88), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth().height(170.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text("PTT audio comes in Build 3\nThis build proves Android shell + notifications + signaling", color = Color(0xFFB5FFD0), fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun SignalScreen(store: WalkTalkStore) {
    var chatText by remember { mutableStateOf("Hello from Android") }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(value = store.serverUrl.value, onValueChange = { store.serverUrl.value = it }, label = { Text("WebSocket server URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = store.room.value, onValueChange = { store.room.value = it }, label = { Text("Room / channel") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { store.connect() }, modifier = Modifier.weight(1f)) { Text("Connect") }
            Button(onClick = { store.joinRoom() }, modifier = Modifier.weight(1f)) { Text("Join") }
            Button(onClick = { store.disconnect() }, modifier = Modifier.weight(1f)) { Text("Stop") }
        }
        OutlinedTextField(value = chatText, onValueChange = { chatText = it }, label = { Text("Chat test") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { store.sendChat(chatText) }, modifier = Modifier.fillMaxWidth()) { Text("Send Chat Test") }
    }
}

@Composable
private fun NotificationCenterScreen(itemsList: List<AppNotification>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Notification Center", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE9FFF2))
        Text("All chats, alerts, pings, and system events appear here.", color = Color(0xFFA8DABD))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(itemsList, key = { it.id }) { item -> NotificationCard(item) }
        }
    }
}

@Composable
private fun NotificationCard(item: AppNotification) {
    val accent = when (item.type) {
        NotificationType.CHAT -> Color(0xFF82B1FF)
        NotificationType.ALERT -> Color(0xFFFF7777)
        NotificationType.PING -> Color(0xFFFFD166)
        NotificationType.SYSTEM -> Color(0xFFB5FFD0)
    }
    Surface(color = Color(0x2211FF88), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text("${item.type} · ${time(item.timestamp)}", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(item.title, color = Color.White, fontWeight = FontWeight.Bold)
            Text(item.message, color = Color(0xFFD5FFE2))
        }
    }
}

@Composable
private fun SettingsScreen(store: WalkTalkStore) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Settings", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE9FFF2))
        StatusCard("Target", "Android 12+ / OxygenOS 12.1+", "minSdk 31, targetSdk 35")
        StatusCard("Build 2", "WebSocket signaling included", "Use the server folder to run the test signaling server")
        StatusCard("Next", "Build 3 will add WebRTC audio", "Offer/answer/ICE + microphone stream")
    }
}

@Composable
private fun StatusCard(title: String, value: String, caption: String) {
    Surface(color = Color(0x1711FF88), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = Color(0xFFA8DABD), fontSize = 12.sp)
            Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(caption, color = Color(0xFFA8DABD), fontSize = 12.sp)
        }
    }
}

private fun time(ts: Long): String = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(ts))
