package com.example.xpeando.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xpeando.repository.DataRepository
import com.example.xpeando.repository.RpgRepository
import com.example.xpeando.model.Jefe
import com.example.xpeando.model.Articulo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RpgViewModel(
    private val rpgRepository: RpgRepository,
    private val userRepository: DataRepository
) : ViewModel() {

    private val _jefeActivo = MutableStateFlow<Jefe?>(null)
    val jefeActivo: StateFlow<Jefe?> = _jefeActivo.asStateFlow()

    var hpAntesDeCambio: Int = -1

    private val _historialJefes = MutableStateFlow<List<Jefe>>(emptyList())
    val historialJefes: StateFlow<List<Jefe>> = _historialJefes.asStateFlow()

    private val _tienda = MutableStateFlow<List<Articulo>>(emptyList())
    val tienda: StateFlow<List<Articulo>> = _tienda.asStateFlow()

    private val _inventario = MutableStateFlow<List<Articulo>>(emptyList())
    val inventario: StateFlow<List<Articulo>> = _inventario.asStateFlow()

    private val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    private var jefeListener: com.google.firebase.firestore.ListenerRegistration? = null

    fun cargarJefeActivo(correo: String) {
        if (correo.isEmpty()) return
        
        jefeListener?.remove()
        
        val jefeRef = db.collection("usuarios").document(correo).collection("rpg").document("jefe_activo")
        jefeListener = jefeRef.addSnapshotListener { snapshot, e ->
            if (e != null) return@addSnapshotListener
            
            if (snapshot != null && snapshot.exists()) {
                val jefe = snapshot.toObject(Jefe::class.java)
                if (jefe != null) {
                    val ultimaMuerte = jefe.fechaMuerte
                    val tiempoRespawn = 21 * 60 * 60 * 1000L
                    val tiempoTranscurrido = System.currentTimeMillis() - ultimaMuerte
                    
                    if (jefe.derrotado && tiempoTranscurrido >= tiempoRespawn) {
                        viewModelScope.launch {
                            val nuevoJefe = rpgRepository.obtenerJefeActivo(correo)
                            _jefeActivo.value = nuevoJefe
                        }
                    } else {
                        _jefeActivo.value = jefe
                    }
                }
            } else {
                viewModelScope.launch {
                    val nuevoJefe = rpgRepository.obtenerJefeActivo(correo)
                    _jefeActivo.value = nuevoJefe
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        jefeListener?.remove()
    }

    fun cargarHistorial(correo: String) {
        if (correo.isEmpty()) return
        viewModelScope.launch {
            val historial = rpgRepository.obtenerHistorialJefes(correo)
            _historialJefes.value = historial
        }
    }

    fun atacarJefe(context: android.content.Context, danioBase: Int, correo: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val uOld = userRepository.obtenerUsuarioLogueado(correo)
            val derrotado = rpgRepository.atacarJefe(danioBase, correo)
            if (derrotado) {
                cargarHistorial(correo)
                uOld?.let {
                    com.example.xpeando.utils.LogroManager.verificarNuevosLogros(context, userRepository, rpgRepository, it, "RPG")
                }
            }
            onResult(derrotado)
        }
    }

    fun cargarTienda() {
        viewModelScope.launch {
            val items = rpgRepository.obtenerTiendaRPG()
            _tienda.value = items
        }
    }

    fun cargarInventario(correo: String) {
        if (correo.isEmpty()) return
        viewModelScope.launch {
            val items = rpgRepository.obtenerInventario(correo)
            _inventario.value = items
        }
    }

    fun comprarArticulo(context: android.content.Context, correo: String, articulo: Articulo, onExito: (Boolean) -> Unit) {
        viewModelScope.launch {
            val uOld = userRepository.obtenerUsuarioLogueado(correo)
            val exito = rpgRepository.comprarArticulo(correo, articulo)
            if (exito) {
                cargarInventario(correo)
                uOld?.let {
                    com.example.xpeando.utils.LogroManager.verificarNuevosLogros(context, userRepository, rpgRepository, it, "RPG")
                }
            }
            onExito(exito)
        }
    }

    fun equiparDesequipar(correo: String, idArticulo: Int) {
        viewModelScope.launch {
            rpgRepository.equiparDesequipar(correo, idArticulo)
            cargarInventario(correo)
        }
    }

    fun usarPocion(correo: String, idArticulo: Int, curacion: Int) {
        viewModelScope.launch {
            // 1. Curar al usuario
            userRepository.actualizarProgresoUsuario(correo, 0, 0, curacion)
            // 2. Quitar la poción de la mochila
            rpgRepository.eliminarDelInventario(idArticulo, correo)
            // 3. Recargar mochila
            cargarInventario(correo)
        }
    }

    suspend fun obtenerUltimoJefeDerrotadoTime(correo: String): Long {
        return rpgRepository.obtenerHistorialJefes(correo).firstOrNull { it.derrotado }?.fechaMuerte ?: 0L
    }
}
