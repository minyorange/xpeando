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
                if (e != null) return@addSnapshotListener

                if (snapshot != null) {
                    val lista = snapshot.toObjects(Nota::class.java)
                    _notas.value = lista
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
                repository.insertarNota(nota)
            } catch (e: Exception) {
                // El listener se encargará de reflejar el estado actual
            }
        }
    }

    fun actualizarNota(nota: Nota) {
        viewModelScope.launch {
            try {
                repository.actualizarNota(nota)
            } catch (e: Exception) { }
        }
    }

    fun eliminarNota(id: Int, correo: String) {
        viewModelScope.launch {
            try {
                repository.eliminarNota(id, correo)
            } catch (e: Exception) { }
        }
    }
}
