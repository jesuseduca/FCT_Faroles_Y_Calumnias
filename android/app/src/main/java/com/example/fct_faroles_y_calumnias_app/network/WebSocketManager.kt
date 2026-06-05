package com.example.fct_faroles_y_calumnias_app.network

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.Response

object WebSocketManager {

    private var webSocket: WebSocket? = null
    private val client = OkHttpClient()
    private val mensajesPendientes = mutableListOf<String>()

    var listener: WebSocketListener? = null
        set(value) {
            field = value
            if (value != null) {
                android.util.Log.d("WebSocket", "Listener registrado, pendientes: ${mensajesPendientes.size}")
                for (mensaje in mensajesPendientes) {
                    android.util.Log.d("WebSocket", "Entregando pendiente: $mensaje")
                    value.onMessage(webSocket!!, mensaje)
                }
                mensajesPendientes.clear()
            }
        }

    private const val SERVER_URL = "ws://192.168.1.36:8765"

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