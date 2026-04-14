package com.example.xpeando.viewmodel

import com.example.xpeando.model.Articulo
import com.example.xpeando.model.Recompensa
import com.example.xpeando.model.Usuario

data class RecompensasState(
    val usuario: Usuario? = null,
    val recompensasPersonales: List<Recompensa> = emptyList(),
    val armeria: List<Articulo> = emptyList()
)
