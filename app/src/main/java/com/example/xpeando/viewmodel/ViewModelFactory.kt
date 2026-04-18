package com.example.xpeando.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.xpeando.repository.*

class ViewModelFactory(
    private val dataRepository: DataRepository = RepositoryProvider.dataRepository,
    private val taskRepository: TaskRepository = RepositoryProvider.taskRepository,
    private val rpgRepository: RpgRepository = RepositoryProvider.rpgRepository,
    private val notesRepository: NotesRepository = RepositoryProvider.notesRepository
) : ViewModelProvider.Factory {

    // Constructor secundario para mantener compatibilidad con código antiguo que pasaba solo 1 repo
    constructor(oldRepository: DataRepository) : this(
        dataRepository = RepositoryProvider.dataRepository,
        taskRepository = RepositoryProvider.taskRepository,
        rpgRepository = RepositoryProvider.rpgRepository,
        notesRepository = RepositoryProvider.notesRepository
    )

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(UsuarioViewModel::class.java) -> UsuarioViewModel(dataRepository) as T
            modelClass.isAssignableFrom(TareasViewModel::class.java) -> TareasViewModel(taskRepository, dataRepository, rpgRepository) as T
            modelClass.isAssignableFrom(DailiesViewModel::class.java) -> DailiesViewModel(taskRepository, dataRepository, rpgRepository) as T
            modelClass.isAssignableFrom(HabitosViewModel::class.java) -> HabitosViewModel(taskRepository, dataRepository, rpgRepository) as T
            modelClass.isAssignableFrom(NotasViewModel::class.java) -> NotasViewModel(notesRepository) as T
            modelClass.isAssignableFrom(RpgViewModel::class.java) -> RpgViewModel(rpgRepository, dataRepository) as T
            modelClass.isAssignableFrom(EstadisticasViewModel::class.java) -> EstadisticasViewModel(dataRepository, rpgRepository) as T
            modelClass.isAssignableFrom(RecompensasViewModel::class.java) -> RecompensasViewModel(dataRepository, rpgRepository) as T
            modelClass.isAssignableFrom(LogrosViewModel::class.java) -> LogrosViewModel(dataRepository, rpgRepository) as T
            modelClass.isAssignableFrom(PersonajeViewModel::class.java) -> PersonajeViewModel(dataRepository, rpgRepository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
