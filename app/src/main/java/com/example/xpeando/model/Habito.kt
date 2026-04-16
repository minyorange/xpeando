package com.example.xpeando.model

data class Habito(
    val id: Int = 0,
    val correo_usuario: String = "",
    val nombre: String = "",
    val experiencia: Int = 10,
    val monedas: Int = 5,
    val completadoHoy: Boolean = false,
    val atributo: String = "Fuerza", // Fuerza, Inteligencia, Constitución, Percepción
    val vecesCompletado: Int = 0
)
