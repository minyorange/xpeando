package com.example.xpeando.model

data class Progreso(
    val id: Int = 0,
    val correo_usuario: String,
    val fecha: String, // format yyyy-MM-dd
    val xp: Int,
    val monedas: Int,
    val tipo: String // "TAREA", "DAILY", "HABITO", "JEFE"
)
