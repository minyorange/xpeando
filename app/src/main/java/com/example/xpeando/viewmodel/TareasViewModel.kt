package com.example.xpeando.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xpeando.model.Tarea
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

class TareasViewModel(private val repository: DataRepository) : ViewModel() {

    private val _tareas = MutableStateFlow<List<Tarea>>(emptyList())
    val tareas: StateFlow<List<Tarea>> = _tareas.asStateFlow()

    private val _usuario = MutableStateFlow<Usuario?>(null)
    val usuario: StateFlow<Usuario?> = _usuario.asStateFlow()

    fun cargarTareas(correo: String, mostrarCompletadas: Boolean) {
        viewModelScope.launch {
            val todas = withContext(Dispatchers.IO) { repository.obtenerTodasLasTareas(correo) }
            val filtradas = if (mostrarCompletadas) {
                todas.filter { it.completada }.takeLast(10).reversed()
            } else {
                todas.filter { !it.completada }
            }
            _tareas.value = filtradas
            val u = withContext(Dispatchers.IO) { repository.obtenerUsuarioLogueado(correo) }
            _usuario.value = u
        }
    }

    fun completarTarea(context: Context, tarea: Tarea, correo: String, onNivelSubido: (Int) -> Unit) {
        viewModelScope.launch {
            val usuarioAntes = withContext(Dispatchers.IO) { repository.obtenerUsuarioLogueado(correo) }
            val nivelAntes = usuarioAntes?.nivel ?: 1
            val tareasAntes = withContext(Dispatchers.IO) { repository.obtenerTotalTareasCompletadas(correo) }
            val monedasAntes = usuarioAntes?.monedas ?: 0

            withContext(Dispatchers.IO) {
                repository.actualizarTarea(tarea.copy(completada = true))
                repository.actualizarProgresoUsuario(correo, tarea.experiencia, tarea.monedas)
                repository.atacarJefe(tarea.dificultad * 15, correo)
            }

            val usuarioDespues = withContext(Dispatchers.IO) { repository.obtenerUsuarioLogueado(correo) } ?: return@launch
            val nivelDespues = usuarioDespues.nivel
            val tareasDespues = withContext(Dispatchers.IO) { repository.obtenerTotalTareasCompletadas(correo) }
            val monedasDespues = usuarioDespues.monedas

            if (nivelDespues > nivelAntes) {
                onNivelSubido(nivelDespues)
                LogroManager.verificarNuevosLogros(context, repository, usuarioDespues, nivelAntes, nivelDespues, "NIVEL")
            }
            
            LogroManager.verificarNuevosLogros(context, repository, usuarioDespues, tareasAntes, tareasDespues, "TAREA")
            LogroManager.verificarNuevosLogros(context, repository, usuarioDespues, monedasAntes, monedasDespues, "MONEDAS")

            cargarTareas(correo, false)
        }
    }

    fun insertarTarea(tarea: Tarea) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.insertarTarea(tarea) }
            cargarTareas(tarea.correo_usuario, false)
        }
    }

    fun eliminarTarea(id: Int, correo: String, mostrarCompletadas: Boolean) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.eliminarTarea(id) }
            cargarTareas(correo, mostrarCompletadas)
        }
    }
}
