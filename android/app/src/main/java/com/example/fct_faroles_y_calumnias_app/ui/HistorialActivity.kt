package com.example.fct_faroles_y_calumnias_app.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.example.fct_faroles_y_calumnias_app.R
import com.example.fct_faroles_y_calumnias_app.network.WebSocketManager
import com.google.gson.Gson
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class HistorialActivity : AppCompatActivity() {

    private val gson = Gson()
    private val listaHistorial = mutableListOf<String>()
    private lateinit var adapterHistorial: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial)

        val lvHistorial = findViewById<ListView>(R.id.lvHistorial)
        val btnVolverPerfil = findViewById<Button>(R.id.btnVolverPerfil)

        val nombreUsuario = intent.getStringExtra("nombre_usuario") ?: ""

        adapterHistorial = ArrayAdapter(this, android.R.layout.simple_list_item_1, listaHistorial)
        lvHistorial.adapter = adapterHistorial

        // Escuchamos la respuesta del servidor
        WebSocketManager.listener = object : WebSocketListener() {

            override fun onMessage(webSocket: WebSocket, text: String) {
                val mapa = gson.fromJson(text, Map::class.java)
                val tipo = mapa["tipo"] as? String ?: return
                val datos = mapa["datos"] as? Map<*, *> ?: return

                runOnUiThread {
                    when (tipo) {
                        "historial" -> {
                            val partidas = datos["partidas"] as? List<*> ?: return@runOnUiThread
                            listaHistorial.clear()
                            for (partida in partidas) {
                                val p = partida as? Map<*, *> ?: continue
                                val ganador = p["ganador"] as? String ?: "?"
                                val coleccion = p["coleccion_usada"] as? String ?: "?"
                                val duracion = p["duracion_partida"] as? String ?: "?"
                                listaHistorial.add("Ganador: $ganador | Mazo: $coleccion | Duración: $duracion")
                            }
                            adapterHistorial.notifyDataSetChanged()
                        }
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                runOnUiThread { }
            }
        }

        // Pedimos el historial al servidor
        val mensaje = mapOf(
            "tipo" to "pedir_historial",
            "datos" to mapOf(
                "nombre_usuario" to nombreUsuario
            )
        )
        val json = gson.toJson(mensaje)
        WebSocketManager.enviarMensaje(json)

        btnVolverPerfil.setOnClickListener {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        WebSocketManager.listener = null
    }
}