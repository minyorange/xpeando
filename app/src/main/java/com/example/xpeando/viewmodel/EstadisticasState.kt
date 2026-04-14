package com.example.xpeando.viewmodel

import com.example.xpeando.model.Usuario
import com.example.xpeando.model.Logro

data class EstadisticasState(
    val usuario: Usuario? = null,
    val totalTareas: Int = 0,
    val totalDailies: Int = 0,
    val totalHabitos: Int = 0,
    val xpSemanal: Map<String, Int> = emptyMap(),
    val logros: List<Logro> = emptyList()
)
