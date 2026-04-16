package com.example.xpeando.model

data class Jefe(
    val id: Int = 0,
    val nombre: String = "",
    val descripcion: String = "",
    val hpMax: Int = 100,
    var hpActual: Int = 100,
    val recompensaMonedas: Int = 0,
    val recompensaXP: Int = 0,
    val icono: String = "monster_1",
    val derrotado: Boolean = false,
    val nivel: Int = 1,
    val armadura: Int = 0,
    val fechaMuerte: Long = 0
)
