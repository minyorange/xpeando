package com.example.xpeando.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xpeando.repository.DataRepository
import com.example.xpeando.model.Jefe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RpgViewModel(private val repository: DataRepository) : ViewModel() {
    private val _jefeActivo = MutableStateFlow<Jefe?>(null)
    val jefeActivo: StateFlow<Jefe?> = _jefeActivo.asStateFlow()

    private val _historialJefes = MutableStateFlow<List<Jefe>>(emptyList())
    val historialJefes: StateFlow<List<Jefe>> = _historialJefes.asStateFlow()

    fun cargarJefeActivo(correo: String) {
        viewModelScope.launch {
            val jefe = withContext(Dispatchers.IO) {
                repository.obtenerJefeActivo(correo)
            }
            _jefeActivo.value = jefe
        }
    }

    fun cargarHistorial(correo: String) {
        viewModelScope.launch {
            val historial = withContext(Dispatchers.IO) {
                repository.obtenerJefesDerrotados(correo)
            }
            _historialJefes.value = historial
        }
    }

    fun atacarJefe(danioBase: Int, correo: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val derrotado = withContext(Dispatchers.IO) {
                repository.atacarJefe(danioBase, correo)
            }
            cargarJefeActivo(correo)
            if (derrotado) {
                cargarHistorial(correo)
            }
            onResult(derrotado)
        }
    }
    
    suspend fun obtenerUltimoJefeDerrotadoTime(correo: String): Long {
        return withContext(Dispatchers.IO) {
            repository.obtenerUltimoJefeDerrotadoTime(correo)
        }
    }
}
