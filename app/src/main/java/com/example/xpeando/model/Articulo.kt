package com.example.xpeando.model

data class Articulo(
    val id: Int = 0,
    val nombre: String,
    val tipo: String, // "EQUIPO", "CONSUMIBLE"
    val subtipo: String, // "ARMA", "ARMADURA", "POCION"
    val precio: Int,
    val bonusFza: Int = 0,
    val bonusInt: Int = 0,
    val bonusCon: Int = 0,
    val bonusPer: Int = 0,
    val bonusHp: Int = 0,
    val icono: String = "ic_recompensas",
    var equipado: Boolean = false,
    var esPropio: Boolean = false // Para saber si el usuario ya lo tiene
)
