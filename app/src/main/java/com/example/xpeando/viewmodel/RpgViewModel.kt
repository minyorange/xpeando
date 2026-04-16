package com.example.xpeando.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xpeando.repository.DataRepository
import com.example.xpeando.model.Jefe
import com.example.xpeando.model.Articulo
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class RpgViewModel(private val repository: DataRepository) : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val _jefeActivo = MutableStateFlow<Jefe?>(null)
    val jefeActivo: StateFlow<Jefe?> = _jefeActivo.asStateFlow()

    private val _historialJefes = MutableStateFlow<List<Jefe>>(emptyList())
    val historialJefes: StateFlow<List<Jefe>> = _historialJefes.asStateFlow()

    private val _tienda = MutableStateFlow<List<Articulo>>(emptyList())
    val tienda: StateFlow<List<Articulo>> = _tienda.asStateFlow()

    private val _inventario = MutableStateFlow<List<Articulo>>(emptyList())
    val inventario: StateFlow<List<Articulo>> = _inventario.asStateFlow()

    private var jefeListener: ListenerRegistration? = null

    fun cargarJefeActivo(correo: String) {
        if (correo.isEmpty()) return
        
        jefeListener?.remove()
        
        // ESCUCHA EN TIEMPO REAL DEL JEFE
        jefeListener = db.collection("usuarios").document(correo)
            .collection("rpg").document("jefe_activo")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    viewModelScope.launch {
                        _jefeActivo.value = withContext(Dispatchers.IO) { repository.obtenerJefeActivo(correo) }
                    }
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    _jefeActivo.value = snapshot.toObject(Jefe::class.java)
                } else {
                    // Si no hay jefe en la nube, intentar cargar local y subir (inicialización)
                    viewModelScope.launch {
                        val jefeLocal = withContext(Dispatchers.IO) { repository.obtenerJefeActivo(correo) }
                        if (jefeLocal != null) {
                            db.collection("usuarios").document(correo)
                                .collection("rpg").document("jefe_activo").set(jefeLocal)
                            _jefeActivo.value = jefeLocal
                        }
                    }
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        jefeListener?.remove()
    }

    fun cargarHistorial(correo: String) {
        viewModelScope.launch {
            try {
                val snapshot = db.collection("usuarios").document(correo)
                    .collection("rpg_historial").get().await()
                val historial = snapshot.toObjects(Jefe::class.java)
                
                if (historial.isEmpty()) {
                    val locales = withContext(Dispatchers.IO) { repository.obtenerJefesDerrotados(correo) }
                    if (locales.isNotEmpty()) {
                        locales.forEach { 
                            db.collection("usuarios").document(correo)
                                .collection("rpg_historial").document(it.id.toString()).set(it)
                        }
                        _historialJefes.value = locales
                        return@launch
                    }
                }
                _historialJefes.value = historial
            } catch (e: Exception) {
                _historialJefes.value = withContext(Dispatchers.IO) { repository.obtenerJefesDerrotados(correo) }
            }
        }
    }

    fun atacarJefe(danioBase: Int, correo: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                // La lógica de daño compleja la sigue haciendo el repository (DAO) por ahora
                // pero luego sincronizamos el resultado a Firestore
                val derrotado = withContext(Dispatchers.IO) {
                    repository.atacarJefe(danioBase, correo)
                }
                
                // Obtener el estado actualizado tras el ataque y subir a Firestore
                val jefeActualizado = withContext(Dispatchers.IO) { repository.obtenerJefeActivo(correo) }
                if (jefeActualizado != null) {
                    db.collection("usuarios").document(correo)
                        .collection("rpg").document("jefe_activo").set(jefeActualizado)
                }

                if (derrotado) {
                    // Si fue derrotado, el repository lo mueve a derrotados en local, 
                    // así que sincronizamos el historial también
                    val historialActualizado = withContext(Dispatchers.IO) { repository.obtenerJefesDerrotados(correo) }
                    historialActualizado.forEach { 
                        db.collection("usuarios").document(correo)
                            .collection("rpg_historial").document(it.id.toString()).set(it)
                    }

                    // Sincronizar Historial de Progreso para Estadísticas
                    val uActualizado = withContext(Dispatchers.IO) { repository.obtenerUsuarioLogueado(correo) }
                    if (uActualizado != null) {
                        db.collection("usuarios").document(correo).set(uActualizado)
                        
                        // Subir entrada de historial de jefe derrotado
                        val localHist = withContext(Dispatchers.IO) { repository.obtenerHistorialCompleto(correo) }
                        val ultimaEntrada = localHist.filter { it.tipo == "GANANCIA" }.lastOrNull()
                        if (ultimaEntrada != null) {
                             db.collection("usuarios").document(correo)
                                .collection("historial_progreso").document(ultimaEntrada.id.toString()).set(ultimaEntrada)
                        }
                    }

                    cargarHistorial(correo)
                }
                
                cargarJefeActivo(correo)
                onResult(derrotado)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    // --- TIENDA E INVENTARIO ---
    fun cargarTienda() {
        viewModelScope.launch {
            try {
                val snapshot = db.collection("tienda_rpg").get().await()
                val items = snapshot.toObjects(Articulo::class.java)
                if (items.isEmpty()) {
                    // Si la tienda global está vacía, cargar local
                    _tienda.value = withContext(Dispatchers.IO) { repository.obtenerTiendaRPG() }
                } else {
                    _tienda.value = items
                }
            } catch (e: Exception) {
                _tienda.value = withContext(Dispatchers.IO) { repository.obtenerTiendaRPG() }
            }
        }
    }

    fun cargarInventario(correo: String) {
        viewModelScope.launch {
            try {
                val snapshot = db.collection("usuarios").document(correo)
                    .collection("inventario").get().await()
                val items = snapshot.toObjects(Articulo::class.java)
                
                if (items.isEmpty()) {
                    val locales = withContext(Dispatchers.IO) { repository.obtenerInventario(correo) }
                    if (locales.isNotEmpty()) {
                        locales.forEach { 
                            db.collection("usuarios").document(correo)
                                .collection("inventario").document(it.id.toString()).set(it)
                        }
                        _inventario.value = locales
                        return@launch
                    }
                }
                _inventario.value = items
            } catch (e: Exception) {
                _inventario.value = withContext(Dispatchers.IO) { repository.obtenerInventario(correo) }
            }
        }
    }

    fun comprarArticulo(correo: String, articulo: Articulo, onExito: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                // 1. Compra en local (DAO gestiona monedas y lógica)
                val exito = withContext(Dispatchers.IO) { repository.comprarArticulo(correo, articulo) }
                if (exito) {
                    // 2. Sincronizar inventario a Firestore
                    val invLocal = withContext(Dispatchers.IO) { repository.obtenerInventario(correo) }
                    invLocal.forEach { 
                        db.collection("usuarios").document(correo)
                            .collection("inventario").document(it.id.toString()).set(it)
                    }
                    // 3. Sincronizar usuario (monedas)
                    val u = withContext(Dispatchers.IO) { repository.obtenerUsuarioLogueado(correo) }
                    if (u != null) db.collection("usuarios").document(correo).set(u)
                    
                    cargarInventario(correo)
                }
                onExito(exito)
            } catch (e: Exception) {
                onExito(false)
            }
        }
    }
    
    suspend fun obtenerUltimoJefeDerrotadoTime(correo: String): Long {
        return withContext(Dispatchers.IO) {
            repository.obtenerUltimoJefeDerrotadoTime(correo)
        }
    }
}
