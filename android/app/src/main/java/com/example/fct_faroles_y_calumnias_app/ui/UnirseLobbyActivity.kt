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

class UnirseLobbyActivity : AppCompatActivity() {

    private val gson = Gson()
    private var codigoSalaGuardado = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_unirse_lobby)

        val etCodigoSala = findViewById<EditText>(R.id.etCodigoSala)
        val btnUnirse = findViewById<Button>(R.id.btnUnirse)

        val nombreUsuario = intent.getStringExtra("nombre_usuario") ?: ""
        val esInvitado = intent.getBooleanExtra("es_invitado", false)
        val perfilId = intent.getStringExtra("perfil_id") ?: ""

        WebSocketManager.listener = object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val mapa = gson.fromJson(text, Map::class.java)
                val tipo = mapa["tipo"]

                runOnUiThread {
                    if (tipo == "jugador_unido") {
                        val intent = Intent(this@UnirseLobbyActivity, LobbyActivity::class.java)
                        intent.putExtra("nombre_usuario", nombreUsuario)
                        intent.putExtra("codigo_sala", codigoSalaGuardado)
                        intent.putExtra("es_creador", false)
                        intent.putExtra("perfil_id", perfilId)
                        startActivity(intent)
                    } else if (tipo == "error") {
                        var mensaje = ""
                        if (mapa["mensaje"] != null) {
                            mensaje = mapa["mensaje"].toString()
                        }
                        Toast.makeText(this@UnirseLobbyActivity, mensaje, Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                runOnUiThread {
                    Toast.makeText(this@UnirseLobbyActivity, "No se puede conectar al servidor", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnUnirse.setOnClickListener {
            val codigoSala = etCodigoSala.text.toString().trim().uppercase()

            if (codigoSala.isEmpty()) {
                Toast.makeText(this, "Introduce el código de la sala", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            codigoSalaGuardado = codigoSala

            val mensaje = mapOf(
                "tipo" to "unirse_sala",
                "datos" to mapOf(
                    "codigo" to codigoSala,
                    "nombre_usuario" to nombreUsuario,
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