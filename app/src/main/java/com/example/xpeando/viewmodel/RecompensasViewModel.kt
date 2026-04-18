package com.example.xpeando.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xpeando.model.Articulo
import com.example.xpeando.model.Recompensa
import com.example.xpeando.repository.DataRepository
import com.example.xpeando.repository.RpgRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class RecompensasViewModel(
    private val userRepository: DataRepository,
    private val rpgRepository: RpgRepository
) : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val _state = MutableStateFlow(RecompensasState())
    val state: StateFlow<RecompensasState> = _state.asStateFlow()

    private var recompensasListener: ListenerRegistration? = null
    private var usuarioListener: ListenerRegistration? = null

    fun cargarDatos(correo: String) {
        if (correo.isEmpty()) return

        usuarioListener?.remove()
        usuarioListener = db.collection("usuarios").document(correo)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val u = snapshot.toObject(com.example.xpeando.model.Usuario::class.java)
                    _state.value = _state.value.copy(usuario = u)
                }
            }

        recompensasListener?.remove()
        recompensasListener = db.collection("usuarios").document(correo)
            .collection("recompensas")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val lista = snapshot.toObjects(Recompensa::class.java)
                    if (lista.isEmpty()) {
                        val ejemplos = listOf(
                            Recompensa(correo_usuario = correo, nombre = "Ver un capítulo de serie", precio = 30),
                            Recompensa(correo_usuario = correo, nombre = "Comer un dulce/snack", precio = 50),
                            Recompensa(correo_usuario = correo, nombre = "1 hora de videojuegos", precio = 100)
                        )
                        ejemplos.forEach { r ->
                            val ref = db.collection("usuarios").document(correo).collection("recompensas").document()
                            db.collection("usuarios").document(correo).collection("recompensas")
                                .document(ref.id.hashCode().toString()).set(r.copy(id = ref.id.hashCode()))
                        }
                    } else {
                        _state.value = _state.value.copy(recompensasPersonales = lista)
                    }
                }
            }

        viewModelScope.launch {
            try {
                val items = withContext(Dispatchers.IO) { rpgRepository.obtenerTiendaRPG() }
                _state.value = _state.value.copy(armeria = items)
            } catch (e: Exception) {
                android.util.Log.e("RecompensasVM", "Error al cargar armería: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        recompensasListener?.remove()
        usuarioListener?.remove()
    }

    fun canjearRecompensaPersonal(context: Context, correo: String, recompensa: Recompensa, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val usuario = withContext(Dispatchers.IO) { userRepository.obtenerUsuarioLogueado(correo) }
            if (usuario != null && usuario.monedas >= recompensa.precio) {
                withContext(Dispatchers.IO) {
                    userRepository.actualizarProgresoUsuario(correo, 0, -recompensa.precio)
                }
                onResult(true, "¡Has canjeado ${recompensa.nombre}!")
            } else {
                onResult(false, "No tienes suficientes monedas")
            }
        }
    }

    fun comprarArticuloArmeria(context: Context, correo: String, articulo: Articulo, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val exito = withContext(Dispatchers.IO) { rpgRepository.comprarArticulo(correo, articulo) }
                if (exito) {
                    onResult(true, "¡Has comprado ${articulo.nombre}!")
                } else {
                    onResult(false, "No tienes suficientes monedas o ya posees este equipo")
                }
            } catch (e: Exception) {
                onResult(false, "Error al procesar la compra: ${e.message}")
            }
        }
    }

    fun insertarRecompensa(recompensa: Recompensa) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { 
                    userRepository.insertarRecompensa(recompensa) 
                }
            } catch (e: Exception) {
                android.util.Log.e("RecompensasVM", "Error al insertar: ${e.message}")
            }
        }
    }

    fun eliminarRecompensa(id: Int, correo: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    userRepository.eliminarRecompensa(id, correo)
                }
            } catch (e: Exception) {
                android.util.Log.e("RecompensasVM", "Error al eliminar: ${e.message}")
            }
        }
    }
}
