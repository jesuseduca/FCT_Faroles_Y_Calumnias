package com.example.fct_faroles_y_calumnias_app.ui

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.GridLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.fct_faroles_y_calumnias_app.R
import com.example.fct_faroles_y_calumnias_app.model.Jugador
import com.example.fct_faroles_y_calumnias_app.network.WebSocketManager
import com.google.gson.Gson
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class JuegoActivity : AppCompatActivity() {

    private val gson = Gson()
    private val listaJugadores = mutableListOf<Jugador>()
    private lateinit var adapterJugadores: JugadorAdapter
    private var temporizador: CountDownTimer? = null
    private var jugadorSeleccionado: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_juego)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // No hacemos nada
            }
        })

        val gridPalabras = findViewById<GridLayout>(R.id.gridPalabras)
        val lvJugadoresVivos = findViewById<ListView>(R.id.lvJugadoresVivos)
        val btnVotar = findViewById<Button>(R.id.btnVotar)

        val nombreUsuario = intent.getStringExtra("nombre_usuario") ?: ""
        val codigoSala = intent.getStringExtra("codigo_sala") ?: ""
        val esFarol = intent.getBooleanExtra("es_farol", false)
        val palabra = intent.getStringExtra("palabra") ?: ""
        val palabras = intent.getStringArrayListExtra("palabras") ?: ArrayList()
        val perfilId = intent.getStringExtra("perfil_id") ?: ""

        adapterJugadores = JugadorAdapter(this, listaJugadores)
        lvJugadoresVivos.adapter = adapterJugadores

        for (p in palabras) {
            val tv = TextView(this)
            val params = GridLayout.LayoutParams()
            params.width = 0
            params.height = GridLayout.LayoutParams.WRAP_CONTENT
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            params.setMargins(4, 4, 4, 4)
            tv.layoutParams = params
            tv.text = p
            tv.textSize = 14f
            tv.setPadding(8, 8, 8, 8)
            tv.gravity = android.view.Gravity.CENTER
            tv.setBackgroundResource(android.R.drawable.dialog_holo_light_frame)

            if (!esFarol && p == palabra) {
                tv.setTextColor(getColor(R.color.rojo_principal))
            } else {
                tv.setTextColor(android.graphics.Color.BLACK)
            }

            gridPalabras.addView(tv)
        }

        lvJugadoresVivos.setOnItemClickListener { _, _, position, _ ->
            jugadorSeleccionado = listaJugadores[position].nombre
            adapterJugadores.posicionSeleccionada = position
            adapterJugadores.notifyDataSetChanged()
        }

        btnVotar.setOnClickListener {
            if (jugadorSeleccionado.isEmpty()) {
                Toast.makeText(this, "Selecciona un jugador primero", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val mensaje = mapOf(
                "tipo" to "voto",
                "datos" to mapOf(
                    "codigo_sala" to codigoSala,
                    "jugador_votado" to jugadorSeleccionado,
                    "votante" to nombreUsuario
                )
            )
            val json = gson.toJson(mensaje)
            WebSocketManager.enviarMensaje(json)
            btnVotar.isEnabled = false
        }

        WebSocketManager.listener = object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val mapa = gson.fromJson(text, Map::class.java)
                val tipo = mapa["tipo"]

                runOnUiThread {
                      if (tipo == "resultado_voto") {
                        var nombreEliminado = ""
                        var vidasRestantes = 0
                        if (mapa["jugador_eliminado"] != null) {
                            nombreEliminado = mapa["jugador_eliminado"].toString()
                        }
                        if (mapa["vidas_restantes"] != null) {
                            vidasRestantes = (mapa["vidas_restantes"] as Double).toInt()
                        }

                        var i = 0
                        while (i < listaJugadores.size) {
                            if (listaJugadores[i].nombre == nombreEliminado) {
                                if (vidasRestantes <= 0) {
                                    listaJugadores.removeAt(i)
                                } else {
                                    listaJugadores[i] = listaJugadores[i].copy(vidas = vidasRestantes)
                                }
                                break
                            }
                            i++
                        }
                        adapterJugadores.notifyDataSetChanged()
                        jugadorSeleccionado = ""
                        btnVotar.isEnabled = true
                    } else if (tipo == "partida_iniciada") {
                        var esFarolNuevo = false
                        var palabraNueva = ""
                        val palabrasNuevas = ArrayList<String>()

                        if (mapa["es_farol"] != null) {
                            esFarolNuevo = mapa["es_farol"] as Boolean
                        }
                        if (mapa["palabra"] != null) {
                            palabraNueva = mapa["palabra"].toString()
                        }
                        if (mapa["palabras"] != null) {
                            val lista = mapa["palabras"] as List<*>
                            for (p in lista) {
                                if (p != null) {
                                    palabrasNuevas.add(p.toString())
                                }
                            }
                        }

                        val i = Intent(this@JuegoActivity, RascaRolActivity::class.java)
                        i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        i.putExtra("nombre_usuario", nombreUsuario)
                        i.putExtra("codigo_sala", codigoSala)
                        i.putExtra("es_farol", esFarolNuevo)
                        i.putExtra("palabra", palabraNueva)
                        i.putStringArrayListExtra("palabras", palabrasNuevas)
                        i.putExtra("perfil_id", perfilId)
                        startActivity(i)
                        finish()
                    }  else if (tipo == "partida_terminada") {
                    var ganador = ""
                    var gano = false
                    var coleccionId = ""
                    var duracion = 0

                    if (mapa["ganador"] != null) {
                        ganador = mapa["ganador"].toString()
                    }
                    if (mapa["gano"] != null) {
                        gano = mapa["gano"] as Boolean
                    }
                    if (mapa["coleccion_id"] != null) {
                        coleccionId = mapa["coleccion_id"].toString()
                    }
                     if (mapa["duracion"] != null) {
                        duracion = (mapa["duracion"] as Double).toInt()
                    }

                    val intent = Intent(this@JuegoActivity, FinalPartidaActivity::class.java)
                    intent.putExtra("ganador", ganador)
                    intent.putExtra("gano", gano)
                    intent.putExtra("coleccion_id", coleccionId)
                    intent.putExtra("nombre_usuario", nombreUsuario)
                    intent.putExtra("codigo_sala", codigoSala)
                    intent.putExtra("perfil_id", perfilId)
                          intent.putExtra("duracion", duracion)
                    startActivity(intent)
                    finish()
                } else if (tipo == "lista_jugadores") {
                        val jugadores = mapa["jugadores"]
                        if (jugadores != null) {
                            val lista = jugadores as List<*>
                            listaJugadores.clear()
                            for (jugador in lista) {
                                if (jugador != null) {
                                    val j = jugador as Map<*, *>
                                    var nombre = ""
                                    var vidas = 3
                                    if (j["nombre"] != null) {
                                        nombre = j["nombre"].toString()
                                    }
                                    if (j["vidas"] != null) {
                                        vidas = (j["vidas"] as Double).toInt()
                                    }
                                    listaJugadores.add(Jugador(nombre = nombre, vidas = vidas))
                                }
                            }
                            adapterJugadores.notifyDataSetChanged()
                        }
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                runOnUiThread {
                    Toast.makeText(this@JuegoActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val mensajeLista = mapOf(
            "tipo" to "pedir_lista_jugadores",
            "datos" to mapOf(
                "codigo" to codigoSala
            )
        )
        val jsonLista = gson.toJson(mensajeLista)
        WebSocketManager.enviarMensaje(jsonLista)

    }

    override fun onDestroy() {
        super.onDestroy()
        temporizador?.cancel()
        WebSocketManager.listener = null
    }
}