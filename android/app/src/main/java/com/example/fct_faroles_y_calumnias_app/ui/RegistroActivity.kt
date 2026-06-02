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
import com.google.gson.JsonObject
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class RegistroActivity : AppCompatActivity() {

    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        val etNombre = findViewById<EditText>(R.id.etNombre)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnRegistrar = findViewById<Button>(R.id.btnRegistrar)
        val btnVolver = findViewById<Button>(R.id.btnVolver)

        WebSocketManager.listener = object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val json = gson.fromJson(text, JsonObject::class.java)
                val tipo = json.get("tipo").asString

                runOnUiThread {
                    if (tipo == "registro_ok") {
                        Toast.makeText(this@RegistroActivity, "Cuenta creada, inicia sesión", Toast.LENGTH_SHORT).show()
                        finish()
                    } else if (tipo == "registro_error") {
                        Toast.makeText(this@RegistroActivity, "El email ya está registrado", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        btnRegistrar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (nombre.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Rellena todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            enviarRegistro(nombre, email, password)
        }

        btnVolver.setOnClickListener {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        WebSocketManager.listener = null
    }

    private fun enviarRegistro(nombre: String, email: String, password: String) {
        val mensaje = mapOf(
            "tipo" to "registro",
            "datos" to mapOf(
                "nombre" to nombre,
                "email" to email,
                "password" to password
            )
        )
        val json = gson.toJson(mensaje)
        WebSocketManager.enviarMensaje(json)
    }
}