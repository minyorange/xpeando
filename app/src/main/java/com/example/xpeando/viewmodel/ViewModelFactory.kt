package com.example.xpeando.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.xpeando.repository.DataRepository

class ViewModelFactory(private val repository: DataRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(UsuarioViewModel::class.java) -> UsuarioViewModel(repository) as T
            modelClass.isAssignableFrom(TareasViewModel::class.java) -> TareasViewModel(repository) as T
            modelClass.isAssignableFrom(DailiesViewModel::class.java) -> DailiesViewModel(repository) as T
            modelClass.isAssignableFrom(HabitosViewModel::class.java) -> HabitosViewModel(repository) as T
            modelClass.isAssignableFrom(NotasViewModel::class.java) -> NotasViewModel(repository) as T
            modelClass.isAssignableFrom(RpgViewModel::class.java) -> RpgViewModel(repository) as T
            modelClass.isAssignableFrom(EstadisticasViewModel::class.java) -> EstadisticasViewModel(repository) as T
            modelClass.isAssignableFrom(RecompensasViewModel::class.java) -> RecompensasViewModel(repository) as T
            modelClass.isAssignableFrom(LogrosViewModel::class.java) -> LogrosViewModel(repository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
