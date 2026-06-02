package com.example.fct_faroles_y_calumnias_app.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.example.fct_faroles_y_calumnias_app.R
import com.example.fct_faroles_y_calumnias_app.model.Jugador

class JugadorAdapter(private val context: Context, private val jugadores: MutableList<Jugador>) : BaseAdapter() {

    override fun getCount(): Int {
        return jugadores.size
    }

    override fun getItem(position: Int): Jugador {
        return jugadores[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val vista = LayoutInflater.from(context).inflate(R.layout.vidas_jugador, parent, false)

        val tvNombre = vista.findViewById<TextView>(R.id.tvNombreJugador)
        val tvVidas = vista.findViewById<TextView>(R.id.tvVidas)

        val jugador = jugadores[position]
        tvNombre.text = jugador.nombre

        var corazones = ""
        var i = 0
        while (i < jugador.vidas) {
            corazones += "❤️"
            i++
        }
        tvVidas.text = corazones

        return vista
    }
}