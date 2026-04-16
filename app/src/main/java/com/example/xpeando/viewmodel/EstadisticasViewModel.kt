package com.example.xpeando.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xpeando.repository.DataRepository
import com.example.xpeando.model.Usuario
import com.example.xpeando.utils.LogroManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EstadisticasViewModel(private val repository: DataRepository) : ViewModel() {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val _state = MutableStateFlow(EstadisticasState())
    val state: StateFlow<EstadisticasState> = _state.asStateFlow()
    private var statsListener: ListenerRegistration? = null
    private var historialListener: ListenerRegistration? = null

    fun cargarEstadisticas(correo: String) {
        if (correo.isEmpty()) return

        statsListener?.remove()
        statsListener = db.collection("usuarios").document(correo)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener

                if (snapshot != null && snapshot.exists()) {
                    val usuario = snapshot.toObject(Usuario::class.java)
                    
                    viewModelScope.launch {
                        val logros = usuario?.let {
                            LogroManager.obtenerLogrosDefinidos(repository, it)
                        } ?: emptyList()

                        _state.value = _state.value.copy(
                            usuario = usuario,
                            totalTareas = usuario?.totalTareasCompletadas ?: 0,
                            totalDailies = usuario?.totalDailiesCompletadas ?: 0,
                            totalHabitos = usuario?.totalHabitosCompletados ?: 0,
                            logros = logros
                        )
                    }
                }
            }

        // Sincronizar y Escuchar Historial de Progreso
        historialListener?.remove()
        historialListener = db.collection("usuarios").document(correo)
            .collection("historial_progreso")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener

                if (snapshot != null) {
                    val historial = snapshot.toObjects(com.example.xpeando.model.Progreso::class.java)
                    if (historial.isEmpty()) {
                        // Migración: Subir historial local a Firestore
                        viewModelScope.launch {
                            val localHistorial: List<com.example.xpeando.model.Progreso> = withContext(Dispatchers.IO) { repository.obtenerHistorialCompleto(correo) }
                            localHistorial.forEach { progreso ->
                                db.collection("usuarios").document(correo)
                                    .collection("historial_progreso").document(progreso.id.toString()).set(progreso)
                            }
                        }
                    } else {
                        // Procesar XP semanal desde Firestore
                        val mapaXP = historial.groupBy { it.fecha }
                            .mapValues { entry -> entry.value.sumOf { it.xp } }
                            .toList()
                            .sortedByDescending { it.first }
                            .take(7)
                            .toMap()
                        
                        _state.value = _state.value.copy(xpSemanal = mapaXP)
                    }
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        statsListener?.remove()
        historialListener?.remove()
    }
}
