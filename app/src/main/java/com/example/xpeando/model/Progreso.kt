package com.example.xpeando.model

data class Progreso(
    val id: Int = 0,
    val correo_usuario: String = "",
    val fecha: String = "", // format yyyy-MM-dd
    val xp: Int = 0,
    val monedas: Int = 0,
    val tipo: String = "TAREA" // "TAREA", "DAILY", "HABITO", "JEFE"
)
