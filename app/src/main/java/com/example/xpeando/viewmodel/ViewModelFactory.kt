package com.example.xpeando.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.xpeando.repository.DataRepository

class ViewModelFactory(private val repository: DataRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UsuarioViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UsuarioViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(RpgViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RpgViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(LogrosViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LogrosViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(PersonajeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PersonajeViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(HabitosViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HabitosViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(TareasViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TareasViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(DailiesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DailiesViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(NotasViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotasViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(EstadisticasViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EstadisticasViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(RecompensasViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RecompensasViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
