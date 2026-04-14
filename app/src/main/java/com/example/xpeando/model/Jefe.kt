package com.example.xpeando.model

data class Jefe(
    val id: Int = 0,
    val nombre: String,
    val descripcion: String = "",
    val hpMax: Int,
    var hpActual: Int,
    val recompensaMonedas: Int,
    val recompensaXP: Int,
    val icono: String,
    val derrotado: Boolean = false,
    val nivel: Int = 1,
    val armadura: Int = 0,
    val fechaMuerte: Long = 0
)
