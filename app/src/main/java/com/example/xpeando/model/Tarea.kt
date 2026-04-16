package com.example.xpeando.model

data class Tarea(
    val id: Int = 0,
    val correo_usuario: String = "",
    val nombre: String = "",
    val dificultad: Int = 1,
    val experiencia: Int = 20,
    val monedas: Int = 10,
    val completada: Boolean = false
)
