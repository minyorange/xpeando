package com.example.xpeando.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xpeando.repository.DataRepository
import com.example.xpeando.model.Logro
import com.example.xpeando.utils.LogroManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LogrosViewModel(private val repository: DataRepository) : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val _logros = MutableStateFlow<List<Logro>>(emptyList())
    val logros: StateFlow<List<Logro>> = _logros.asStateFlow()

    fun cargarLogros(correo: String) {
        if (correo.isEmpty()) return
        
        viewModelScope.launch {
            // 1. Obtener la definición de logros (lógica local)
            val usuario = withContext(Dispatchers.IO) { repository.obtenerUsuarioLogueado(correo) }
            if (usuario == null) return@launch

            // 2. Escuchar logros desbloqueados en Firestore
            db.collection("usuarios").document(correo)
                .collection("logros")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        val nombresLogrosNube = snapshot.documents.map { it.id }
                        
                        viewModelScope.launch {
                            val logrosDefinidos = withContext(Dispatchers.IO) { 
                                LogroManager.obtenerLogrosDefinidos(repository, usuario) 
                            }
                            
                            // Marcar como completados los que están en la nube
                            val listaFinal = logrosDefinidos.map { logro ->
                                if (nombresLogrosNube.contains(logro.nombre)) {
                                    logro.copy(completado = true)
                                } else {
                                    logro
                                }
                            }
                            _logros.value = listaFinal
                        }
                    }
                }
        }
    }
}
