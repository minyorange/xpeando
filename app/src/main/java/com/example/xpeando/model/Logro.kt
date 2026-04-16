package com.example.xpeando.model

data class Logro(
    val nombre: String = "",
    val descripcion: String = "",
    val requisito: Int = 0,
    val progresoActual: Int = 0,
    val completado: Boolean = false,
    val iconoResId: Int = 0
)
