package com.example.xpeando.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xpeando.repository.DataRepository
import com.example.xpeando.model.Jefe
import com.example.xpeando.model.Articulo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RpgViewModel(private val repository: DataRepository) : ViewModel() {

    private val _jefeActivo = MutableStateFlow<Jefe?>(null)
    val jefeActivo: StateFlow<Jefe?> = _jefeActivo.asStateFlow()

    // Guardamos el HP anterior para animar al entrar al fragmento
    var hpAntesDeCambio: Int = -1
    private var primerCarga = true

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
        
        // Escuchar cambios en el jefe activo en tiempo real
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
                        // Si está derrotado pero ya pasó el tiempo, pedir uno nuevo
                        viewModelScope.launch {
                            val nuevoJefe = repository.obtenerJefeActivo(correo)
                            _jefeActivo.value = nuevoJefe
                        }
                    } else {
                        // Emitimos el jefe tal cual (vivo o derrotado en cooldown)
                        _jefeActivo.value = jefe
                    }
                }
            } else {
                // No hay jefe, pedir uno nuevo
                viewModelScope.launch {
                    val nuevoJefe = repository.obtenerJefeActivo(correo)
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
            val historial = repository.obtenerHistorialJefes(correo)
            _historialJefes.value = historial
        }
    }

    suspend fun obtenerUltimoJefeDerrotadoTime(correo: String): Long {
        if (correo.isEmpty()) return 0L
        val historial = repository.obtenerHistorialJefes(correo)
        return historial.maxByOrNull { it.fechaMuerte }?.fechaMuerte ?: 0L
    }

    fun atacarJefe(danioBase: Int, correo: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val derrotado = repository.atacarJefe(danioBase, correo)
            if (derrotado) {
                cargarHistorial(correo)
            }
            cargarJefeActivo(correo)
            onResult(derrotado)
        }
    }

    fun cargarTienda() {
        viewModelScope.launch {
            val items = repository.obtenerTiendaRPG()
            _tienda.value = items
        }
    }

    fun cargarInventario(correo: String) {
        if (correo.isEmpty()) return
        viewModelScope.launch {
            val items = repository.obtenerInventario(correo)
            _inventario.value = items
        }
    }

    fun comprarArticulo(correo: String, articulo: Articulo, onExito: (Boolean) -> Unit) {
        viewModelScope.launch {
            val exito = repository.comprarArticulo(correo, articulo)
            if (exito) {
                cargarInventario(correo)
            }
            onExito(exito)
        }
    }
}
