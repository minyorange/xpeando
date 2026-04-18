package com.example.xpeando.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xpeando.repository.DataRepository
import com.example.xpeando.repository.RpgRepository
import com.example.xpeando.model.Logro
import com.example.xpeando.utils.LogroManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LogrosViewModel(
    private val userRepository: DataRepository,
    private val rpgRepository: RpgRepository
) : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val _logros = MutableStateFlow<List<Logro>>(emptyList())
    val logros: StateFlow<List<Logro>> = _logros.asStateFlow()

    fun cargarLogros(correo: String) {
        if (correo.isEmpty()) return
        
        viewModelScope.launch {
            val usuario = withContext(Dispatchers.IO) { userRepository.obtenerUsuarioLogueado(correo) }
            if (usuario == null) return@launch

            db.collection("usuarios").document(correo)
                .collection("logros")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        val nombresLogrosNube = snapshot.documents.map { it.id }
                        
                        viewModelScope.launch {
                            val logrosDefinidos = withContext(Dispatchers.IO) { 
                                LogroManager.obtenerLogrosDefinidos(rpgRepository, usuario)
                            }
                            
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
