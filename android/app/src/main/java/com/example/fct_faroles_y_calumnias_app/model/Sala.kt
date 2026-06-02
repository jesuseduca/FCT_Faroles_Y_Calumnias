package com.example.fct_faroles_y_calumnias_app.model

data class Sala(
    val codigoSala: String = "",
    val nombreSala: String = "",
    val jugadores: List<Jugador> = emptyList(),
    val coleccionElegida: String = ""
)