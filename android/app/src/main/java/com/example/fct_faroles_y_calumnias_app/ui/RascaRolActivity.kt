package com.example.fct_faroles_y_calumnias_app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.fct_faroles_y_calumnias_app.R
import com.example.fct_faroles_y_calumnias_app.utils.ScratchView

class RascaRolActivity : AppCompatActivity() {

    private val frases = listOf(
        "El que no arriesga, no gana",
        "La verdad tiene muchas caras",
        "Confía en nadie, duda de todos",
        "El farol más grande es el silencio",
        "Miente bien o pierde todo",
        "Los ojos no saben mentir",
        "Hoy todos son sospechosos"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rasca_rol)

        val tvRol = findViewById<TextView>(R.id.tvRol)
        val tvFrase = findViewById<TextView>(R.id.tvFrase)
        val scratchView = findViewById<ScratchView>(R.id.scratchView)
        val btnListo = findViewById<Button>(R.id.btnListoRasca)

        val esFarol = intent.getBooleanExtra("es_farol", false)
        val nombreUsuario = intent.getStringExtra("nombre_usuario") ?: ""
        val codigoSala = intent.getStringExtra("codigo_sala") ?: ""
        val palabra = intent.getStringExtra("palabra") ?: ""
        val palabras = intent.getStringArrayListExtra("palabras") ?: ArrayList()
        val perfilId = intent.getStringExtra("perfil_id") ?: ""

        tvFrase.text = frases.random()

        if (esFarol) {
            tvRol.text = "¡Eres el\nFarol!"
            tvRol.setTextColor(getColor(R.color.rojo_principal))
        } else {
            tvRol.text = "Eres un\nLugareño"
            tvRol.setTextColor(android.graphics.Color.WHITE)
        }

        scratchView.onScratchedListener = {
            runOnUiThread {
                btnListo.visibility = View.VISIBLE
            }
        }

        btnListo.setOnClickListener {
            val i = Intent(this, JuegoActivity::class.java)
            i.putExtra("nombre_usuario", nombreUsuario)
            i.putExtra("codigo_sala", codigoSala)
            i.putExtra("es_farol", esFarol)
            i.putExtra("palabra", palabra)
            i.putStringArrayListExtra("palabras", palabras)
            i.putExtra("perfil_id", perfilId)
            startActivity(i)
            finish()
        }
    }
}