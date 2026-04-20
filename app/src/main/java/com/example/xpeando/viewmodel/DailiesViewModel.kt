package com.example.xpeando.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xpeando.model.Daily
import com.example.xpeando.model.Usuario
import com.example.xpeando.repository.DataRepository
import com.example.xpeando.repository.TaskRepository
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DailiesViewModel(
    private val taskRepository: TaskRepository,
    private val userRepository: DataRepository,
    private val rpgRepository: com.example.xpeando.repository.RpgRepository
) : ViewModel() {

    private val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    private val _dailies = MutableStateFlow<List<Daily>>(emptyList())
    val dailies: StateFlow<List<Daily>> = _dailies.asStateFlow()

    private val _usuario = MutableStateFlow<Usuario?>(null)
    val usuario: StateFlow<Usuario?> = _usuario.asStateFlow()

    private var dailiesListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var usuarioListener: com.google.firebase.firestore.ListenerRegistration? = null

    fun cargarDailies(correo: String) {
        if (correo.isEmpty()) return
        
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
            launch(kotlinx.coroutines.Dispatchers.IO) {
                // Cambiamos actualizarEstadoDaily por eliminarDaily para que no reaparezca nunca
                taskRepository.eliminarDaily(daily.id, correo)
                userRepository.actualizarProgresoUsuario(correo, daily.experiencia, daily.monedas, tipoAccion = "DAILY")
                userRepository.actualizarRacha(correo)
                
                // DAÑO AL JEFE: Cuando se completa una daily
                rpgRepository.atacarJefe(daily.experiencia, correo)

                uActual?.let { usuario ->
                    com.example.xpeando.utils.LogroManager.verificarNuevosLogros(
                        context,
                        userRepository,
                        rpgRepository,
                        usuario,
                        "DAILY"
                    )
                }
            }
            
            com.example.xpeando.utils.XpeandoToast.mostrarProgreso(context, daily.experiencia, daily.monedas, esDaily = true)
        }
    }

    fun procesarDailiesFallidas(context: android.content.Context, correo: String, dias: Int) {
        viewModelScope.launch {
            val (danio, murio) = userRepository.procesarDailiesFallidas(correo, dias)
            if (murio) {
                com.example.xpeando.utils.NotificationHelper.enviarNotificacionMuerte(context)
            }
            if (danio > 0) {
                com.example.xpeando.utils.XpeandoToast.mostrarPenalizacion(context, -danio, 0)
            }
            cargarDailies(correo)
        }
    }

    fun insertarDaily(daily: Daily) {
        viewModelScope.launch {
            taskRepository.insertarDaily(daily)
            cargarDailies(daily.correo_usuario)
        }
    }

    fun eliminarDaily(id: Int, correo: String) {
        viewModelScope.launch {
            taskRepository.eliminarDaily(id, correo)
            cargarDailies(correo)
        }
    }
}
