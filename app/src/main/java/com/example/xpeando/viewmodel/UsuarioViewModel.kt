package com.example.xpeando.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xpeando.repository.DataRepository
import com.example.xpeando.model.Usuario
import com.example.xpeando.model.Articulo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UsuarioViewModel(private val repository: DataRepository) : ViewModel() {
    private val _usuario = MutableStateFlow<Usuario?>(null)
    val usuario: StateFlow<Usuario?> = _usuario.asStateFlow()

    fun cargarUsuario(correo: String) {
        viewModelScope.launch {
            val u = repository.obtenerUsuarioLogueado(correo)
            _usuario.value = u
        }
    }

    fun refrescarUsuario(correo: String) {
        cargarUsuario(correo)
    }

    fun validarUsuario(correo: String, contrasena: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val exito = repository.validarUsuario(correo, contrasena)
            callback(exito)
        }
    }

    fun registrarUsuario(usuario: Usuario, callback: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repository.registrarUsuario(usuario)
            callback(id)
        }
    }

    fun actualizarProgreso(correo: String, xp: Int, monedas: Int, hp: Int = 0) {
        viewModelScope.launch {
            repository.actualizarProgresoUsuario(correo, xp, monedas, hp)
            cargarUsuario(correo)
        }
    }

    fun actualizarAtributos(correo: String, fza: Double, int: Double, con: Double, per: Double, puntos: Int) {
        viewModelScope.launch {
            repository.actualizarAtributos(correo, fza, int, con, per, puntos)
            cargarUsuario(correo)
        }
    }

    fun comprarArticulo(correo: String, articulo: Articulo) {
        viewModelScope.launch {
            repository.comprarArticulo(correo, articulo)
            cargarUsuario(correo)
        }
    }

    fun obtenerInventario(correo: String, callback: (List<Articulo>) -> Unit) {
        viewModelScope.launch {
            val inv = repository.obtenerInventario(correo)
            callback(inv)
        }
    }

    fun eliminarDelInventario(id: Int, correo: String) {
        viewModelScope.launch {
            repository.eliminarDelInventario(id)
            cargarUsuario(correo)
        }
    }
}
