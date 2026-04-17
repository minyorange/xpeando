package com.example.xpeando.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xpeando.model.Daily
import com.example.xpeando.model.Usuario
import com.example.xpeando.repository.DataRepository
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DailiesViewModel(private val repository: DataRepository) : ViewModel() {

    private val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    private val _dailies = MutableStateFlow<List<Daily>>(emptyList())
    val dailies: StateFlow<List<Daily>> = _dailies.asStateFlow()

    private val _usuario = MutableStateFlow<Usuario?>(null)
    val usuario: StateFlow<Usuario?> = _usuario.asStateFlow()

    private var dailiesListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var usuarioListener: com.google.firebase.firestore.ListenerRegistration? = null

    fun cargarDailies(correo: String) {
        if (correo.isEmpty()) return
        
        // Listener en tiempo real para Dailies
        dailiesListener?.remove()
        dailiesListener = db.collection("usuarios").document(correo).collection("dailies")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val hoy = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                    val todas = snapshot.toObjects(Daily::class.java).map { 
                        it.copy(completadaHoy = it.ultimaVezCompletada == hoy)
                    }
                    _dailies.value = todas.filter { !it.completadaHoy }
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
        dailiesListener?.remove()
        usuarioListener?.remove()
    }

    fun completarDaily(context: Context, daily: Daily, correo: String, onNivelSubido: (Int) -> Unit) {
        viewModelScope.launch {
            val uActual = _usuario.value
            // Optimismo de UI: Lanzamos todo a la vez sin esperar
            launch(kotlinx.coroutines.Dispatchers.IO) {
                repository.actualizarEstadoDaily(daily, true)
                repository.actualizarProgresoUsuario(correo, daily.experiencia, daily.monedas, tipoAccion = "DAILY")
                repository.actualizarRacha(correo)

                // --- VERIFICAR LOGROS ---
                uActual?.let { usuario ->
                    com.example.xpeando.utils.LogroManager.verificarNuevosLogros(
                        context,
                        repository,
                        usuario,
                        "DAILY"
                    )
                }
            }
            
            // Mostramos feedback visual inmediatamente (SOLO UNO COMBINADO)
            com.example.xpeando.utils.XpeandoToast.mostrarProgreso(context, daily.experiencia, daily.monedas, esDaily = true)
        }
    }

    fun procesarDailiesFallidas(correo: String, dias: Int, onResultado: (Int) -> Unit) {
        viewModelScope.launch {
            val danio = repository.procesarDailiesFallidas(correo, dias)
            if (danio > 0) {
                onResultado(danio)
            }
            cargarDailies(correo)
        }
    }

    fun insertarDaily(daily: Daily) {
        viewModelScope.launch {
            repository.insertarDaily(daily)
            cargarDailies(daily.correo_usuario)
        }
    }

    fun eliminarDaily(id: Int, correo: String) {
        viewModelScope.launch {
            repository.eliminarDaily(id, correo)
            cargarDailies(correo)
        }
    }
}
