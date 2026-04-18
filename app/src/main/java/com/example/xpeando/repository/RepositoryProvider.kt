package com.example.xpeando.repository

object RepositoryProvider {
    val dataRepository by lazy { DataRepository() }
    val taskRepository by lazy { TaskRepository() }
    val rpgRepository by lazy { RpgRepository() }
    val notesRepository by lazy { NotesRepository() }
}
