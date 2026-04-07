package com.example.xpeando.model

data class Daily(
    val id: Int = 0,
    val nombre: String,
    val experiencia: Int = 15,
    val monedas: Int = 10,
    val completadaHoy: Boolean = false,
    val ultimaVezCompletada: String = "" // Fecha en formato YYYY-MM-DD
)
