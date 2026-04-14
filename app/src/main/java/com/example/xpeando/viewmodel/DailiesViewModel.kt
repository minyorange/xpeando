package com.example.xpeando.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xpeando.model.Daily
import com.example.xpeando.model.Usuario
import com.example.xpeando.repository.DataRepository
import com.example.xpeando.utils.LogroManager
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DailiesViewModel(private val repository: DataRepository) : ViewModel() {

    private val _dailies = MutableStateFlow<List<Daily>>(emptyList())
    val dailies: StateFlow<List<Daily>> = _dailies.asStateFlow()

    private val _usuario = MutableStateFlow<Usuario?>(null)
    val usuario: StateFlow<Usuario?> = _usuario.asStateFlow()

    fun cargarDailies(correo: String) {
        viewModelScope.launch {
            val todas = withContext(Dispatchers.IO) { repository.obtenerTodasDailies(correo) }
            _dailies.value = todas.filter { !it.completadaHoy }
            val u = withContext(Dispatchers.IO) { repository.obtenerUsuarioLogueado(correo) }
            _usuario.value = u
        }
    }

    fun completarDaily(context: Context, daily: Daily, correo: String, onNivelSubido: (Int) -> Unit) {
        viewModelScope.launch {
            val usuarioAntes = withContext(Dispatchers.IO) { repository.obtenerUsuarioLogueado(correo) }
            val nivelAntes = usuarioAntes?.nivel ?: 1
            val dailiesAntes = withContext(Dispatchers.IO) { repository.obtenerTotalDailiesCompletadas(correo) }
            val monedasAntes = usuarioAntes?.monedas ?: 0

            withContext(Dispatchers.IO) {
                repository.actualizarEstadoDaily(daily, true)
                repository.actualizarRacha(correo)
                repository.actualizarProgresoUsuario(correo, daily.experiencia, daily.monedas)
                repository.atacarJefe(25, correo)
            }

            val usuarioDespues = withContext(Dispatchers.IO) { repository.obtenerUsuarioLogueado(correo) } ?: return@launch
            val nivelDespues = usuarioDespues.nivel
            val dailiesDespues = withContext(Dispatchers.IO) { repository.obtenerTotalDailiesCompletadas(correo) }
            val monedasDespues = usuarioDespues.monedas

            if (nivelDespues > nivelAntes) {
                onNivelSubido(nivelDespues)
                LogroManager.verificarNuevosLogros(context, repository, usuarioDespues, nivelAntes, nivelDespues, "NIVEL")
            }
            
            LogroManager.verificarNuevosLogros(context, repository, usuarioDespues, dailiesAntes, dailiesDespues, "DAILY")
            LogroManager.verificarNuevosLogros(context, repository, usuarioDespues, monedasAntes, monedasDespues, "MONEDAS")

            cargarDailies(correo)
        }
    }

    fun insertarDaily(daily: Daily) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.insertarDaily(daily) }
            cargarDailies(daily.correo_usuario)
        }
    }

    fun eliminarDaily(id: Int, correo: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.eliminarDaily(id) }
            cargarDailies(correo)
        }
    }

    fun procesarDailiesFallidas(correo: String, dias: Int): Int {
        // Esta operación es sincrónica en el DAO original, pero la llamaremos asíncronamente
        // En el ViewModel, solo devolvemos el daño para mostrar el feedback
        var danio = 0
        viewModelScope.launch {
            danio = withContext(Dispatchers.IO) { repository.procesarDailiesFallidas(correo, dias) }
            cargarDailies(correo)
        }
        return danio
    }
}
