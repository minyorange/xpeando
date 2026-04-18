package com.example.xpeando.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xpeando.repository.DataRepository
import com.example.xpeando.repository.RpgRepository
import com.example.xpeando.model.Articulo
import com.example.xpeando.model.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PersonajeViewModel(
    private val userRepository: DataRepository,
    private val rpgRepository: RpgRepository
) : ViewModel() {
    private val _usuario = MutableStateFlow<Usuario?>(null)
    val usuario: StateFlow<Usuario?> = _usuario.asStateFlow()

    private val _inventario = MutableStateFlow<List<Articulo>>(emptyList())
    val inventario: StateFlow<List<Articulo>> = _inventario.asStateFlow()

    fun cargarDatos(correo: String) {
        if (correo.isEmpty()) return
        viewModelScope.launch {
            val user = userRepository.obtenerUsuarioLogueado(correo)
            _usuario.value = user
            
            val items = rpgRepository.obtenerInventario(correo)
            _inventario.value = items
        }
    }

    fun subirAtributo(correo: String, tipo: String) {
        viewModelScope.launch {
            when (tipo) {
                "fza" -> userRepository.actualizarAtributos(correo, fza = 1.0, int = 0.0, con = 0.0, per = 0.0, puntos = 1)
                "int" -> userRepository.actualizarAtributos(correo, fza = 0.0, int = 1.0, con = 0.0, per = 0.0, puntos = 1)
                "con" -> userRepository.actualizarAtributos(correo, fza = 0.0, int = 0.0, con = 1.0, per = 0.0, puntos = 1)
                "per" -> userRepository.actualizarAtributos(correo, fza = 0.0, int = 0.0, con = 0.0, per = 1.0, puntos = 1)
            }
            cargarDatos(correo)
        }
    }

    fun equiparDesequipar(correo: String, idArticulo: Int) {
        viewModelScope.launch {
            rpgRepository.equiparDesequipar(correo, idArticulo)
            cargarDatos(correo)
        }
    }

    fun usarPocion(correo: String, id: Int, curacion: Int) {
        viewModelScope.launch {
            userRepository.actualizarProgresoUsuario(correo, 0, 0, curacion)
            rpgRepository.eliminarDelInventario(id, correo)
            cargarDatos(correo)
        }
    }
}
