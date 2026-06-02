package com.example.fct_faroles_y_calumnias_app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.fct_faroles_y_calumnias_app.R
import com.example.fct_faroles_y_calumnias_app.network.WebSocketManager
import com.google.gson.Gson
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class CrearLobbyActivity : AppCompatActivity() {

    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_lobby)


        val btnCrearSala = findViewById<Button>(R.id.btnCrearSala)

        val nombreUsuario = intent.getStringExtra("nombre_usuario") ?: ""
        val perfilId = intent.getStringExtra("perfil_id") ?: ""

        WebSocketManager.listener = object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val mapa = gson.fromJson(text, Map::class.java)
                val tipo = mapa["tipo"]

                if (tipo == "sala_creada") {
                    val codigoSala = mapa["codigo"].toString()
                    runOnUiThread {
                        val intent = Intent(this@CrearLobbyActivity, LobbyActivity::class.java)
                        intent.putExtra("nombre_usuario", nombreUsuario)
                        intent.putExtra("codigo_sala", codigoSala)
                        intent.putExtra("es_creador", true)
                        intent.putExtra("perfil_id", perfilId)  // añade esto
                        startActivity(intent)
                    }
                } else if (tipo == "sala_error") {
                    runOnUiThread {
                        Toast.makeText(this@CrearLobbyActivity, "Error al crear la sala", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                runOnUiThread {
                    Toast.makeText(this@CrearLobbyActivity, "No se puede conectar al servidor", Toast.LENGTH_SHORT).show()
                }
            }
        }
        val etNombrePartida = findViewById<EditText>(R.id.etNombrePartida)
        btnCrearSala.setOnClickListener {
            val nombrePartida = etNombrePartida.text.toString().trim()

            if (nombrePartida.isEmpty()) {
                Toast.makeText(this, "Escribe tu nombre para la partida", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val perfilId = intent.getStringExtra("perfil_id") ?: ""

            val mensaje = mapOf(
                "tipo" to "crear_sala",
                "datos" to mapOf(
                    "nombre_usuario" to nombrePartida,
                    "perfil_id" to perfilId
                )
            )
            val json = gson.toJson(mensaje)
            WebSocketManager.enviarMensaje(json)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        WebSocketManager.listener = null
    }
}