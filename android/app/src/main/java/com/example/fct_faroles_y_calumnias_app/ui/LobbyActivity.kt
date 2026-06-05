package com.example.fct_faroles_y_calumnias_app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.fct_faroles_y_calumnias_app.R
import com.example.fct_faroles_y_calumnias_app.network.WebSocketManager
import com.google.gson.Gson
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class LobbyActivity : AppCompatActivity() {

    private val gson = Gson()
    private val listaJugadores = mutableListOf<String>()
    private lateinit var adapterJugadores: ArrayAdapter<String>
    private val listaMazos = mutableListOf<String>()
    private val listaIds = mutableListOf<String>()
    private lateinit var adapterMazos: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lobby)

        val tvCodigoSala = findViewById<TextView>(R.id.tvCodigoSala)
        val lvJugadores = findViewById<ListView>(R.id.lvJugadores)
        val spinnerMazos = findViewById<Spinner>(R.id.spinnerMazos)
        val btnEmpezarPartida = findViewById<Button>(R.id.btnEmpezarPartida)

        val nombreUsuario = intent.getStringExtra("nombre_usuario") ?: ""
        val codigoSala = intent.getStringExtra("codigo_sala") ?: ""
        val esCreador = intent.getBooleanExtra("es_creador", false)
        val perfilId = intent.getStringExtra("perfil_id") ?: ""

        tvCodigoSala.text = "Código: $codigoSala"

        if (!esCreador) {
            btnEmpezarPartida.visibility = View.GONE
            spinnerMazos.visibility = View.GONE
        }

        adapterJugadores = ArrayAdapter(this, R.layout.item_jugador, listaJugadores)
        lvJugadores.adapter = adapterJugadores

        adapterMazos = ArrayAdapter(this, android.R.layout.simple_spinner_item, listaMazos)
        adapterMazos.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMazos.adapter = adapterMazos

        listaJugadores.add(nombreUsuario)
        adapterJugadores.notifyDataSetChanged()

        WebSocketManager.listener = object : WebSocketListener() {

            override fun onMessage(webSocket: WebSocket, text: String) {
                val mapa = gson.fromJson(text, Map::class.java)
                val tipo = mapa["tipo"]



                runOnUiThread {
                    if (tipo == "jugador_unido") {
                        val jugadores = mapa["jugadores"]
                        if (jugadores != null) {
                            val lista = jugadores as List<*>
                            listaJugadores.clear()
                            for (j in lista) {
                                if (j != null && j.toString().isNotEmpty()) {
                                    listaJugadores.add(j.toString())
                                }
                            }
                            adapterJugadores.notifyDataSetChanged()
                        }
                    } else if (tipo == "lista_colecciones") {
                        val colecciones = mapa["colecciones"]
                        if (colecciones != null) {
                            val lista = colecciones as List<*>
                            listaMazos.clear()
                            listaIds.clear()
                            for (c in lista) {
                                if (c != null) {
                                    val coleccion = c as Map<*, *>
                                    listaMazos.add(coleccion["nombre"].toString())
                                    listaIds.add(coleccion["id"].toString())
                                }
                            }
                            adapterMazos.notifyDataSetChanged()
                        }
                    } else if (tipo == "partida_iniciada") {
                    var esFarol = false
                    var palabra = ""
                    val palabras = ArrayList<String>()

                    if (mapa["es_farol"] != null) {
                        esFarol = mapa["es_farol"] as Boolean
                    }
                    if (mapa["palabra"] != null) {
                        palabra = mapa["palabra"].toString()
                    }
                    if (mapa["palabras"] != null) {
                        val lista = mapa["palabras"] as List<*>
                        for (p in lista) {
                            if (p != null) {
                                palabras.add(p.toString())
                            }
                        }
                    }

                    val intent = Intent(this@LobbyActivity, RascaRolActivity::class.java)
                    intent.putExtra("nombre_usuario", nombreUsuario)
                    intent.putExtra("codigo_sala", codigoSala)
                    intent.putExtra("es_farol", esFarol)
                    intent.putExtra("palabra", palabra)
                    intent.putStringArrayListExtra("palabras", palabras)
                        intent.putExtra("perfil_id", perfilId)
                        WebSocketManager.listener = null
                    startActivity(intent)
                }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                runOnUiThread {
                    Toast.makeText(this@LobbyActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
                }
            }
        }



        if (esCreador) {
            val mensaje = mapOf(
                "tipo" to "pedir_colecciones",
                "datos" to mapOf<String, String>()
            )
            val json = gson.toJson(mensaje)
            WebSocketManager.enviarMensaje(json)
        }

        btnEmpezarPartida.setOnClickListener {
            val indice = spinnerMazos.selectedItemPosition
            if (indice < 0 || listaIds.isEmpty()) {
                Toast.makeText(this, "Elige una colección primero", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val idColeccion = listaIds[indice]
            val mensaje = mapOf(
                "tipo" to "cerrar_lobby",
                "datos" to mapOf(
                    "codigo" to codigoSala,
                    "coleccion_id" to idColeccion
                )
            )
            val json = gson.toJson(mensaje)
            WebSocketManager.enviarMensaje(json)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

    }
}