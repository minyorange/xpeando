package com.example.xpeando.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xpeando.model.Tarea
import com.example.xpeando.model.Usuario
import com.example.xpeando.repository.DataRepository
import com.example.xpeando.utils.LogroManager
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TareasViewModel(private val repository: DataRepository) : ViewModel() {

    private val _tareas = MutableStateFlow<List<Tarea>>(emptyList())
    val tareas: StateFlow<List<Tarea>> = _tareas.asStateFlow()

    private val _usuario = MutableStateFlow<Usuario?>(null)
    val usuario: StateFlow<Usuario?> = _usuario.asStateFlow()

    fun cargarTareas(correo: String, mostrarCompletadas: Boolean) {
        if (correo.isEmpty()) return
        viewModelScope.launch {
            val todas = repository.obtenerTodasLasTareas(correo)
            val filtradas = if (mostrarCompletadas) {
                todas.filter { it.completada }.takeLast(10).reversed()
            } else {
                todas.filter { !it.completada }
            }
            _tareas.value = filtradas
            
            val u = repository.obtenerUsuarioLogueado(correo)
            _usuario.value = u
        }
    }

    fun completarTarea(context: Context, tarea: Tarea, correo: String, onNivelSubido: (Int) -> Unit) {
        viewModelScope.launch {
            val usuarioAntes = repository.obtenerUsuarioLogueado(correo)
            val nivelAntes = usuarioAntes?.nivel ?: 1

            val tareaCompletada = tarea.copy(completada = true)
            repository.actualizarTarea(tareaCompletada)
            repository.actualizarProgresoUsuario(correo, tarea.experiencia, tarea.monedas)
            
            val uFinal = repository.obtenerUsuarioLogueado(correo)
            val nivelDespues = uFinal?.nivel ?: 1
            if (nivelDespues > nivelAntes) {
                onNivelSubido(nivelDespues)
            }
            
            cargarTareas(correo, false)
        }
    }

    fun insertarTarea(tarea: Tarea) {
        viewModelScope.launch {
            repository.insertarTarea(tarea)
            cargarTareas(tarea.correo_usuario, false)
        }
    }

    fun eliminarTarea(id: Int, correo: String, mostrarCompletadas: Boolean) {
        viewModelScope.launch {
            repository.eliminarTarea(id, correo)
            cargarTareas(correo, mostrarCompletadas)
        }
    }
}
