package com.example.xpeando.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xpeando.model.Articulo
import com.example.xpeando.model.Recompensa
import com.example.xpeando.repository.DataRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class RecompensasViewModel(private val repository: DataRepository) : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val _state = MutableStateFlow(RecompensasState())
    val state: StateFlow<RecompensasState> = _state.asStateFlow()

    private var recompensasListener: ListenerRegistration? = null
    private var usuarioListener: ListenerRegistration? = null

    fun cargarDatos(correo: String) {
        if (correo.isEmpty()) return

        // 1. Escuchar Usuario en tiempo real (para monedas)
        usuarioListener?.remove()
        usuarioListener = db.collection("usuarios").document(correo)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val u = snapshot.toObject(com.example.xpeando.model.Usuario::class.java)
                    _state.value = _state.value.copy(usuario = u)
                }
            }

        // 2. Escuchar Recompensas Personalizadas
        recompensasListener?.remove()
        recompensasListener = db.collection("usuarios").document(correo)
            .collection("recompensas")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val lista = snapshot.toObjects(Recompensa::class.java)
                    if (lista.isEmpty()) {
                        viewModelScope.launch {
                            val locales = withContext(Dispatchers.IO) { repository.obtenerTodasRecompensas(correo) }
                            locales.forEach { 
                                db.collection("usuarios").document(correo)
                                    .collection("recompensas").document(it.id.toString()).set(it)
                            }
                        }
                    } else {
                        _state.value = _state.value.copy(recompensasPersonales = lista)
                    }
                }
            }

        // 3. Cargar Armería (Global)
        viewModelScope.launch {
            try {
                val snapshot = db.collection("tienda_rpg").get().await()
                val items = snapshot.toObjects(Articulo::class.java)
                
                if (items.isEmpty()) {
                    // Si la tienda global en la nube está vacía, cargamos los locales
                    val locales = withContext(Dispatchers.IO) { repository.obtenerTiendaRPG() }
                    _state.value = _state.value.copy(armeria = locales)
                    
                    // Opcional: Poblar la tienda global en Firestore si eres el primer usuario/admin
                    locales.forEach { articulo ->
                        db.collection("tienda_rpg").document(articulo.id.toString()).set(articulo)
                    }
                } else {
                    _state.value = _state.value.copy(armeria = items)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(armeria = withContext(Dispatchers.IO) { repository.obtenerTiendaRPG() })
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
            val usuario = withContext(Dispatchers.IO) { repository.obtenerUsuarioLogueado(correo) }
            if (usuario != null && usuario.monedas >= recompensa.precio) {
                withContext(Dispatchers.IO) {
                    repository.actualizarProgresoUsuario(correo, 0, -recompensa.precio)
                    val uActualizado = repository.obtenerUsuarioLogueado(correo)
                    if (uActualizado != null) {
                        db.collection("usuarios").document(correo).set(uActualizado)
                    }
                }
                onResult(true, "¡Has canjeado ${recompensa.nombre}!")
            } else {
                onResult(false, "No tienes suficientes monedas")
            }
        }
    }

    fun comprarArticuloArmeria(context: Context, correo: String, articulo: Articulo, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val usuario = withContext(Dispatchers.IO) { repository.obtenerUsuarioLogueado(correo) }
            if (usuario != null && usuario.monedas >= articulo.precio) {
                if (withContext(Dispatchers.IO) { repository.comprarArticulo(correo, articulo) }) {
                    withContext(Dispatchers.IO) {
                        repository.actualizarProgresoUsuario(correo, 0, -articulo.precio)
                        val uActualizado = repository.obtenerUsuarioLogueado(correo)
                        if (uActualizado != null) db.collection("usuarios").document(correo).set(uActualizado)
                        val inv = repository.obtenerInventario(correo)
                        inv.forEach { 
                            db.collection("usuarios").document(correo)
                                .collection("inventario").document(it.id.toString()).set(it)
                        }
                    }
                    onResult(true, "¡Has comprado ${articulo.nombre}!")
                } else {
                    onResult(false, "Error al procesar la compra")
                }
            } else {
                onResult(false, "No tienes suficientes monedas")
            }
        }
    }

    fun insertarRecompensa(recompensa: Recompensa) {
        viewModelScope.launch {
            val idLocal = withContext(Dispatchers.IO) { repository.insertarRecompensa(recompensa) }
            val rConId = recompensa.copy(id = idLocal.toInt())
            db.collection("usuarios").document(recompensa.correo_usuario)
                .collection("recompensas").document(idLocal.toString()).set(rConId).await()
        }
    }

    fun eliminarRecompensa(id: Int, correo: String) {
        viewModelScope.launch {
            db.collection("usuarios").document(correo)
                .collection("recompensas").document(id.toString()).delete().await()
            withContext(Dispatchers.IO) { repository.eliminarRecompensa(id) }
        }
    }
}
