package com.example.xpeando.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xpeando.repository.DataRepository
import com.example.xpeando.model.Jefe
import com.example.xpeando.model.Articulo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RpgViewModel(private val repository: DataRepository) : ViewModel() {

    private val _jefeActivo = MutableStateFlow<Jefe?>(null)
    val jefeActivo: StateFlow<Jefe?> = _jefeActivo.asStateFlow()

    private val _historialJefes = MutableStateFlow<List<Jefe>>(emptyList())
    val historialJefes: StateFlow<List<Jefe>> = _historialJefes.asStateFlow()

    private val _tienda = MutableStateFlow<List<Articulo>>(emptyList())
    val tienda: StateFlow<List<Articulo>> = _tienda.asStateFlow()

    private val _inventario = MutableStateFlow<List<Articulo>>(emptyList())
    val inventario: StateFlow<List<Articulo>> = _inventario.asStateFlow()

    fun cargarJefeActivo(correo: String) {
        if (correo.isEmpty()) return
        viewModelScope.launch {
            val jefe = repository.obtenerJefeActivo(correo)
            _jefeActivo.value = jefe
        }
    }

    fun cargarHistorial(correo: String) {
        if (correo.isEmpty()) return
        viewModelScope.launch {
            val historial = repository.obtenerHistorialJefes(correo)
            _historialJefes.value = historial
        }
    }

    suspend fun obtenerUltimoJefeDerrotadoTime(correo: String): Long {
        if (correo.isEmpty()) return 0L
        val historial = repository.obtenerHistorialJefes(correo)
        return historial.maxByOrNull { it.fechaMuerte }?.fechaMuerte ?: 0L
    }

    fun atacarJefe(danioBase: Int, correo: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val derrotado = repository.atacarJefe(danioBase, correo)
            if (derrotado) {
                cargarHistorial(correo)
            }
            cargarJefeActivo(correo)
            onResult(derrotado)
        }
    }

    fun cargarTienda() {
        viewModelScope.launch {
            val items = repository.obtenerTiendaRPG()
            _tienda.value = items
        }
    }

    fun cargarInventario(correo: String) {
        if (correo.isEmpty()) return
        viewModelScope.launch {
            val items = repository.obtenerInventario(correo)
            _inventario.value = items
        }
    }

    fun comprarArticulo(correo: String, articulo: Articulo, onExito: (Boolean) -> Unit) {
        viewModelScope.launch {
            val exito = repository.comprarArticulo(correo, articulo)
            if (exito) {
                cargarInventario(correo)
            }
            onExito(exito)
        }
    }
}
