package com.example.xpeando.model

data class Jefe(
    val id: Int = 0,
    val nombre: String,
    val hpMax: Int,
    var hpActual: Int,
    val recompensaMonedas: Int,
    val recompensaXP: Int,
    val icono: String, // Nombre del icono (drawable)
    val derrotado: Boolean = false
)
