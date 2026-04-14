package com.example.xpeando.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xpeando.model.Nota
import com.example.xpeando.repository.DataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotasViewModel(private val repository: DataRepository) : ViewModel() {

    private val _notas = MutableStateFlow<List<Nota>>(emptyList())
    val notas: StateFlow<List<Nota>> = _notas.asStateFlow()

    fun cargarNotas(correo: String) {
        viewModelScope.launch {
            val lista = repository.obtenerTodasNotas(correo)
            _notas.value = lista
        }
    }

    fun insertarNota(nota: Nota) {
        viewModelScope.launch {
            repository.insertarNota(nota)
            cargarNotas(nota.correo_usuario)
        }
    }

    fun actualizarNota(nota: Nota) {
        viewModelScope.launch {
            repository.actualizarNota(nota)
            cargarNotas(nota.correo_usuario)
        }
    }

    fun eliminarNota(id: Int, correo: String) {
        viewModelScope.launch {
            repository.eliminarNota(id)
            cargarNotas(correo)
        }
    }
}
