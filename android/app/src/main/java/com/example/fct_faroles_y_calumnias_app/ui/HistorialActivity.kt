package com.example.fct_faroles_y_calumnias_app.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.example.fct_faroles_y_calumnias_app.R
import com.example.fct_faroles_y_calumnias_app.network.WebSocketManager
import com.google.gson.Gson
import com.google.gson.JsonObject
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

        val perfilId = intent.getStringExtra("perfil_id") ?: ""

        adapterHistorial = ArrayAdapter(this, android.R.layout.simple_list_item_1, listaHistorial)
        lvHistorial.adapter = adapterHistorial

        WebSocketManager.listener = object : WebSocketListener() {

            override fun onMessage(webSocket: WebSocket, text: String) {
                val json = gson.fromJson(text, JsonObject::class.java)
                val tipo = json.get("tipo").asString

                if (tipo == "historial") {
                    val partidas = json.getAsJsonArray("historial")

                    runOnUiThread {
                        listaHistorial.clear()

                        for (elemento in partidas) {
                            val p = elemento.asJsonObject
                            var ganador = "?"
                            var coleccion = "?"
                            var duracion = "?"
                            var nombre = "?"
                            var gano = false
                            var vecesFarol = 0
                            var ganoTexto = ""

                            if (p.has("ganador")) {
                                ganador = p.get("ganador").asString
                            }
                            if (p.has("coleccion_usada")) {
                                coleccion = p.get("coleccion_usada").asString
                            }
                            if (p.has("duracion_partida")) {
                                duracion = p.get("duracion_partida").asString
                            }
                            if (p.has("nombre")) {
                                nombre = p.get("nombre").asString
                            }
                            if (p.has("gano")) {
                                gano = p.get("gano").asBoolean
                            }
                            if (p.has("veces_farol")) {
                                vecesFarol = p.get("veces_farol").asInt
                            }
                            if (gano) {
                                ganoTexto = "Sí"
                            } else {
                                ganoTexto = "No"
                            }

                            listaHistorial.add("Ganador: $ganador | Mazo: $coleccion | ¿Ganaste? $ganoTexto | Farol: $vecesFarol veces")
                        }

                        adapterHistorial.notifyDataSetChanged()
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                runOnUiThread { }
            }
        }

        val mensaje = mapOf(
            "tipo" to "pedir_historial",
            "datos" to mapOf(
                "perfil_id" to perfilId
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