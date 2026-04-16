package com.example.xpeando.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xpeando.model.Nota
import com.example.xpeando.repository.DataRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class NotasViewModel(private val repository: DataRepository) : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val _notas = MutableStateFlow<List<Nota>>(emptyList())
    val notas: StateFlow<List<Nota>> = _notas.asStateFlow()
    
    private var notasListener: ListenerRegistration? = null

    fun cargarNotas(correo: String) {
        if (correo.isEmpty()) return
        
        // Cancelar listener previo si existe
        notasListener?.remove()

        // Escucha en tiempo real de la sub-colección de notas
        notasListener = db.collection("usuarios").document(correo)
            .collection("notas")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    // Fallback a local si hay error de red
                    viewModelScope.launch {
                        _notas.value = withContext(Dispatchers.IO) { repository.obtenerTodasNotas(correo) }
                    }
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val lista = snapshot.toObjects(Nota::class.java)
                    
                    // Si Firebase está vacío, intentamos migrar desde local una sola vez
                    if (lista.isEmpty()) {
                        viewModelScope.launch {
                            val locales = withContext(Dispatchers.IO) { repository.obtenerTodasNotas(correo) }
                            if (locales.isNotEmpty()) {
                                locales.forEach { nota ->
                                    db.collection("usuarios").document(correo)
                                        .collection("notas").document(nota.id.toString()).set(nota)
                                }
                            }
                        }
                    } else {
                        _notas.value = lista
                    }
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        notasListener?.remove()
    }

    fun insertarNota(nota: Nota) {
        viewModelScope.launch {
            try {
                // 1. Insertar en local para obtener un ID único numérico
                val idLocal = withContext(Dispatchers.IO) { repository.insertarNota(nota) }
                val notaConId = nota.copy(id = idLocal.toInt())
                
                // 2. Subir a Firestore usando el mismo ID
                db.collection("usuarios").document(nota.correo_usuario)
                    .collection("notas").document(idLocal.toString())
                    .set(notaConId).await()
                
                // No hace falta llamar a cargarNotas() porque el Listener lo hará por nosotros
            } catch (e: Exception) {
                // Error manejado por el listener
            }
        }
    }

    fun actualizarNota(nota: Nota) {
        viewModelScope.launch {
            try {
                db.collection("usuarios").document(nota.correo_usuario)
                    .collection("notas").document(nota.id.toString())
                    .set(nota).await()
                withContext(Dispatchers.IO) { repository.actualizarNota(nota) }
                cargarNotas(nota.correo_usuario)
            } catch (e: Exception) {
                cargarNotas(nota.correo_usuario)
            }
        }
    }

    fun eliminarNota(id: Int, correo: String) {
        viewModelScope.launch {
            try {
                db.collection("usuarios").document(correo)
                    .collection("notas").document(id.toString()).delete().await()
                withContext(Dispatchers.IO) { repository.eliminarNota(id) }
                cargarNotas(correo)
            } catch (e: Exception) {
                cargarNotas(correo)
            }
        }
    }
}
