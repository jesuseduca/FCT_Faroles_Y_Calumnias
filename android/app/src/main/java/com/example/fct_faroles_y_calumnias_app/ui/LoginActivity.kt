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

class LoginActivity : AppCompatActivity() {

    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        WebSocketManager.conectar()

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnIrRegistro = findViewById<Button>(R.id.btnIrRegistro)






        btnLogin.setOnClickListener {
            val emailONombre = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (emailONombre.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "No puede haber campos vacíos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            enviarLogin(emailONombre, password)
        }

        btnIrRegistro.setOnClickListener {
            val intent = Intent(this, RegistroActivity::class.java)
            startActivity(intent)

        }

        val btnInvitado = findViewById<Button>(R.id.btnInvitado)

        btnInvitado.setOnClickListener {
            val intent = Intent(this, MenuActivity::class.java)
            intent.putExtra("nombre_usuario", "Invitado")
            intent.putExtra("es_invitado", true)
            startActivity(intent)
        }

        WebSocketManager.listener = object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val mapa = gson.fromJson(text, Map::class.java)
                val tipo = mapa["tipo"] as String

                runOnUiThread {
                    when (tipo) {
                        "login_ok" -> {
                            var nombre = ""
                            var perfilId = ""

                            if (mapa["nombre"] != null) {
                                nombre = mapa["nombre"] as String
                            }
                            if (mapa["perfil_id"] != null) {
                                perfilId = mapa["perfil_id"] as String
                            }

                            val intent = Intent(this@LoginActivity, MenuActivity::class.java)
                            intent.putExtra("nombre_usuario", nombre)
                            intent.putExtra("perfil_id", perfilId)
                            intent.putExtra("es_invitado", false)
                            startActivity(intent)
                        }
                        "login_error" -> {
                            Toast.makeText(this@LoginActivity, "Email o contraseña incorrectos", Toast.LENGTH_SHORT).show()
                        }
                        "registro_ok" -> {
                            Toast.makeText(this@LoginActivity, "Cuenta creada, inicia sesión", Toast.LENGTH_SHORT).show()
                        }
                        "registro_error" -> {
                            Toast.makeText(this@LoginActivity, "El usuario ya existe", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                runOnUiThread {
                    Toast.makeText(this@LoginActivity, "No se puede conectar al servidor", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        WebSocketManager.listener = null
    }

    private fun enviarLogin(emailONombre: String, password: String) {
        val mensaje = mapOf(
            "tipo" to "login",
            "datos" to mapOf(
                "emailONombre" to emailONombre,
                "password" to password
            )
        )
        val json = gson.toJson(mensaje)
        WebSocketManager.enviarMensaje(json)
    }
}