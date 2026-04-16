package com.example.xpeando.model

data class Daily(
    val id: Int = 0,
    val correo_usuario: String = "",
    val nombre: String = "",
    val dificultad: Int = 1,
    val experiencia: Int = 15,
    val monedas: Int = 10,
    val completadaHoy: Boolean = false,
    val ultimaVezCompletada: String = "" // Fecha en formato YYYY-MM-DD
)
