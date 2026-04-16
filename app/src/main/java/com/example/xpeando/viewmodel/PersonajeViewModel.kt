package com.example.xpeando.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xpeando.repository.DataRepository
import com.example.xpeando.model.Articulo
import com.example.xpeando.model.Usuario
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PersonajeViewModel(private val repository: DataRepository) : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val _usuario = MutableStateFlow<Usuario?>(null)
    val usuario: StateFlow<Usuario?> = _usuario.asStateFlow()

    private val _inventario = MutableStateFlow<List<Articulo>>(emptyList())
    val inventario: StateFlow<List<Articulo>> = _inventario.asStateFlow()

    private var correoActual: String? = null
    private var usuarioListener: ListenerRegistration? = null
    private var inventarioListener: ListenerRegistration? = null

    fun cargarDatos(correo: String) {
        if (correo.isEmpty()) return
        correoActual = correo

        // Escuchar Usuario en tiempo real
        usuarioListener?.remove()
        usuarioListener = db.collection("usuarios").document(correo)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null && snapshot.exists()) {
                    val user = snapshot.toObject(Usuario::class.java)
                    _usuario.value = user
                    
                    // Sincronizar hacia local si viene de la nube
                    viewModelScope.launch {
                        user?.let { repository.upsertUsuario(it) }
                    }
                }
            }

        // Escuchar Inventario en tiempo real
        inventarioListener?.remove()
        inventarioListener = db.collection("usuarios").document(correo)
            .collection("inventario")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    val lista = snapshot.toObjects(Articulo::class.java)
                    if (lista.isEmpty()) {
                        viewModelScope.launch {
                            val invLocales = withContext(Dispatchers.IO) { repository.obtenerInventario(correo) }
                            invLocales.forEach { art ->
                                db.collection("usuarios").document(correo)
                                    .collection("inventario").document(art.id.toString()).set(art)
                            }
                        }
                    } else {
                        _inventario.value = lista
                    }
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        usuarioListener?.remove()
        inventarioListener?.remove()
    }

    fun refrescarSiEsNecesario() {
        correoActual?.let { cargarDatos(it) }
    }

    fun subirAtributo(correo: String, tipo: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                when (tipo) {
                    "fza" -> repository.actualizarAtributos(correo, fza = 1.0, int = 0.0, con = 0.0, per = 0.0, puntos = 1)
                    "int" -> repository.actualizarAtributos(correo, fza = 0.0, int = 1.0, con = 0.0, per = 0.0, puntos = 1)
                    "con" -> repository.actualizarAtributos(correo, fza = 0.0, int = 0.0, con = 1.0, per = 0.0, puntos = 1)
                    "per" -> repository.actualizarAtributos(correo, fza = 0.0, int = 0.0, con = 0.0, per = 1.0, puntos = 1)
                }
            }
            cargarDatos(correo)
        }
    }

    fun equiparDesequipar(correo: String, idArticulo: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.equiparDesequipar(correo, idArticulo) }
            cargarDatos(correo)
        }
    }

    fun usarPocion(correo: String, id: Int, curacion: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.actualizarProgresoUsuario(correo, 0, 0, curacion)
                repository.eliminarDelInventario(id)
            }
            cargarDatos(correo)
        }
    }
}
