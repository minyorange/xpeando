package com.example.xpeando.repository

import com.example.xpeando.model.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class TaskRepository {
    private val db = FirebaseFirestore.getInstance()

    // --- MISIONES (TAREAS) ---
    suspend fun obtenerTodasLasTareas(correo: String): List<Tarea> = withContext(Dispatchers.IO) {
        val snap = db.collection("usuarios").document(correo).collection("tareas").get().await()
        snap.toObjects(Tarea::class.java)
    }

    suspend fun insertarTarea(tarea: Tarea): Long = withContext(Dispatchers.IO) {
        val ref = db.collection("usuarios").document(tarea.correo_usuario).collection("tareas").document()
        val id = ref.id.hashCode()
        ref.set(tarea.copy(id = id)).await()
        id.toLong()
    }

    suspend fun actualizarTarea(tarea: Tarea) = withContext(Dispatchers.IO) {
        val snap = db.collection("usuarios").document(tarea.correo_usuario).collection("tareas").whereEqualTo("id", tarea.id).get().await()
        snap.documents.forEach { it.reference.set(tarea).await() }
    }

    suspend fun actualizarEstadoTarea(id: Int, correo: String, completada: Boolean) = withContext(Dispatchers.IO) {
        val snap = db.collection("usuarios").document(correo).collection("tareas").whereEqualTo("id", id).get().await()
        for (doc in snap.documents) {
            doc.reference.update("completada", completada).await()
            if (completada) {
                val completadas = db.collection("usuarios").document(correo).collection("tareas").whereEqualTo("completada", true).get().await()
                if (completadas.size() > 10) {
                    val listaOrdenada = completadas.documents.sortedBy { it.getLong("id") ?: 0L }
                    val cantidadABorrar = completadas.size() - 10
                    for (i in 0 until cantidadABorrar) {
                        listaOrdenada[i].reference.delete().await()
                    }
                }
            }
        }
    }

    suspend fun eliminarTarea(id: Int, correo: String) = withContext(Dispatchers.IO) {
        val snap = db.collection("usuarios").document(correo).collection("tareas").whereEqualTo("id", id).get().await()
        snap.documents.forEach { it.reference.delete().await() }
    }

    // --- DAILIES ---
    suspend fun obtenerTodasDailies(correo: String): List<Daily> = withContext(Dispatchers.IO) {
        val snap = db.collection("usuarios").document(correo).collection("dailies").get().await()
        snap.toObjects(Daily::class.java)
    }

    suspend fun actualizarEstadoDaily(daily: Daily, completada: Boolean) = withContext(Dispatchers.IO) {
        val hoy = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val snap = db.collection("usuarios").document(daily.correo_usuario).collection("dailies").whereEqualTo("id", daily.id).get().await()
        for (doc in snap.documents) {
            if (completada) doc.reference.update("ultimaVezCompletada", hoy).await()
            else doc.reference.update("ultimaVezCompletada", "").await()
        }
    }

    suspend fun insertarDaily(daily: Daily): Long = withContext(Dispatchers.IO) {
        val ref = db.collection("usuarios").document(daily.correo_usuario).collection("dailies").document()
        val id = ref.id.hashCode()
        ref.set(daily.copy(id = id)).await()
        id.toLong()
    }

    suspend fun eliminarDaily(id: Int, correo: String) = withContext(Dispatchers.IO) {
        val snap = db.collection("usuarios").document(correo).collection("dailies").whereEqualTo("id", id).get().await()
        snap.documents.forEach { it.reference.delete().await() }
    }

    // --- HÁBITOS ---
    suspend fun obtenerTodosHabitos(correo: String): List<Habito> = withContext(Dispatchers.IO) {
        val snap = db.collection("usuarios").document(correo).collection("habitos").get().await()
        snap.toObjects(Habito::class.java)
    }

    suspend fun actualizarEstadoHabito(habito: Habito, delta: Int) = withContext(Dispatchers.IO) {
        val snap = db.collection("usuarios").document(habito.correo_usuario).collection("habitos").whereEqualTo("id", habito.id).get().await()
        snap.documents.forEach { it.reference.update("vecesCompletado", habito.vecesCompletado + delta).await() }
    }

    suspend fun insertarHabito(habito: Habito): Long = withContext(Dispatchers.IO) {
        val ref = db.collection("usuarios").document(habito.correo_usuario).collection("habitos").document()
        val id = ref.id.hashCode()
        ref.set(habito.copy(id = id)).await()
        id.toLong()
    }

    suspend fun eliminarHabito(id: Int, correo: String) = withContext(Dispatchers.IO) {
        val snap = db.collection("usuarios").document(correo).collection("habitos").whereEqualTo("id", id).get().await()
        snap.documents.forEach { it.reference.delete().await() }
    }
}
