package com.example.xpeando.model

data class Nota(
    val id: Int = 0,
    val correo_usuario: String,
    var titulo: String,
    var contenido: String,
    val fecha: Long = System.currentTimeMillis()
)