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

    private val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    private val _tareas = MutableStateFlow<List<Tarea>>(emptyList())
    val tareas: StateFlow<List<Tarea>> = _tareas.asStateFlow()

    private val _usuario = MutableStateFlow<Usuario?>(null)
    val usuario: StateFlow<Usuario?> = _usuario.asStateFlow()

    private var tareasListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var usuarioListener: com.google.firebase.firestore.ListenerRegistration? = null

    fun cargarTareas(correo: String, mostrarCompletadas: Boolean) {
        if (correo.isEmpty()) return

        // Listener en tiempo real para Tareas
        tareasListener?.remove()
        tareasListener = db.collection("usuarios").document(correo).collection("tareas")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val todas = snapshot.toObjects(Tarea::class.java)
                    val filtradas = if (mostrarCompletadas) {
                        todas.filter { it.completada }.sortedByDescending { it.id }.take(10)
                    } else {
                        todas.filter { !it.completada }
                    }
                    _tareas.value = filtradas
                }
            }

        // Listener en tiempo real para Usuario
        usuarioListener?.remove()
        usuarioListener = db.collection("usuarios").document(correo)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    _usuario.value = snapshot.toObject(Usuario::class.java)
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        tareasListener?.remove()
        usuarioListener?.remove()
    }

    fun completarTarea(context: Context, tarea: Tarea, correo: String, onNivelSubido: (Int) -> Unit) {
        viewModelScope.launch {
            // FEEDBACK INSTANTÁNEO
            com.example.xpeando.utils.XpeandoToast.mostrarProgreso(context, tarea.experiencia, tarea.monedas)

            // OPERACIONES DE FONDO (Sin bloquear la UI)
            launch(kotlinx.coroutines.Dispatchers.IO) {
                val tareaCompletada = tarea.copy(completada = true)
                repository.actualizarTarea(tareaCompletada)
                repository.actualizarProgresoUsuario(correo, tarea.experiencia, tarea.monedas, tipoAccion = "TAREA")
                
                // Autolimpieza asíncrona (solo las 10 últimas completadas)
                val todas = repository.obtenerTodasLasTareas(correo)
                val completadas = todas.filter { it.completada }.sortedBy { it.id }
                if (completadas.size > 10) {
                    completadas.take(completadas.size - 10).forEach { 
                        repository.eliminarTarea(it.id, correo)
                    }
                }
            }
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
