package com.chanti.walktalk

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import android.os.Handler
import android.os.Looper

class SignalingClient(private val onMessage: (String) -> Unit) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private fun emit(text: String) = mainHandler.post { onMessage(text) }
    private val client = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private var socket: WebSocket? = null

    fun connect(url: String) {
        disconnect()
        val request = Request.Builder().url(url).build()
        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                emit("{\"type\":\"connected\"}")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                emit(text)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                emit("{\"type\":\"system\",\"message\":\"WebSocket closed: $reason\"}")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                emit("{\"type\":\"system\",\"message\":\"WebSocket failure: ${t.message ?: "unknown"}\"}")
            }
        })
    }

    fun send(text: String) {
        socket?.send(text) ?: emit("{\"type\":\"system\",\"message\":\"Cannot send: not connected\"}")
    }

    fun disconnect() {
        socket?.close(1000, "User disconnected")
        socket = null
    }
}
