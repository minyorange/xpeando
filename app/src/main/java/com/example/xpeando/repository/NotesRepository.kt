package com.example.xpeando.repository

import com.example.xpeando.model.Nota
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class NotesRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun obtenerTodasNotas(correo: String): List<Nota> = withContext(Dispatchers.IO) {
        val snap = db.collection("usuarios").document(correo).collection("notas").get().await()
        snap.toObjects(Nota::class.java)
    }

    suspend fun insertarNota(nota: Nota): Long = withContext(Dispatchers.IO) {
        val ref = db.collection("usuarios").document(nota.correo_usuario).collection("notas").document()
        val id = ref.id.hashCode()
        ref.set(nota.copy(id = id)).await()
        id.toLong()
    }

    suspend fun actualizarNota(nota: Nota) = withContext(Dispatchers.IO) {
        val colRef = db.collection("usuarios").document(nota.correo_usuario).collection("notas")
        val snap = colRef.whereEqualTo("id", nota.id).get().await()
        for (doc in snap.documents) {
            doc.reference.set(nota).await()
        }
    }

    suspend fun eliminarNota(id: Int, correo: String) = withContext(Dispatchers.IO) {
        val colRef = db.collection("usuarios").document(correo).collection("notas")
        val snap = colRef.whereEqualTo("id", id).get().await()
        for (doc in snap.documents) {
            doc.reference.delete().await()
        }
    }
}
