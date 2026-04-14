package com.example.xpeando.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xpeando.repository.DataRepository
import com.example.xpeando.model.Logro
import com.example.xpeando.utils.LogroManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LogrosViewModel(private val repository: DataRepository) : ViewModel() {
    private val _logros = MutableStateFlow<List<Logro>>(emptyList())
    val logros: StateFlow<List<Logro>> = _logros.asStateFlow()

    fun cargarLogros(correo: String) {
        viewModelScope.launch {
            val lista = withContext(Dispatchers.IO) {
                val usuario = repository.obtenerUsuarioLogueado(correo)
                if (usuario != null) {
                    LogroManager.obtenerLogrosDefinidos(repository, usuario)
                } else {
                    emptyList()
                }
            }
            _logros.value = lista
        }
    }
}
