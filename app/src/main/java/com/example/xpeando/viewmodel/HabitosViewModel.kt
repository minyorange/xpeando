package com.example.xpeando.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xpeando.model.Habito
import com.example.xpeando.model.Usuario
import com.example.xpeando.repository.DataRepository
import com.example.xpeando.repository.TaskRepository
import com.example.xpeando.utils.LogroManager
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HabitosViewModel(
    private val taskRepository: TaskRepository,
    private val userRepository: DataRepository,
    private val rpgRepository: com.example.xpeando.repository.RpgRepository
) : ViewModel() {

    private val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    private val _habitos = MutableStateFlow<List<Habito>>(emptyList())
    val habitos: StateFlow<List<Habito>> = _habitos.asStateFlow()

    private val _usuario = MutableStateFlow<Usuario?>(null)
    val usuario: StateFlow<Usuario?> = _usuario.asStateFlow()

    private var habitosListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var usuarioListener: com.google.firebase.firestore.ListenerRegistration? = null

    fun cargarHabitos(correo: String) {
        if (correo.isEmpty()) return
        
        habitosListener?.remove()
        habitosListener = db.collection("usuarios").document(correo).collection("habitos")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    _habitos.value = snapshot.toObjects(Habito::class.java)
                }
            }

        usuarioListener?.remove()
        usuarioListener = db.collection("usuarios").document(correo)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    _usuario.value = snapshot.toObject(Usuario::class.java)
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        habitosListener?.remove()
        usuarioListener?.remove()
    }

    fun procesarAccion(context: Context, habito: Habito, delta: Int, correo: String, onNivelSubido: (Int) -> Unit) {
        viewModelScope.launch {
            val uActual = _usuario.value
            
            val xpCambio = habito.experiencia * delta
            val monedasCambio = if (delta > 0) habito.monedas else 0
            val hpCambio = if (delta < 0) -5 else 0
            
            launch(Dispatchers.IO) {
                if (delta > 0) {
                    taskRepository.actualizarEstadoHabito(habito, delta)
                    userRepository.actualizarProgresoUsuario(
                        correo, 
                        xpCambio, 
                        monedasCambio, 
                        hpCambio, 
                        tipoAccion = "HABITO", 
                        atributoAIncrementar = habito.atributo
                    )
                    
                    // DAÑO AL JEFE: Cuando se completa un hábito positivo
                    rpgRepository.atacarJefe(xpCambio, correo)

                    uActual?.let { usuario ->
                        LogroManager.verificarNuevosLogros(
                            context,
                            userRepository,
                            rpgRepository,
                            usuario,
                            "HABITO"
                        )
                    }
                } else {
                    userRepository.actualizarProgresoUsuario(correo, xpCambio, monedasCambio, hpCambio, tipoAccion = null)
                }
            }

            if (delta > 0) {
                com.example.xpeando.utils.XpeandoToast.mostrarProgreso(context, xpCambio, monedasCambio)
            } else {
                com.example.xpeando.utils.XpeandoToast.mostrarPenalizacion(context, hpCambio, xpCambio)
            }
        }
    }

    fun insertarHabito(habito: Habito) {
        viewModelScope.launch {
            taskRepository.insertarHabito(habito)
            cargarHabitos(habito.correo_usuario)
        }
    }

    fun eliminarHabito(id: Int, correo: String) {
        viewModelScope.launch {
            taskRepository.eliminarHabito(id, correo)
            cargarHabitos(correo)
        }
    }
}
