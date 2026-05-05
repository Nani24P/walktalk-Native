package com.chanti.walktalk

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateListOf
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.json.JSONObject
import java.util.UUID

class MainActivity : ComponentActivity() {
    private lateinit var notifier: AndroidNotifier

    private val requestMicPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        WalkTalkStore.addSystem(if (granted) "Microphone permission granted" else "Microphone permission denied")
    }

    private val requestNotificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        WalkTalkStore.addSystem(if (granted) "Notification permission granted" else "Notification permission denied")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notifier = AndroidNotifier(this)
        notifier.createChannels()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            WalkTalkApp(
                store = WalkTalkStore,
                onRequestMic = { requestMicPermission.launch(Manifest.permission.RECORD_AUDIO) },
                onSendTestNotification = {
                    WalkTalkStore.addAlert("Local test alert created from Android app")
                    notifier.showAlert("WalkTalk Alert", "Local test alert from Build 1")
                },
                onSignalEvent = { raw ->
                    val title = "Signaling event"
                    WalkTalkStore.addSystem(raw.take(140))
                    notifier.showSystem(title, raw.take(180))
                }
            )
        }
    }
}

object WalkTalkStore {
    val notifications = mutableStateListOf<AppNotification>()
    val signaling = SignalingClient { raw -> handleSignal(raw) }

    var statusText = androidx.compose.runtime.mutableStateOf("Not connected")
    var serverUrl = androidx.compose.runtime.mutableStateOf("ws://YOUR-SERVER-HERE:8787")
    var room = androidx.compose.runtime.mutableStateOf("walktalk-channel-1")
    val clientId: String = "android-" + UUID.randomUUID().toString().take(8)

    fun addChat(message: String, from: String = "Device") = add(NotificationType.CHAT, "Chat", "$from: $message", NotificationPriority.NORMAL)
    fun addAlert(message: String) = add(NotificationType.ALERT, "Alert", message, NotificationPriority.EMERGENCY)
    fun addPing(message: String) = add(NotificationType.PING, "Ping", message, NotificationPriority.NORMAL)
    fun addSystem(message: String) = add(NotificationType.SYSTEM, "System", message, NotificationPriority.LOW)

    private fun add(type: NotificationType, title: String, message: String, priority: NotificationPriority) {
        notifications.add(0, AppNotification(UUID.randomUUID().toString(), type, title, message, System.currentTimeMillis(), false, priority))
    }

    fun connect() {
        statusText.value = "Connecting..."
        signaling.connect(serverUrl.value)
    }

    fun disconnect() {
        signaling.disconnect()
        statusText.value = "Disconnected"
        addSystem("Disconnected from signaling server")
    }

    fun joinRoom() {
        val payload = JSONObject()
            .put("type", "join")
            .put("room", room.value)
            .put("clientId", clientId)
            .put("clientType", "android")
        signaling.send(payload.toString())
        addSystem("Join sent for ${room.value}")
    }

    fun sendChat(text: String) {
        val payload = JSONObject()
            .put("type", "chat")
            .put("room", room.value)
            .put("from", clientId)
            .put("message", text)
        signaling.send(payload.toString())
        addChat(text, "Me")
    }

    fun sendPing() {
        val payload = JSONObject()
            .put("type", "ping")
            .put("room", room.value)
            .put("from", clientId)
        signaling.send(payload.toString())
        addPing("Ping sent to ${room.value}")
    }

    fun sendAlert() {
        val payload = JSONObject()
            .put("type", "alert")
            .put("room", room.value)
            .put("from", clientId)
            .put("level", "emergency")
            .put("message", "Emergency alert from Android")
        signaling.send(payload.toString())
        addAlert("Emergency alert sent")
    }

    private fun handleSignal(raw: String) {
        try {
            val json = JSONObject(raw)
            when (json.optString("type")) {
                "connected" -> {
                    statusText.value = "Connected"
                    addSystem("Connected to signaling server")
                }
                "peer-joined" -> addSystem("Peer joined: ${json.optString("clientId")}")
                "peer-left" -> addSystem("Peer left: ${json.optString("clientId")}")
                "chat" -> addChat(json.optString("message"), json.optString("from", "Peer"))
                "ping" -> addPing("Ping received from ${json.optString("from", "Peer")}")
                "alert" -> addAlert(json.optString("message", "Emergency alert received"))
                else -> addSystem("Signal: ${raw.take(160)}")
            }
        } catch (e: Exception) {
            addSystem("Raw signal: ${raw.take(160)}")
        }
    }
}

data class AppNotification(
    val id: String,
    val type: NotificationType,
    val title: String,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean,
    val priority: NotificationPriority
)

enum class NotificationType { CHAT, ALERT, PING, SYSTEM }
enum class NotificationPriority { LOW, NORMAL, HIGH, EMERGENCY }

class AndroidNotifier(private val context: Context) {
    companion object {
        const val CHATS = "walktalk_chats"
        const val ALERTS = "walktalk_alerts"
        const val PINGS = "walktalk_pings"
        const val SYSTEM = "walktalk_system"
    }

    fun createChannels() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channels = listOf(
            NotificationChannel(CHATS, "WalkTalk Chats", NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel(ALERTS, "WalkTalk Alerts", NotificationManager.IMPORTANCE_HIGH),
            NotificationChannel(PINGS, "WalkTalk Pings", NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel(SYSTEM, "WalkTalk System", NotificationManager.IMPORTANCE_LOW)
        )
        manager.createNotificationChannels(channels)
    }

    fun showAlert(title: String, message: String) = show(ALERTS, title, message, NotificationCompat.PRIORITY_HIGH)
    fun showSystem(title: String, message: String) = show(SYSTEM, title, message, NotificationCompat.PRIORITY_LOW)

    private fun show(channelId: String, title: String, message: String, priority: Int) {
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(priority)
            .setAutoCancel(true)
            .build()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify((System.currentTimeMillis() % 100000).toInt(), notification)
        }
    }
}
