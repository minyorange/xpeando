package com.example.xpeando.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xpeando.model.Habito
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

class HabitosViewModel(private val repository: DataRepository) : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val _habitos = MutableStateFlow<List<Habito>>(emptyList())
    val habitos: StateFlow<List<Habito>> = _habitos.asStateFlow()

    private val _usuario = MutableStateFlow<Usuario?>(null)
    val usuario: StateFlow<Usuario?> = _usuario.asStateFlow()

    private var habitosListener: ListenerRegistration? = null
    private var usuarioListener: ListenerRegistration? = null

    fun cargarHabitos(correo: String) {
        if (correo.isEmpty()) return

        // Escuchar Habitos en tiempo real
        habitosListener?.remove()
        habitosListener = db.collection("usuarios").document(correo)
            .collection("habitos")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    val lista = snapshot.toObjects(Habito::class.java)
                    if (lista.isEmpty()) {
                        viewModelScope.launch {
                            val locales = withContext(Dispatchers.IO) { repository.obtenerTodosHabitos(correo) }
                            locales.forEach { h ->
                                db.collection("usuarios").document(correo)
                                    .collection("habitos").document(h.id.toString()).set(h)
                            }
                        }
                    } else {
                        _habitos.value = lista
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
        habitosListener?.remove()
        usuarioListener?.remove()
    }

    fun procesarAccion(context: Context, habito: Habito, delta: Int, correo: String, onNivelSubido: (Int) -> Unit) {
        viewModelScope.launch {
            val usuarioAntes = withContext(Dispatchers.IO) { repository.obtenerUsuarioLogueado(correo) }
            val nivelAntes = usuarioAntes?.nivel ?: 1
            val habitosAntes = withContext(Dispatchers.IO) { repository.obtenerTotalHabitosCompletados(correo) }
            val monedasAntes = usuarioAntes?.monedas ?: 0

            withContext(Dispatchers.IO) {
                if (delta > 0) {
                    repository.actualizarRacha(correo)
                    val habitoCopia = habito.copy(completadoHoy = true)
                    repository.actualizarEstadoHabito(habitoCopia)
                    
                    // Actualizar hábito en Firestore
                    db.collection("usuarios").document(correo)
                        .collection("habitos").document(habito.id.toString()).set(habitoCopia)
                }
                
                val xpCambio = habito.experiencia * delta
                val monedasCambio = if (delta > 0) habito.monedas else 0
                val hpCambio = if (delta < 0) -5 else 0
                
                repository.actualizarProgresoUsuario(correo, xpCambio, monedasCambio, hpCambio)

                if (habito.atributo == "Fuerza") {
                    val danioBase = if (delta > 0) 10 else -15
                    repository.atacarJefe(danioBase, correo)
                }

                if (delta > 0) {
                    when (habito.atributo) {
                        "Fuerza" -> repository.actualizarAtributos(correo, 0.01, 0.0, 0.0, 0.0, 0)
                        "Inteligencia" -> repository.actualizarAtributos(correo, 0.0, 0.01, 0.0, 0.0, 0)
                        "Constitución" -> repository.actualizarAtributos(correo, 0.0, 0.0, 0.01, 0.0, 0)
                        "Percepción" -> repository.actualizarAtributos(correo, 0.0, 0.0, 0.0, 0.01, 0)
                    }
                }
            }

            val usuarioDespues = withContext(Dispatchers.IO) { repository.obtenerUsuarioLogueado(correo) } ?: return@launch
            
            // --- SINCRONIZAR PERFIL A FIRESTORE ---
            try {
                db.collection("usuarios").document(correo).set(usuarioDespues).await()

                // Sincronizar Historial de Progreso
                val localHist = withContext(Dispatchers.IO) { repository.obtenerHistorialCompleto(correo) }
                val ultimaEntrada = localHist.lastOrNull()
                if (ultimaEntrada != null) {
                     db.collection("usuarios").document(correo)
                        .collection("historial_progreso").document(ultimaEntrada.id.toString()).set(ultimaEntrada)
                }
            } catch (e: Exception) {}

            val nivelDespues = usuarioDespues.nivel
            val habitosDespues = withContext(Dispatchers.IO) { repository.obtenerTotalHabitosCompletados(correo) }
            val monedasDespues = usuarioDespues.monedas

            if (nivelDespues > nivelAntes) {
                onNivelSubido(nivelDespues)
                LogroManager.verificarNuevosLogros(context, repository, usuarioDespues, nivelAntes, nivelDespues, "NIVEL")
            }
            
            LogroManager.verificarNuevosLogros(context, repository, usuarioDespues, habitosAntes, habitosDespues, "HABITO")
            LogroManager.verificarNuevosLogros(context, repository, usuarioDespues, monedasAntes, monedasDespues, "MONEDAS")

            cargarHabitos(correo)
        }
    }

    fun insertarHabito(habito: Habito) {
        viewModelScope.launch {
            val idLocal = withContext(Dispatchers.IO) { repository.insertarHabito(habito) }
            val habitoConId = habito.copy(id = idLocal.toInt())
            try {
                db.collection("usuarios").document(habito.correo_usuario)
                    .collection("habitos").document(idLocal.toString()).set(habitoConId).await()
            } catch (e: Exception) {}
            cargarHabitos(habito.correo_usuario)
        }
    }

    fun eliminarHabito(id: Int, correo: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.eliminarHabito(id) }
            try {
                db.collection("usuarios").document(correo)
                    .collection("habitos").document(id.toString()).delete().await()
            } catch (e: Exception) {}
            cargarHabitos(correo)
        }
    }
}
