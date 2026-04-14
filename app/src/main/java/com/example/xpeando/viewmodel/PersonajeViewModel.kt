package com.example.xpeando.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xpeando.repository.DataRepository
import com.example.xpeando.model.Articulo
import com.example.xpeando.model.Usuario
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PersonajeViewModel(private val repository: DataRepository) : ViewModel() {
    private val _usuario = MutableStateFlow<Usuario?>(null)
    val usuario: StateFlow<Usuario?> = _usuario.asStateFlow()

    private val _inventario = MutableStateFlow<List<Articulo>>(emptyList())
    val inventario: StateFlow<List<Articulo>> = _inventario.asStateFlow()

    fun cargarDatos(correo: String) {
        viewModelScope.launch {
            val u = withContext(Dispatchers.IO) { repository.obtenerUsuarioLogueado(correo) }
            val inv = withContext(Dispatchers.IO) { repository.obtenerInventario(correo) }
            _usuario.value = u
            _inventario.value = inv
        }
    }

    fun subirAtributo(correo: String, tipo: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                when (tipo) {
                    "fza" -> repository.actualizarAtributos(correo, fza = 1.0, int = 0.0, con = 0.0, per = 0.0, puntos = 1)
                    "int" -> repository.actualizarAtributos(correo, fza = 0.0, int = 1.0, con = 0.0, per = 0.0, puntos = 1)
                    "con" -> repository.actualizarAtributos(correo, fza = 0.0, int = 0.0, con = 1.0, per = 0.0, puntos = 1)
                    "per" -> repository.actualizarAtributos(correo, fza = 0.0, int = 0.0, con = 0.0, per = 1.0, puntos = 1)
                }
            }
            cargarDatos(correo)
        }
    }

    fun equiparDesequipar(correo: String, idArticulo: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.equiparDesequipar(correo, idArticulo) }
            cargarDatos(correo)
        }
    }

    fun usarPocion(correo: String, id: Int, curacion: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.actualizarProgresoUsuario(correo, 0, 0, curacion)
                repository.eliminarDelInventario(id)
            }
            cargarDatos(correo)
        }
    }
}
