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
                        // Si está vacío, insertamos ejemplos por defecto
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

        // 3. Cargar Armería (Global)
        viewModelScope.launch {
            try {
                val snapshot = db.collection("tienda_global").get().await()
                val items = snapshot.toObjects(Articulo::class.java)
                
                if (items.isEmpty()) {
                    // Si la tienda global en la nube está vacía, creamos los items básicos
                    val basicos = listOf(
                        Articulo(id = 1, nombre = "Espada de Madera", tipo = "EQUIPO", subtipo = "ARMA", precio = 150, bonusFza = 2, icono = "espada_madera"),
                        Articulo(id = 2, nombre = "Escudo de Hierro", tipo = "EQUIPO", subtipo = "ESCUDO", precio = 300, bonusCon = 3, icono = "escudo_hierro"),
                        Articulo(id = 3, nombre = "Poción de Vida", tipo = "CONSUMIBLE", subtipo = "POCION", precio = 50, bonusHp = 25, icono = "pocion_vida")
                    )
                    basicos.forEach { db.collection("tienda_global").document(it.id.toString()).set(it) }
                    _state.value = _state.value.copy(armeria = basicos)
                } else {
                    _state.value = _state.value.copy(armeria = items)
                }
            } catch (e: Exception) {
                // Fallback a local
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
            try {
                // El repositorio ya maneja la transacción de resta de monedas y adición al inventario
                val exito = withContext(Dispatchers.IO) { repository.comprarArticulo(correo, articulo) }
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
                // Dejamos que el repositorio maneje la inserción única
                withContext(Dispatchers.IO) { 
                    repository.insertarRecompensa(recompensa) 
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
                    repository.eliminarRecompensa(id, correo)
                }
                // También borrar de Firestore explícitamente si el ID coincide con el nombre del documento
                db.collection("usuarios").document(correo)
                    .collection("recompensas").whereEqualTo("id", id).get().await()
                    .documents.forEach { it.reference.delete().await() }
            } catch (e: Exception) {
                android.util.Log.e("RecompensasVM", "Error al eliminar: ${e.message}")
            }
        }
    }
}
