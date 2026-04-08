package com.example.xpeando.model

data class Usuario(
    val id: Int = 0,
    val nombre: String,
    val correo: String,
    val contrasena: String,
    val nivel: Int = 1,
    val experiencia: Int = 0,
    val monedas: Int = 0,
    val hp: Int = 50,
    val fuerza: Double = 1.0,
    val inteligencia: Double = 1.0,
    val constitucion: Double = 1.0,
    val percepcion: Double = 1.0,
    val puntosDisponibles: Int = 0,
    val totalHabitos: Int = 0,
    val rachaActual: Int = 0,
    val rachaMaxima: Int = 0,
    val ultimaFechaActividad: String? = null
)
