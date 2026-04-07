package com.example.xpeando.model

data class Logro(
    val nombre: String,
    val descripcion: String,
    val requisito: Int,
    val progresoActual: Int,
    val completado: Boolean,
    val iconoResId: Int
)
