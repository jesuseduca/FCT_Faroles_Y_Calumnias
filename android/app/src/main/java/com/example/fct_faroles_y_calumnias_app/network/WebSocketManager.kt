package com.example.fct_faroles_y_calumnias_app.network

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.Response
import okio.ByteString

object WebSocketManager {

    private var webSocket: WebSocket? = null
    private val client = OkHttpClient()
    var listener: WebSocketListener? = null

    // Aquí se guardará la URL del servidor con ngrok
    private const val SERVER_URL = "ws://192.168.1.35:8765"

    fun conectar() {
        val request = Request.Builder()
            .url(SERVER_URL)
            .build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                listener?.onOpen(webSocket, response)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                listener?.onMessage(webSocket, text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                android.util.Log.e("WebSocket", "Error: ${t.message}")
                listener?.onFailure(webSocket, t, response)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                listener?.onClosed(webSocket, code, reason)
            }
        })
    }

    fun enviarMensaje(mensaje: String) {
        webSocket?.send(mensaje)
    }

    fun desconectar() {
        webSocket?.close(1000, "Cierre normal")
        webSocket = null
    }
}