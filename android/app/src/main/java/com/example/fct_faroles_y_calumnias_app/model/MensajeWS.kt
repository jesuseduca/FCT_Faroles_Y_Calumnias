package com.example.fct_faroles_y_calumnias_app.model

data class MensajeWS(
    val tipo: String = "",
    val datos: Map<String, Any> = emptyMap()
)