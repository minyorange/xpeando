package com.example.xpeando.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xpeando.model.Tarea
import com.example.xpeando.model.Usuario
import com.example.xpeando.repository.DataRepository
import com.example.xpeando.utils.LogroManager
import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class TareasViewModel(private val repository: DataRepository) : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val _tareas = MutableStateFlow<List<Tarea>>(emptyList())
    val tareas: StateFlow<List<Tarea>> = _tareas.asStateFlow()

    private val _usuario = MutableStateFlow<Usuario?>(null)
    val usuario: StateFlow<Usuario?> = _usuario.asStateFlow()

    private var tareasListener: ListenerRegistration? = null
    private var usuarioListener: ListenerRegistration? = null

    fun cargarTareas(correo: String, mostrarCompletadas: Boolean) {
        if (correo.isEmpty()) return

        // Escuchar Tareas en tiempo real
        tareasListener?.remove()
        tareasListener = db.collection("usuarios").document(correo)
            .collection("tareas")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    val todas = snapshot.toObjects(Tarea::class.java)
                    if (todas.isEmpty()) {
                        viewModelScope.launch {
                            val locales = withContext(Dispatchers.IO) { repository.obtenerTodasLasTareas(correo) }
                            locales.forEach { t ->
                                db.collection("usuarios").document(correo)
                                    .collection("tareas").document(t.id.toString()).set(t)
                            }
                        }
                    } else {
                        val filtradas = if (mostrarCompletadas) {
                            todas.filter { it.completada }.takeLast(10).reversed()
                        } else {
                            todas.filter { !it.completada }
                        }
                        _tareas.value = filtradas
                    }
                }
            }

        // Escuchar Usuario en tiempo real
        usuarioListener?.remove()
        usuarioListener = db.collection("usuarios").document(correo)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
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
            try {
                val usuarioAntes = withContext(Dispatchers.IO) { repository.obtenerUsuarioLogueado(correo) }
                val nivelAntes = usuarioAntes?.nivel ?: 1

                // 1. Marcar como completada en Firestore y Local
                val tareaCompletada = tarea.copy(completada = true)
                db.collection("usuarios").document(correo)
                    .collection("tareas").document(tarea.id.toString())
                    .set(tareaCompletada).await()
                
                withContext(Dispatchers.IO) {
                    repository.actualizarTarea(tareaCompletada)
                    repository.actualizarProgresoUsuario(correo, tarea.experiencia, tarea.monedas)
                    repository.atacarJefe(tarea.dificultad * 15, correo)
                    
                    // SUBIR USUARIO ACTUALIZADO (Esto activará el Listener en UsuarioViewModel)
                    val uActualizado = repository.obtenerUsuarioLogueado(correo)
                    if (uActualizado != null) {
                        db.collection("usuarios").document(correo).set(uActualizado).await()
                    }

                    // Sincronizar Historial de Progreso
                    val localHist = repository.obtenerHistorialCompleto(correo)
                    val ultimaEntrada = localHist.lastOrNull()
                    if (ultimaEntrada != null) {
                         db.collection("usuarios").document(correo)
                            .collection("historial_progreso").document(ultimaEntrada.id.toString()).set(ultimaEntrada)
                    }
                }

                val uFinal = withContext(Dispatchers.IO) { repository.obtenerUsuarioLogueado(correo) }
                val nivelDespues = uFinal?.nivel ?: 1
                if (nivelDespues > nivelAntes) {
                    onNivelSubido(nivelDespues)
                }
                
                cargarTareas(correo, false)
            } catch (e: Exception) {
                cargarTareas(correo, false)
            }
        }
    }

    fun insertarTarea(tarea: Tarea) {
        viewModelScope.launch {
            try {
                // Insertar en local para obtener un ID numérico (por ahora)
                val idLocal = withContext(Dispatchers.IO) { repository.insertarTarea(tarea) }
                val tareaConId = tarea.copy(id = idLocal.toInt())
                
                // Insertar en Firestore usando ese ID
                db.collection("usuarios").document(tarea.correo_usuario)
                    .collection("tareas").document(idLocal.toString())
                    .set(tareaConId).await()
                
                cargarTareas(tarea.correo_usuario, false)
            } catch (e: Exception) {
                // Fallback local
                cargarTareas(tarea.correo_usuario, false)
            }
        }
    }

    fun eliminarTarea(id: Int, correo: String, mostrarCompletadas: Boolean) {
        viewModelScope.launch {
            try {
                db.collection("usuarios").document(correo)
                    .collection("tareas").document(id.toString()).delete().await()
                withContext(Dispatchers.IO) { repository.eliminarTarea(id) }
                cargarTareas(correo, mostrarCompletadas)
            } catch (e: Exception) {
                cargarTareas(correo, mostrarCompletadas)
            }
        }
    }
}
