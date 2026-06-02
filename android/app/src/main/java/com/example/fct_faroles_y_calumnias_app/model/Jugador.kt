package com.example.fct_faroles_y_calumnias_app.model

data class Jugador(
    val id: String = "",
    val nombre: String = "",
    val esAdivino: Boolean = false,
    val vidas: Int = 3,
    val eliminado: Boolean = false
)