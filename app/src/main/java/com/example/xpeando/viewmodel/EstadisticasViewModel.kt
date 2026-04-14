package com.example.xpeando.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xpeando.repository.DataRepository
import com.example.xpeando.utils.LogroManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EstadisticasViewModel(private val repository: DataRepository) : ViewModel() {

    private val _state = MutableStateFlow(EstadisticasState())
    val state: StateFlow<EstadisticasState> = _state.asStateFlow()

    fun cargarEstadisticas(correo: String) {
        viewModelScope.launch {
            val usuario = repository.obtenerUsuarioLogueado(correo)
            val totalTareas = repository.obtenerTotalTareasCompletadas(correo)
            val totalDailies = repository.obtenerTotalDailiesCompletadas(correo)
            val totalHabitos = repository.obtenerTotalHabitosCompletados(correo)
            val xpSemanal = repository.obtenerXPSemanal(correo)
            
            val logros = usuario?.let {
                LogroManager.obtenerLogrosDefinidos(repository, it)
            } ?: emptyList()

            _state.value = EstadisticasState(
                usuario = usuario,
                totalTareas = totalTareas,
                totalDailies = totalDailies,
                totalHabitos = totalHabitos,
                xpSemanal = xpSemanal,
                logros = logros
            )
        }
    }
}
