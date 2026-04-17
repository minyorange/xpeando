package com.example.xpeando.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xpeando.model.Habito
import com.example.xpeando.model.Usuario
import com.example.xpeando.repository.DataRepository
import com.example.xpeando.utils.LogroManager
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HabitosViewModel(private val repository: DataRepository) : ViewModel() {

    private val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    private val _habitos = MutableStateFlow<List<Habito>>(emptyList())
    val habitos: StateFlow<List<Habito>> = _habitos.asStateFlow()

    private val _usuario = MutableStateFlow<Usuario?>(null)
    val usuario: StateFlow<Usuario?> = _usuario.asStateFlow()

    private var habitosListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var usuarioListener: com.google.firebase.firestore.ListenerRegistration? = null

    fun cargarHabitos(correo: String) {
        if (correo.isEmpty()) return
        
        // Listener para Hábitos
        habitosListener?.remove()
        habitosListener = db.collection("usuarios").document(correo).collection("habitos")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    _habitos.value = snapshot.toObjects(Habito::class.java)
                }
            }

        // Listener para Usuario
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
        habitosListener?.remove()
        usuarioListener?.remove()
    }

    fun procesarAccion(context: Context, habito: Habito, delta: Int, correo: String, onNivelSubido: (Int) -> Unit) {
        viewModelScope.launch {
            // Ya no necesitamos pedir el usuario manualmente antes de empezar
            val uActual = _usuario.value
            val nivelAntes = uActual?.nivel ?: 1
            
            val xpCambio = habito.experiencia * delta
            val monedasCambio = if (delta > 0) habito.monedas else 0
            val hpCambio = if (delta < 0) -5 else 0
            
            // Lanzamos las actualizaciones a la nube
            // No usamos "await" aquí para que el Toast salga instantáneo (Optimismo UI)
            launch(Dispatchers.IO) {
                if (delta > 0) {
                    repository.actualizarEstadoHabito(habito, delta)
                    repository.actualizarProgresoUsuario(
                        correo, 
                        xpCambio, 
                        monedasCambio, 
                        hpCambio, 
                        tipoAccion = "HABITO", 
                        atributoAIncrementar = habito.atributo
                    )
                    
                    // --- VERIFICAR LOGROS ---
                    uActual?.let { usuario ->
                        LogroManager.verificarNuevosLogros(
                            context,
                            repository,
                            usuario,
                            "HABITO"
                        )
                    }
                } else {
                    repository.actualizarProgresoUsuario(correo, xpCambio, monedasCambio, hpCambio, tipoAccion = null)
                }
            }

            // El Toast sale inmediatamente, sin esperar a internet
            if (delta > 0) {
                com.example.xpeando.utils.XpeandoToast.mostrarProgreso(context, xpCambio, monedasCambio)
            } else {
                com.example.xpeando.utils.XpeandoToast.mostrarPenalizacion(context, hpCambio, xpCambio)
            }

            // La verificación de nivel se hará cuando el Listener de usuario detecte el cambio
        }
    }

    fun insertarHabito(habito: Habito) {
        viewModelScope.launch {
            repository.insertarHabito(habito)
            cargarHabitos(habito.correo_usuario)
        }
    }

    fun eliminarHabito(id: Int, correo: String) {
        viewModelScope.launch {
            repository.eliminarHabito(id, correo)
            cargarHabitos(correo)
        }
    }
}
