package com.example.xpeando.database.daos

import android.content.ContentValues
import com.example.xpeando.database.DBHelper
import com.example.xpeando.model.Habito
import com.example.xpeando.model.Tarea
import com.example.xpeando.model.Daily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TareaDao(private val dbHelper: DBHelper) {

    // --- HÁBITOS ---
    fun insertarHabito(habito: Habito): Long {
        val db = dbHelper.writableDatabase
        val valores = ContentValues().apply {
            put("correo_usuario", habito.correo_usuario)
            put("nombre", habito.nombre)
            put("experiencia", habito.experiencia)
            put("monedas", habito.monedas)
            put("completadoHoy", if (habito.completadoHoy) 1 else 0)
            put("atributo", habito.atributo)
            put("vecesCompletado", habito.vecesCompletado)
        }
        val id = db.insert("habitos", null, valores)
        return id
    }

    fun obtenerTodosHabitos(correo: String): List<Habito> {
        val lista = mutableListOf<Habito>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM habitos WHERE correo_usuario = ?", arrayOf(correo))
        if (cursor.moveToFirst()) {
            do {
                lista.add(Habito(
                    cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("correo_usuario")),
                    cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("experiencia")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("monedas")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("completadoHoy")) == 1,
                    cursor.getString(cursor.getColumnIndexOrThrow("atributo")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("vecesCompletado"))
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return lista
    }

    fun eliminarHabito(id: Int) {
        val db = dbHelper.writableDatabase
        db.delete("habitos", "id = ?", arrayOf(id.toString()))
    }

    fun actualizarEstadoHabito(habito: Habito) {
        val db = dbHelper.writableDatabase
        val valores = ContentValues().apply {
            put("completadoHoy", if (habito.completadoHoy) 1 else 0)
            if (habito.completadoHoy) {
                // Si se marca como completado, incrementamos el contador total
                db.execSQL("UPDATE habitos SET vecesCompletado = vecesCompletado + 1 WHERE id = ?", arrayOf(habito.id))
            }
        }
        db.update("habitos", valores, "id = ?", arrayOf(habito.id.toString()))
    }

    // --- TAREAS ---
    fun insertarTarea(tarea: Tarea): Long {
        val db = dbHelper.writableDatabase
        val valores = ContentValues().apply {
            put("correo_usuario", tarea.correo_usuario)
            put("nombre", tarea.nombre)
            put("dificultad", tarea.dificultad)
            put("experiencia", tarea.experiencia)
            put("monedas", tarea.monedas)
            put("completada", if (tarea.completada) 1 else 0)
        }
        val id = db.insert("tareas", null, valores)
        return id
    }

    fun obtenerTodasLasTareas(correo: String): List<Tarea> {
        val lista = mutableListOf<Tarea>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM tareas WHERE correo_usuario = ?", arrayOf(correo))
        if (cursor.moveToFirst()) {
            do {
                lista.add(Tarea(
                    cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("correo_usuario")),
                    cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("dificultad")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("experiencia")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("monedas")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("completada")) == 1
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return lista
    }

    fun eliminarTarea(id: Int) {
        val db = dbHelper.writableDatabase
        db.delete("tareas", "id = ?", arrayOf(id.toString()))
    }

    fun actualizarTarea(tarea: Tarea) {
        val db = dbHelper.writableDatabase
        val valores = ContentValues().apply {
            put("nombre", tarea.nombre)
            put("dificultad", tarea.dificultad)
            put("experiencia", tarea.experiencia)
            put("monedas", tarea.monedas)
            put("completada", if (tarea.completada) 1 else 0)
        }
        db.update("tareas", valores, "id = ?", arrayOf(tarea.id.toString()))
    }

    // --- DAILIES ---
    fun insertarDaily(daily: Daily): Long {
        val db = dbHelper.writableDatabase
        val valores = ContentValues().apply {
            put("correo_usuario", daily.correo_usuario)
            put("nombre", daily.nombre)
            put("dificultad", daily.dificultad)
            put("experiencia", daily.experiencia)
            put("monedas", daily.monedas)
            put("completadaHoy", 0)
            put("ultimaVezCompletada", "")
        }
        val id = db.insert("dailies", null, valores)
        return id
    }

    fun obtenerTodasDailies(correo: String): List<Daily> {
        val lista = mutableListOf<Daily>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM dailies WHERE correo_usuario = ?", arrayOf(correo))
        val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        if (cursor.moveToFirst()) {
            do {
                val ultimaVez = cursor.getString(cursor.getColumnIndexOrThrow("ultimaVezCompletada"))
                lista.add(Daily(
                    cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("correo_usuario")),
                    cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("dificultad")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("experiencia")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("monedas")),
                    ultimaVez == hoy,
                    ultimaVez
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return lista
    }

    fun actualizarEstadoDaily(daily: Daily, completada: Boolean) {
        val db = dbHelper.writableDatabase
        val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val valores = ContentValues().apply {
            put("completadaHoy", if (completada) 1 else 0)
            put("ultimaVezCompletada", if (completada) hoy else "")
        }
        db.update("dailies", valores, "id = ?", arrayOf(daily.id.toString()))
    }

    fun eliminarDaily(id: Int) {
        val db = dbHelper.writableDatabase
        db.delete("dailies", "id = ?", arrayOf(id.toString()))
    }

    // --- ESTADÍSTICAS Y PROCESAMIENTO ---

    fun obtenerTotalHabitosCompletados(correo: String): Int {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT SUM(vecesCompletado) FROM habitos WHERE correo_usuario = ?", arrayOf(correo))
        var total = 0
        if (cursor.moveToFirst()) total = cursor.getInt(0)
        cursor.close()
        return total
    }

    fun obtenerTotalTareasCompletadas(correo: String): Int {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM tareas WHERE correo_usuario = ? AND completada = 1", arrayOf(correo))
        var total = 0
        if (cursor.moveToFirst()) total = cursor.getInt(0)
        cursor.close()
        return total
    }

    fun obtenerTotalDailiesCompletadas(correo: String): Int {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM dailies WHERE correo_usuario = ? AND (ultimaVezCompletada IS NOT NULL AND ultimaVezCompletada != '')", arrayOf(correo))
        var total = 0
        if (cursor.moveToFirst()) total = cursor.getInt(0)
        cursor.close()
        return total
    }

    fun procesarDailiesFallidas(correo: String, mult: Int = 1): Int {
        val dbRead = dbHelper.readableDatabase
        val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val cursor = dbRead.rawQuery("SELECT COUNT(*) FROM dailies WHERE correo_usuario = ? AND (ultimaVezCompletada != ? OR ultimaVezCompletada IS NULL)", arrayOf(correo, hoy))
        var fallidas = 0
        if (cursor.moveToFirst()) fallidas = cursor.getInt(0)
        cursor.close()

        if (fallidas > 0) {
            val danio = fallidas * 5 * mult
            dbHelper.usuarioDao.actualizarProgresoUsuario(correo, 0, 0, -danio)
            return danio
        }
        return 0
    }
}
