package com.example.fct_faroles_y_calumnias_app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.fct_faroles_y_calumnias_app.R
import com.example.fct_faroles_y_calumnias_app.network.WebSocketManager
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class PerfilActivity : AppCompatActivity() {

    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        val tvNombre = findViewById<TextView>(R.id.tvNombre)
        val tvEmail = findViewById<TextView>(R.id.tvEmail)
        val tvPartidasJugadas = findViewById<TextView>(R.id.tvPartidasJugadas)
        val tvPartidasGanadas = findViewById<TextView>(R.id.tvPartidasGanadas)
        val btnVerHistorial = findViewById<Button>(R.id.btnVerHistorial)
        val btnVolver = findViewById<Button>(R.id.btnVolver)

        val nombreUsuario = intent.getStringExtra("nombre_usuario") ?: ""
        val perfilId = intent.getStringExtra("perfil_id") ?: ""

        WebSocketManager.listener = object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val json = gson.fromJson(text, JsonObject::class.java)
                val tipo = json.get("tipo").asString

                if (tipo == "perfil_ok") {
                    var nombre = ""
                    var email = ""
                    var partidasJugadas = 0
                    var partidasGanadas = 0

                    if (json.has("nombre")) {
                        nombre = json.get("nombre").asString
                    }
                    if (json.has("email")) {
                        email = json.get("email").asString
                    }
                    if (json.has("partidas_jugadas")) {
                        partidasJugadas = json.get("partidas_jugadas").asInt
                    }
                    if (json.has("partidas_ganadas")) {
                        partidasGanadas = json.get("partidas_ganadas").asInt
                    }

                    runOnUiThread {
                        tvNombre.text = nombre
                        tvEmail.text = email
                        tvPartidasJugadas.text = "Partidas jugadas: $partidasJugadas"
                        tvPartidasGanadas.text = "Partidas ganadas: $partidasGanadas"
                    }
                } else if (tipo == "perfil_error") {
                    runOnUiThread {
                        Toast.makeText(this@PerfilActivity, "No se pudo cargar el perfil", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        val mensaje = mapOf(
            "tipo" to "pedir_perfil",
            "datos" to mapOf(
                "perfil_id" to perfilId
            )
        )
        val mensajeJson = gson.toJson(mensaje)
        WebSocketManager.enviarMensaje(mensajeJson)

        btnVerHistorial.setOnClickListener {
            val intent = Intent(this, HistorialActivity::class.java)
            intent.putExtra("nombre_usuario", nombreUsuario)
            intent.putExtra("perfil_id", perfilId)
            startActivity(intent)
        }

        btnVolver.setOnClickListener {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        WebSocketManager.listener = null
    }
}