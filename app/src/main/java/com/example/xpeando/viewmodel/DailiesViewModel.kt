package com.example.xpeando.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xpeando.model.Daily
import com.example.xpeando.model.Usuario
import com.example.xpeando.repository.DataRepository
import com.example.xpeando.utils.LogroManager
import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class DailiesViewModel(private val repository: DataRepository) : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val _dailies = MutableStateFlow<List<Daily>>(emptyList())
    val dailies: StateFlow<List<Daily>> = _dailies.asStateFlow()

    private val _usuario = MutableStateFlow<Usuario?>(null)
    val usuario: StateFlow<Usuario?> = _usuario.asStateFlow()

    private var dailiesListener: ListenerRegistration? = null
    private var usuarioListener: ListenerRegistration? = null

    fun cargarDailies(correo: String) {
        if (correo.isEmpty()) return

        // Escuchar Dailies en tiempo real
        dailiesListener?.remove()
        dailiesListener = db.collection("usuarios").document(correo)
            .collection("dailies")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    val todas = snapshot.toObjects(Daily::class.java)
                    if (todas.isEmpty()) {
                        viewModelScope.launch {
                            val locales = withContext(Dispatchers.IO) { repository.obtenerTodasDailies(correo) }
                            locales.forEach { d ->
                                db.collection("usuarios").document(correo)
                                    .collection("dailies").document(d.id.toString()).set(d)
                            }
                        }
                    } else {
                        _dailies.value = todas.filter { !it.completadaHoy }
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
        dailiesListener?.remove()
        usuarioListener?.remove()
    }

    fun completarDaily(context: Context, daily: Daily, correo: String, onNivelSubido: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                db.collection("usuarios").document(correo)
                    .collection("dailies").document(daily.id.toString())
                    .update("completadaHoy", true).await()

                withContext(Dispatchers.IO) {
                    repository.actualizarEstadoDaily(daily, true)
                    repository.actualizarRacha(correo)
                    repository.actualizarProgresoUsuario(correo, daily.experiencia, daily.monedas)
                    repository.atacarJefe(25, correo)
                    
                    val uActualizado = repository.obtenerUsuarioLogueado(correo)
                    if (uActualizado != null) {
                        db.collection("usuarios").document(correo).set(uActualizado)
                    }

                    // Sincronizar Historial de Progreso
                    val localHist = repository.obtenerHistorialCompleto(correo)
                    val ultimaEntrada = localHist.lastOrNull()
                    if (ultimaEntrada != null) {
                         db.collection("usuarios").document(correo)
                            .collection("historial_progreso").document(ultimaEntrada.id.toString()).set(ultimaEntrada)
                    }
                }
                // ... (Lógica de logros)
                cargarDailies(correo)
            } catch (e: Exception) {
                // Error
            }
        }
    }

    fun insertarDaily(daily: Daily) {
        viewModelScope.launch {
            try {
                val idLocal = withContext(Dispatchers.IO) { repository.insertarDaily(daily) }
                val dailyConId = daily.copy(id = idLocal.toInt())
                db.collection("usuarios").document(daily.correo_usuario)
                    .collection("dailies").document(idLocal.toString())
                    .set(dailyConId).await()
                cargarDailies(daily.correo_usuario)
            } catch (e: Exception) {
                cargarDailies(daily.correo_usuario)
            }
        }
    }

    fun eliminarDaily(id: Int, correo: String) {
        viewModelScope.launch {
            try {
                db.collection("usuarios").document(correo)
                    .collection("dailies").document(id.toString()).delete().await()
                withContext(Dispatchers.IO) { repository.eliminarDaily(id) }
                cargarDailies(correo)
            } catch (e: Exception) {
                cargarDailies(correo)
            }
        }
    }

    fun procesarDailiesFallidas(correo: String, dias: Int): Int {
        // Esta operación es sincrónica en el DAO original, pero la llamaremos asíncronamente
        // En el ViewModel, solo devolvemos el daño para mostrar el feedback
        var danio = 0
        viewModelScope.launch {
            danio = withContext(Dispatchers.IO) { repository.procesarDailiesFallidas(correo, dias) }
            cargarDailies(correo)
        }
        return danio
    }
}
