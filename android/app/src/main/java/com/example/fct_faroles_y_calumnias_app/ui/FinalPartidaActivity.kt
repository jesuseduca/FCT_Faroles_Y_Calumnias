package com.example.fct_faroles_y_calumnias_app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.fct_faroles_y_calumnias_app.R
import com.example.fct_faroles_y_calumnias_app.network.WebSocketManager
import com.google.gson.Gson
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class FinalPartidaActivity : AppCompatActivity() {

    private val gson = Gson()
    private val listaClasificacion = mutableListOf<String>()
    private lateinit var adapterClasificacion: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_final_partida)

        val tvGanador = findViewById<TextView>(R.id.tvGanador)
        val lvClasificacion = findViewById<ListView>(R.id.lvClasificacion)
        val btnVolverMenu = findViewById<Button>(R.id.btnVolverMenu)

        val ganador = intent.getStringExtra("ganador") ?: ""
        val nombreUsuario = intent.getStringExtra("nombre_usuario") ?: ""
        val codigoSala = intent.getStringExtra("codigo_sala") ?: ""

        val perfilId = intent.getStringExtra("perfil_id") ?: ""
        val gano = intent.getBooleanExtra("gano", false)
        val coleccionId = intent.getStringExtra("coleccion_id") ?: ""
        val duracion = intent.getIntExtra("duracion", 0)

        if (perfilId.isNotEmpty()) {
            val mensaje = mapOf(
                "tipo" to "guardar_partida",
                "datos" to mapOf(
                    "perfil_id" to perfilId,
                    "ganador" to ganador,
                    "coleccion_id" to coleccionId,
                    "gano" to gano,
                    "nombre" to nombreUsuario,
                    "duracion" to duracion
                )
            )
            val json = gson.toJson(mensaje)
            WebSocketManager.enviarMensaje(json)
        }

        tvGanador.text = "Ganador: $ganador"

        adapterClasificacion = ArrayAdapter(this, android.R.layout.simple_list_item_1, listaClasificacion)
        lvClasificacion.adapter = adapterClasificacion

        WebSocketManager.listener = object : WebSocketListener() {

            override fun onMessage(webSocket: WebSocket, text: String) {
                val mapa = gson.fromJson(text, Map::class.java)
                val tipo = mapa["tipo"] as? String ?: return
                val datos = mapa["datos"] as? Map<*, *> ?: return

                runOnUiThread {
                    when (tipo) {
                        "clasificacion" -> {
                            val jugadores = datos["jugadores"] as? List<*> ?: return@runOnUiThread
                            listaClasificacion.clear()
                            for (jugador in jugadores) {
                                listaClasificacion.add(jugador.toString())
                            }
                            adapterClasificacion.notifyDataSetChanged()
                        }
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                runOnUiThread {
                }
            }
        }

        btnVolverMenu.setOnClickListener {
            val intent = Intent(this, MenuActivity::class.java)
            intent.putExtra("nombre_usuario", nombreUsuario)
            intent.putExtra("es_invitado", false)
            // Limpiamos toda la pila de Activities para que no pueda volver atrás
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        WebSocketManager.listener = null
    }
}