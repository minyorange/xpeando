package com.example.xpeando.model

data class Recompensa(
    val id: Int = 0,
    val correo_usuario: String = "",
    val nombre: String,
    val precio: Int,
    val icono: String = "ic_recompensas"
)
