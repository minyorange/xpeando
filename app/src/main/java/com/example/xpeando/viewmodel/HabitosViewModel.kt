package com.example.xpeando.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xpeando.model.Habito
import com.example.xpeando.model.Usuario
import com.example.xpeando.repository.DataRepository
import com.example.xpeando.utils.LogroManager
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HabitosViewModel(private val repository: DataRepository) : ViewModel() {

    private val _habitos = MutableStateFlow<List<Habito>>(emptyList())
    val habitos: StateFlow<List<Habito>> = _habitos.asStateFlow()

    private val _usuario = MutableStateFlow<Usuario?>(null)
    val usuario: StateFlow<Usuario?> = _usuario.asStateFlow()

    fun cargarHabitos(correo: String) {
        if (correo.isEmpty()) return
        viewModelScope.launch {
            val lista = repository.obtenerTodosHabitos(correo)
            _habitos.value = lista
            
            val u = repository.obtenerUsuarioLogueado(correo)
            _usuario.value = u
        }
    }

    fun procesarAccion(context: Context, habito: Habito, delta: Int, correo: String, onNivelSubido: (Int) -> Unit) {
        viewModelScope.launch {
            val usuarioAntes = repository.obtenerUsuarioLogueado(correo)
            val nivelAntes = usuarioAntes?.nivel ?: 1
            val habitosAntes = repository.obtenerTotalHabitosCompletados(correo)
            val monedasAntes = usuarioAntes?.monedas ?: 0

            // Toda la lógica de negocio ahora está centralizada en el DataRepository (Cloud)
            if (delta > 0) {
                repository.actualizarEstadoHabito(habito.copy(completadoHoy = true))
            }
            
            val xpCambio = habito.experiencia * delta
            val monedasCambio = if (delta > 0) habito.monedas else 0
            val hpCambio = if (delta < 0) -5 else 0
            
            repository.actualizarProgresoUsuario(correo, xpCambio, monedasCambio, hpCambio)

            val usuarioDespues = repository.obtenerUsuarioLogueado(correo) ?: return@launch
            val nivelDespues = usuarioDespues.nivel
            val habitosDespues = repository.obtenerTotalHabitosCompletados(correo)
            val monedasDespues = usuarioDespues.monedas

            if (nivelDespues > nivelAntes) {
                onNivelSubido(nivelDespues)
                LogroManager.verificarNuevosLogros(context, repository, usuarioDespues, nivelAntes, nivelDespues, "NIVEL")
            }
            
            LogroManager.verificarNuevosLogros(context, repository, usuarioDespues, habitosAntes, habitosDespues, "HABITO")
            LogroManager.verificarNuevosLogros(context, repository, usuarioDespues, monedasAntes, monedasDespues, "MONEDAS")

            cargarHabitos(correo)
        }
    }

    fun insertarHabito(habito: Habito) {
        viewModelScope.launch {
            repository.insertarHabito(habito)
            cargarHabitos(habito.correo_usuario)
        }
    }

    fun eliminarHabito(id: Int, correo: String) {
        viewModelScope.launch {
            repository.eliminarHabito(id, correo)
            cargarHabitos(correo)
        }
    }
}
