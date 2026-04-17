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

class UsuarioViewModel(private val repository: DataRepository) : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    
    private val _usuario = MutableStateFlow<Usuario?>(null)
    val usuario: StateFlow<Usuario?> = _usuario.asStateFlow()

    private val _inventario = MutableStateFlow<List<Articulo>>(emptyList())
    val inventario: StateFlow<List<Articulo>> = _inventario.asStateFlow()

    private var usuarioListener: ListenerRegistration? = null

    fun cargarUsuario(correo: String) {
        if (correo.isEmpty()) return
        
        usuarioListener?.remove()

        // ESCUCHA EN TIEMPO REAL DE FIRESTORE (Fuente única de verdad)
        usuarioListener = db.collection("usuarios").document(correo)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                
                if (snapshot != null && snapshot.exists()) {
                    _usuario.value = snapshot.toObject(Usuario::class.java)
                }
            }

        // También escuchar el inventario en tiempo real para que los botones se actualicen
        db.collection("usuarios").document(correo).collection("inventario")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    _inventario.value = snapshot.toObjects(Articulo::class.java)
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        usuarioListener?.remove()
    }

    fun registrarUsuarioFirebase(usuario: Usuario, callback: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(usuario.correo, usuario.contrasena).await()
                if (result.user != null) {
                    val usuarioInicial = usuario.copy(
                        totalTareasCompletadas = 0,
                        totalDailiesCompletadas = 0,
                        totalHabitosCompletados = 0
                    )
                    repository.registrarUsuario(usuarioInicial)
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
            repository.actualizarProgresoUsuario(correo, xp, monedas, hp)
            // No hace falta llamar a cargarUsuario() porque el addSnapshotListener lo hará solo
        }
    }

    fun subirAtributo(correo: String, tipo: String) {
        viewModelScope.launch {
            val user = _usuario.value ?: return@launch
            if (user.puntosDisponibles > 0) {
                when (tipo) {
                    "fza" -> repository.actualizarAtributos(correo, fza = 1.0, int = 0.0, con = 0.0, per = 0.0, puntos = 1)
                    "int" -> repository.actualizarAtributos(correo, fza = 0.0, int = 1.0, con = 0.0, per = 0.0, puntos = 1)
                    "con" -> repository.actualizarAtributos(correo, fza = 0.0, int = 0.0, con = 1.0, per = 0.0, puntos = 1)
                    "per" -> repository.actualizarAtributos(correo, fza = 0.0, int = 0.0, con = 0.0, per = 1.0, puntos = 1)
                }
            }
        }
    }

    fun comprarArticulo(correo: String, articulo: Articulo) {
        viewModelScope.launch {
            val exito = repository.comprarArticulo(correo, articulo)
            if (exito) {
                _inventario.value = repository.obtenerInventario(correo)
            }
        }
    }

    fun usarPocion(correo: String, id: Int, curacion: Int) {
        viewModelScope.launch {
            repository.actualizarProgresoUsuario(correo, 0, 0, curacion)
            repository.eliminarDelInventario(id, correo)
            _inventario.value = repository.obtenerInventario(correo)
        }
    }

    fun equiparDesequipar(correo: String, idArticulo: Int) {
        viewModelScope.launch {
            repository.equiparDesequipar(correo, idArticulo)
            _inventario.value = repository.obtenerInventario(correo)
        }
    }

    fun marcarTutorialVisto(correo: String) {
        viewModelScope.launch {
            repository.marcarTutorialComoVisto(correo)
        }
    }
    
    fun cerrarSesion() {
        auth.signOut()
        _usuario.value = null
    }
}
