package com.example.xpeando.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xpeando.model.Daily
import com.example.xpeando.model.Usuario
import com.example.xpeando.repository.DataRepository
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DailiesViewModel(private val repository: DataRepository) : ViewModel() {

    private val _dailies = MutableStateFlow<List<Daily>>(emptyList())
    val dailies: StateFlow<List<Daily>> = _dailies.asStateFlow()

    private val _usuario = MutableStateFlow<Usuario?>(null)
    val usuario: StateFlow<Usuario?> = _usuario.asStateFlow()

    fun cargarDailies(correo: String) {
        if (correo.isEmpty()) return
        viewModelScope.launch {
            val todas = repository.obtenerTodasDailies(correo)
            _dailies.value = todas
            
            val u = repository.obtenerUsuarioLogueado(correo)
            _usuario.value = u
        }
    }

    fun completarDaily(context: Context, daily: Daily, correo: String, onNivelSubido: (Int) -> Unit) {
        viewModelScope.launch {
            val usuarioAntes = repository.obtenerUsuarioLogueado(correo)
            val nivelAntes = usuarioAntes?.nivel ?: 1

            // 1. Marcar como completada en la nube
            repository.actualizarEstadoDaily(daily, true)
            // 2. Aplicar lógica de RPG (XP, Monedas, Level Up, Atributos)
            repository.actualizarProgresoUsuario(correo, daily.experiencia, daily.monedas, tipoAccion = "DAILY")
            // 3. Actualizar racha
            repository.actualizarRacha(correo)
            
            val uFinal = repository.obtenerUsuarioLogueado(correo)
            _usuario.value = uFinal
            
            val nivelDespues = uFinal?.nivel ?: 1
            if (nivelDespues > nivelAntes) {
                onNivelSubido(nivelDespues)
            }
            
            cargarDailies(correo)
        }
    }

    fun procesarDailiesFallidas(correo: String, dias: Int, onResultado: (Int) -> Unit) {
        viewModelScope.launch {
            val danio = repository.procesarDailiesFallidas(correo, dias)
            if (danio > 0) {
                onResultado(danio)
            }
            cargarDailies(correo)
        }
    }

    fun insertarDaily(daily: Daily) {
        viewModelScope.launch {
            repository.insertarDaily(daily)
            cargarDailies(daily.correo_usuario)
        }
    }

    fun eliminarDaily(id: Int, correo: String) {
        viewModelScope.launch {
            repository.eliminarDaily(id, correo)
            cargarDailies(correo)
        }
    }
}
