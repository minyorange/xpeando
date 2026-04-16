package com.example.xpeando.model

data class Articulo(
    val id: Int = 0,
    val nombre: String = "",
    val tipo: String = "CONSUMIBLE", // "EQUIPO", "CONSUMIBLE"
    val subtipo: String = "POCION", // "ARMA", "ARMADURA", "POCION"
    val precio: Int = 0,
    val bonusFza: Int = 0,
    val bonusInt: Int = 0,
    val bonusCon: Int = 0,
    val bonusPer: Int = 0,
    val bonusHp: Int = 0,
    val icono: String = "premios",
    var equipado: Boolean = false,
    var esPropio: Boolean = false, // Para saber si el usuario ya lo tiene
    var cantidad: Int = 1
)
