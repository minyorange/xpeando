package com.example.xpeando.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xpeando.repository.DataRepository
import com.example.xpeando.model.Usuario
import com.example.xpeando.model.Articulo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class UsuarioViewModel(private val repository: DataRepository) : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val _usuario = MutableStateFlow<Usuario?>(null)
    val usuario: StateFlow<Usuario?> = _usuario.asStateFlow()

    private val _inventario = MutableStateFlow<List<Articulo>>(emptyList())
    val inventario: StateFlow<List<Articulo>> = _inventario.asStateFlow()

    private var correoActual: String? = null
    private var usuarioListener: ListenerRegistration? = null

    fun cargarUsuario(correo: String) {
        if (correo.isEmpty()) return
        correoActual = correo
        
        // Cancelar listener anterior si existe
        usuarioListener?.remove()

        // ESCUCHA EN TIEMPO REAL DE FIRESTORE
        usuarioListener = db.collection("usuarios").document(correo)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                
                if (snapshot != null && snapshot.exists()) {
                    val u = snapshot.toObject(Usuario::class.java)
                    if (u != null) {
                        _usuario.value = u
                        // SINCRONIZAR A LOCAL PARA MANTENER COHERENCIA
                        viewModelScope.launch(Dispatchers.IO) {
                            repository.upsertUsuario(u)
                        }
                    }
                } else {
                    // Si no está en Firestore, cargar local una vez (para migrar)
                    viewModelScope.launch {
                        val uLocal = withContext(Dispatchers.IO) { repository.obtenerUsuarioLogueado(correo) }
                        if (uLocal != null) {
                            _usuario.value = uLocal
                            sincronizarPerfilConFirestore(correo)
                        }
                    }
                }
            }

        viewModelScope.launch {
            val inv = withContext(Dispatchers.IO) { repository.obtenerInventario(correo) }
            _inventario.value = inv
        }
    }

    override fun onCleared() {
        super.onCleared()
        usuarioListener?.remove()
    }

    fun sincronizarPerfilConFirestore(correo: String) {
        viewModelScope.launch {
            try {
                val uLocal = withContext(Dispatchers.IO) { repository.obtenerUsuarioLogueado(correo) }
                if (uLocal != null) {
                    // Recalcular totales reales de los DAOs para que la nube esté al día
                    val totalT = withContext(Dispatchers.IO) { repository.obtenerTotalTareasCompletadas(correo) }
                    val totalD = withContext(Dispatchers.IO) { repository.obtenerTotalDailiesCompletadas(correo) }
                    val totalH = withContext(Dispatchers.IO) { repository.obtenerTotalHabitosCompletados(correo) }
                    
                    val uParaSubir = uLocal.copy(
                        totalTareasCompletadas = totalT,
                        totalDailiesCompletadas = totalD,
                        totalHabitosCompletados = totalH
                    )
                    
                    db.collection("usuarios").document(correo).set(uParaSubir).await()
                    _usuario.value = uParaSubir
                }
            } catch (e: Exception) {
                // Error silencioso de red
            }
        }
    }

    fun refrescarUsuario(correo: String) {
        cargarUsuario(correo)
    }

    fun validarUsuario(correo: String, contrasena: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val exito = repository.validarUsuario(correo, contrasena)
            callback(exito)
        }
    }

    fun registrarUsuarioFirebase(usuario: Usuario, callback: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(usuario.correo, usuario.contrasena).await()
                if (result.user != null) {
                    // Inicializar perfil en Firestore con los campos nuevos
                    val usuarioInicial = usuario.copy(
                        totalTareasCompletadas = 0,
                        totalDailiesCompletadas = 0,
                        totalHabitosCompletados = 0,
                        preferenciaNotificacion = "08:00"
                    )
                    db.collection("usuarios").document(usuario.correo).set(usuarioInicial).await()
                    
                    withContext(Dispatchers.IO) {
                        repository.registrarUsuario(usuarioInicial)
                    }
                    callback(true, null)
                } else {
                    callback(false, "Error desconocido al crear usuario")
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

    fun registrarUsuario(usuario: Usuario, callback: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repository.registrarUsuario(usuario)
            callback(id)
        }
    }

    fun actualizarProgreso(correo: String, xp: Int, monedas: Int, hp: Int = 0) {
        viewModelScope.launch {
            // 1. Actualizar localmente por compatibilidad (opcional, si quieres 100% cloud puedes quitarlo luego)
            repository.actualizarProgresoUsuario(correo, xp, monedas, hp)
            
            // 2. Obtener el usuario actualizado de local para subirlo a Firebase
            val uActualizado = repository.obtenerUsuarioLogueado(correo)
            if (uActualizado != null) {
                db.collection("usuarios").document(correo).set(uActualizado)
            }
            
            cargarUsuario(correo)
        }
    }

    fun actualizarAtributos(correo: String, fza: Double, int: Double, con: Double, per: Double, puntos: Int) {
        viewModelScope.launch {
            repository.actualizarAtributos(correo, fza, int, con, per, puntos)
            val uActualizado = repository.obtenerUsuarioLogueado(correo)
            if (uActualizado != null) {
                db.collection("usuarios").document(correo).set(uActualizado)
            }
            cargarUsuario(correo)
        }
    }

    fun subirAtributo(correo: String, tipo: String) {
        viewModelScope.launch {
            val usuarioActual = _usuario.value ?: return@launch
            if (usuarioActual.puntosDisponibles <= 0) return@launch

            withContext(Dispatchers.IO) {
                when (tipo) {
                    "fza" -> repository.actualizarAtributos(correo, fza = 1.0, int = 0.0, con = 0.0, per = 0.0, puntos = 1)
                    "int" -> repository.actualizarAtributos(correo, fza = 0.0, int = 1.0, con = 0.0, per = 0.0, puntos = 1)
                    "con" -> repository.actualizarAtributos(correo, fza = 0.0, int = 0.0, con = 1.0, per = 0.0, puntos = 1)
                    "per" -> repository.actualizarAtributos(correo, fza = 0.0, int = 0.0, con = 0.0, per = 1.0, puntos = 1)
                }
            }
            // Sincronizar con Firestore después del cambio local
            val uActualizado = withContext(Dispatchers.IO) { repository.obtenerUsuarioLogueado(correo) }
            if (uActualizado != null) {
                db.collection("usuarios").document(correo).set(uActualizado).await()
                _usuario.value = uActualizado
            }
        }
    }

    fun comprarArticulo(correo: String, articulo: Articulo) {
        viewModelScope.launch {
            repository.comprarArticulo(correo, articulo)
            cargarUsuario(correo)
        }
    }

    fun equiparDesequipar(correo: String, idArticulo: Int) {
        viewModelScope.launch {
            repository.equiparDesequipar(correo, idArticulo)
            cargarUsuario(correo)
        }
    }

    fun usarPocion(correo: String, id: Int, curacion: Int) {
        viewModelScope.launch {
            repository.actualizarProgresoUsuario(correo, 0, 0, curacion)
            repository.eliminarDelInventario(id)
            cargarUsuario(correo)
        }
    }

    fun obtenerInventario(correo: String, callback: (List<Articulo>) -> Unit) {
        viewModelScope.launch {
            val inv = repository.obtenerInventario(correo)
            callback(inv)
        }
    }

    fun eliminarDelInventario(id: Int, correo: String) {
        viewModelScope.launch {
            repository.eliminarDelInventario(id)
            cargarUsuario(correo)
        }
    }
}
