package com.example.xpeando.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xpeando.repository.DataRepository
import com.example.xpeando.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class UsuarioViewModel(private val userRepository: DataRepository) : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    
    private val _usuario = MutableStateFlow<Usuario?>(null)
    val usuario: StateFlow<Usuario?> = _usuario.asStateFlow()

    private var usuarioListener: ListenerRegistration? = null

    fun cargarUsuario(correo: String) {
        if (correo.isEmpty()) return
        
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
        usuarioListener?.remove()
    }

    fun registrarUsuarioFirebase(usuario: Usuario, contrasena: String, callback: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(usuario.correo, contrasena).await()
                if (result.user != null) {
                    val usuarioInicial = usuario.copy(
                        totalTareasCompletadas = 0,
                        totalDailiesCompletadas = 0,
                        totalHabitosCompletados = 0,
                        tutorialVisto = false
                    )
                    userRepository.registrarUsuario(usuarioInicial)
                    callback(true, null)
                }
            } catch (e: Exception) {
                callback(false, e.message)
            }
        }
    }

    fun loginUsuarioFirebase(correo: String, contrasena: String, callback: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val result = auth.signInWithEmailAndPassword(correo, contrasena).await()
                if (result.user != null) {
                    callback(true, null)
                } else {
                    callback(false, "Error al iniciar sesión")
                }
            } catch (e: Exception) {
                callback(false, e.message)
            }
        }
    }

    fun actualizarProgreso(correo: String, xp: Int, monedas: Int, hp: Int = 0) {
        viewModelScope.launch {
            userRepository.actualizarProgresoUsuario(correo, xp, monedas, hp)
        }
    }

    fun subirAtributo(correo: String, tipo: String) {
        viewModelScope.launch {
            val user = _usuario.value ?: return@launch
            if (user.puntosDisponibles > 0) {
                when (tipo) {
                    "fza" -> userRepository.actualizarAtributos(correo, fza = 1.0, int = 0.0, con = 0.0, per = 0.0, puntos = 1)
                    "int" -> userRepository.actualizarAtributos(correo, fza = 0.0, int = 1.0, con = 0.0, per = 0.0, puntos = 1)
                    "con" -> userRepository.actualizarAtributos(correo, fza = 0.0, int = 0.0, con = 1.0, per = 0.0, puntos = 1)
                    "per" -> userRepository.actualizarAtributos(correo, fza = 0.0, int = 0.0, con = 0.0, per = 1.0, puntos = 1)
                }
            }
        }
    }

    fun marcarTutorialVisto(correo: String) {
        viewModelScope.launch {
            userRepository.marcarTutorialComoVisto(correo)
        }
    }

    fun cerrarSesion() {
        auth.signOut()
        _usuario.value = null
    }

    fun borrarCuenta(correo: String, onCompletado: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val userRef = db.collection("usuarios").document(correo)
                
                // 1. Borrar subcolecciones (Firestore no las borra solas)
                val subcolecciones = listOf(
                    "dailies", "habitos", "tareas", "rpg", "rpg_historial", 
                    "inventario", "logros", "notas", "recompensas", "historial_progreso"
                )
                for (sub in subcolecciones) {
                    val snapshot = userRef.collection(sub).get().await()
                    for (doc in snapshot.documents) {
                        doc.reference.delete().await()
                    }
                }

                // 2. Borrar documento del usuario
                userRef.delete().await()
                
                // 3. Borrar de Firebase Auth
                auth.currentUser?.delete()?.await()
                
                withContext(Dispatchers.Main) {
                    onCompletado(true)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onCompletado(false)
                }
            }
        }
    }
}