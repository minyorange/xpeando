package com.example.xpeando.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xpeando.model.Articulo
import com.example.xpeando.model.Recompensa
import com.example.xpeando.repository.DataRepository
import com.example.xpeando.utils.LogroManager
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecompensasViewModel(private val repository: DataRepository) : ViewModel() {

    private val _state = MutableStateFlow(RecompensasState())
    val state: StateFlow<RecompensasState> = _state.asStateFlow()

    fun cargarDatos(correo: String) {
        viewModelScope.launch {
            val usuario = repository.obtenerUsuarioLogueado(correo)
            val recompensas = repository.obtenerTodasRecompensas(correo)
            val armeria = repository.obtenerTiendaRPG()
            _state.value = RecompensasState(usuario, recompensas, armeria)
        }
    }

    fun canjearRecompensaPersonal(context: Context, correo: String, recompensa: Recompensa, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val usuario = repository.obtenerUsuarioLogueado(correo)
            if (usuario != null && usuario.monedas >= recompensa.precio) {
                val monedasAntes = usuario.monedas
                repository.actualizarProgresoUsuario(correo, 0, -recompensa.precio)
                
                val usuarioDespues = repository.obtenerUsuarioLogueado(correo)
                if (usuarioDespues != null) {
                    LogroManager.verificarNuevosLogros(context, repository, usuarioDespues, monedasAntes, usuarioDespues.monedas, "MONEDAS")
                }

                cargarDatos(correo)
                onResult(true, "¡Has canjeado ${recompensa.nombre}!")
            } else {
                onResult(false, "No tienes suficientes monedas")
            }
        }
    }

    fun comprarArticuloArmeria(context: Context, correo: String, articulo: Articulo, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val usuario = repository.obtenerUsuarioLogueado(correo)
            if (usuario != null && usuario.monedas >= articulo.precio) {
                val monedasAntes = usuario.monedas
                val itemsAntes = repository.obtenerInventario(correo).size

                if (repository.comprarArticulo(correo, articulo)) {
                    repository.actualizarProgresoUsuario(correo, 0, -articulo.precio)
                    
                    val usuarioDespues = repository.obtenerUsuarioLogueado(correo)
                    val itemsDespues = repository.obtenerInventario(correo).size

                    if (usuarioDespues != null) {
                        LogroManager.verificarNuevosLogros(context, repository, usuarioDespues, monedasAntes, usuarioDespues.monedas, "MONEDAS")
                        // Añadiremos verificación de colección en LogroManager si no existe, 
                        // o lo manejamos aquí si es un tipo nuevo
                        LogroManager.verificarNuevosLogros(context, repository, usuarioDespues, itemsAntes, itemsDespues, "COLECCION")
                    }

                    cargarDatos(correo)
                    onResult(true, "¡Has comprado ${articulo.nombre}!")
                } else {
                    onResult(false, "Error al procesar la compra")
                }
            } else {
                onResult(false, "No tienes suficientes monedas")
            }
        }
    }

    fun insertarRecompensa(recompensa: Recompensa) {
        viewModelScope.launch {
            repository.insertarRecompensa(recompensa)
            cargarDatos(recompensa.correo_usuario)
        }
    }

    fun eliminarRecompensa(id: Int, correo: String) {
        viewModelScope.launch {
            repository.eliminarRecompensa(id)
            cargarDatos(correo)
        }
    }
}
