package com.example.xpeando.database.daos

import android.content.ContentValues
import com.example.xpeando.database.DBHelper
import com.example.xpeando.model.Nota

class NotaDao(private val dbHelper: DBHelper) {

    fun insertarNota(nota: Nota): Long {
        val db = dbHelper.writableDatabase
        val valores = ContentValues().apply {
            put("correo_usuario", nota.correo_usuario)
            put("titulo", nota.titulo)
            put("contenido", nota.contenido)
            put("fecha", nota.fecha)
        }
        val id = db.insert("notas", null, valores)
        return id
    }

    fun obtenerTodasNotas(correo: String): List<Nota> {
        val lista = mutableListOf<Nota>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM notas WHERE correo_usuario = ? ORDER BY fecha DESC", arrayOf(correo))
        if (cursor.moveToFirst()) {
            do {
                lista.add(Nota(
                    cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("correo_usuario")),
                    cursor.getString(cursor.getColumnIndexOrThrow("titulo")),
                    cursor.getString(cursor.getColumnIndexOrThrow("contenido")),
                    cursor.getLong(cursor.getColumnIndexOrThrow("fecha"))
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return lista
    }

    fun eliminarNota(id: Int) {
        val db = dbHelper.writableDatabase
        db.delete("notas", "id = ?", arrayOf(id.toString()))
    }

    fun actualizarNota(nota: Nota) {
        val db = dbHelper.writableDatabase
        val valores = ContentValues().apply {
            put("titulo", nota.titulo)
            put("contenido", nota.contenido)
        }
        db.update("notas", valores, "id = ?", arrayOf(nota.id.toString()))
    }
}
