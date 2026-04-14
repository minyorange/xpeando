package com.example.xpeando.database.daos

import android.content.ContentValues
import com.example.xpeando.database.DBHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogroDao(private val dbHelper: DBHelper) {

    fun esLogroDesbloqueado(correo: String, nombre: String): Boolean {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM logros_usuario WHERE correo = ? AND nombre_logro = ?", arrayOf(correo, nombre))
        val res = cursor.count > 0
        cursor.close()
        return res
    }

    fun desbloquearLogro(correo: String, nombre: String) {
        val db = dbHelper.writableDatabase
        val v = ContentValues().apply {
            put("correo", correo)
            put("nombre_logro", nombre)
            put("fecha", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
        }
        db.insert("logros_usuario", null, v)
    }
}
