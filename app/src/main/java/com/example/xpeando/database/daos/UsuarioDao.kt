package com.example.xpeando.database.daos

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.example.xpeando.database.DBHelper
import com.example.xpeando.model.Usuario
import com.example.xpeando.model.Progreso
import com.example.xpeando.model.Articulo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

class UsuarioDao(private val dbHelper: DBHelper) {

    fun registrarUsuario(usuario: Usuario): Long {
        val db = dbHelper.database
        var id: Long = -1
        db.beginTransaction()
        try {
            val valores = ContentValues().apply {
                put("nombre", usuario.nombre)
                put("correo", usuario.correo)
                put("contrasena", usuario.contrasena)
                put("nivel", usuario.nivel)
                put("experiencia", usuario.experiencia)
                put("monedas", usuario.monedas)
                put("hp", usuario.hp)
                put("fuerza", usuario.fuerza)
                put("inteligencia", usuario.inteligencia)
                put("constitucion", usuario.constitucion)
                put("percepcion", usuario.percepcion)
                put("puntosDisponibles", usuario.puntosDisponibles)
                put("rachaActual", 0)
                put("rachaMaxima", 0)
                put("ultimaFechaActividad", "")
            }
            id = db.insert("usuarios", null, valores)
            
            // Inicializar jefe para el nuevo usuario corregido
            val vJefe = ContentValues().apply {
                put("correo_usuario", usuario.correo)
                put("nombre", "Dragón Pereza (Nivel 1)")
                put("descripcion", "El guardián de las tareas pendientes. ¡Derrótalo para ganar XP!")
                put("hpMax", 500)
                put("hpActual", 500)
                put("recompensaMonedas", 150)
                put("recompensaXP", 250)
                put("icono", "dragon_pereza")
                put("derrotado", 0)
                put("fechaMuerte", 0L)
                put("nivel", 1)
                put("armadura", 0)
            }
            db.insert("jefes", null, vJefe)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return id
    }

    fun validarUsuario(correo: String, contrasena: String): Boolean {
        val db = dbHelper.database
        val cursor = db.rawQuery("SELECT id FROM usuarios WHERE correo = ? AND contrasena = ?", arrayOf(correo, contrasena))
        val existe = cursor.count > 0
        cursor.close()
        return existe
    }

    fun obtenerUsuarioLogueado(correo: String): Usuario? {
        val db = dbHelper.database
        val cursor = db.rawQuery("SELECT * FROM usuarios WHERE correo = ?", arrayOf(correo))
        var usuario: Usuario? = null
        if (cursor.moveToFirst()) {
            usuario = Usuario(
                cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                cursor.getString(cursor.getColumnIndexOrThrow("correo")),
                cursor.getString(cursor.getColumnIndexOrThrow("contrasena")),
                cursor.getInt(cursor.getColumnIndexOrThrow("nivel")),
                cursor.getInt(cursor.getColumnIndexOrThrow("experiencia")),
                cursor.getInt(cursor.getColumnIndexOrThrow("monedas")),
                cursor.getInt(cursor.getColumnIndexOrThrow("hp")),
                cursor.getDouble(cursor.getColumnIndexOrThrow("fuerza")),
                cursor.getDouble(cursor.getColumnIndexOrThrow("inteligencia")),
                cursor.getDouble(cursor.getColumnIndexOrThrow("constitucion")),
                cursor.getDouble(cursor.getColumnIndexOrThrow("percepcion")),
                cursor.getInt(cursor.getColumnIndexOrThrow("puntosDisponibles")),
                cursor.getInt(cursor.getColumnIndexOrThrow("totalHabitos")),
                cursor.getInt(cursor.getColumnIndexOrThrow("rachaActual")),
                cursor.getInt(cursor.getColumnIndexOrThrow("rachaMaxima")),
                cursor.getString(cursor.getColumnIndexOrThrow("ultimaFechaActividad"))
            )
        }
        cursor.close()
        return usuario
    }

    fun actualizarProgresoUsuario(correo: String, xpBase: Int, monedasBase: Int, hpCambioBase: Int = 0) {
        val db = dbHelper.database
        db.beginTransaction()
        try {
            val usuario = obtenerUsuarioLogueado(correo) ?: return
            
            val inventario = mutableListOf<Articulo>()
            val cursorInv = db.rawQuery("SELECT * FROM inventario WHERE correo_usuario = ? AND equipado = 1", arrayOf(correo))
            if (cursorInv.moveToFirst()) {
                do {
                    inventario.add(Articulo(
                        id = cursorInv.getInt(cursorInv.getColumnIndexOrThrow("id")),
                        nombre = cursorInv.getString(cursorInv.getColumnIndexOrThrow("nombre")),
                        tipo = cursorInv.getString(cursorInv.getColumnIndexOrThrow("tipo")),
                        subtipo = cursorInv.getString(cursorInv.getColumnIndexOrThrow("subtipo")),
                        precio = 0,
                        bonusFza = cursorInv.getInt(cursorInv.getColumnIndexOrThrow("bonusFza")),
                        bonusInt = cursorInv.getInt(cursorInv.getColumnIndexOrThrow("bonusInt")),
                        bonusCon = cursorInv.getInt(cursorInv.getColumnIndexOrThrow("bonusCon")),
                        bonusPer = cursorInv.getInt(cursorInv.getColumnIndexOrThrow("bonusPer")),
                        bonusHp = cursorInv.getInt(cursorInv.getColumnIndexOrThrow("bonusHp")),
                        icono = cursorInv.getString(cursorInv.getColumnIndexOrThrow("icono")),
                        equipado = true
                    ))
                } while (cursorInv.moveToNext())
            }
            cursorInv.close()

            val intTotal = usuario.inteligencia + (inventario.sumOf { it.bonusInt } / 10.0)
            val perTotal = usuario.percepcion + (inventario.sumOf { it.bonusPer } / 10.0)
            val conTotal = usuario.constitucion + (inventario.sumOf { it.bonusCon } / 10.0)

            val bonusRacha = when {
                usuario.rachaActual >= 7 -> 1.25
                usuario.rachaActual >= 3 -> 1.10
                else -> 1.0
            }

            val xpFinal = if (xpBase > 0) (xpBase * intTotal * bonusRacha).toInt() else xpBase
            val monedasFinal = if (monedasBase > 0) (monedasBase * perTotal * bonusRacha).toInt() else monedasBase
            val hpFinalCambio = if (hpCambioBase < 0) (hpCambioBase / conTotal).toInt() else hpCambioBase

            var nuevaXp = usuario.experiencia + xpFinal
            var nuevoNivel = usuario.nivel
            var nuevasMonedas = usuario.monedas + monedasFinal
            var nuevoHp = usuario.hp + hpFinalCambio
            var nuevosPuntos = usuario.puntosDisponibles

            // CÁLCULO DE NIVEL DINÁMICO (Cada nivel pide más XP)
            var xpParaSiguienteNivel = nuevoNivel * 100 
            while (nuevaXp >= xpParaSiguienteNivel) {
                nuevaXp -= xpParaSiguienteNivel
                nuevoNivel++
                xpParaSiguienteNivel = nuevoNivel * 100
                nuevoHp = 50 
                nuevosPuntos += 3
            }
            
            nuevaXp = nuevaXp.coerceAtLeast(0)
            nuevasMonedas = nuevasMonedas.coerceAtLeast(0)
            nuevoHp = nuevoHp.coerceIn(0, 50)

            val valores = ContentValues().apply {
                put("nivel", nuevoNivel)
                put("experiencia", nuevaXp)
                put("monedas", nuevasMonedas)
                put("hp", nuevoHp)
                put("puntosDisponibles", nuevosPuntos)
                if (xpBase != 0 || hpCambioBase != 0) {
                    put("totalHabitos", usuario.totalHabitos + 1)
                }
            }
            db.update("usuarios", valores, "correo = ?", arrayOf(correo))

            if (xpBase != 0 || monedasBase != 0 || hpCambioBase != 0) {
                val tipo = when {
                    xpBase > 0 -> "GANANCIA"
                    hpCambioBase < 0 -> "PENALIZACION"
                    else -> "ACCION"
                }
                registrarHistorial(correo, xpFinal, monedasFinal, tipo)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun registrarHistorial(correo: String, xp: Int, monedas: Int, tipo: String): Long {
        val db = dbHelper.database
        val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val valores = ContentValues().apply {
            put("correo_usuario", correo)
            put("fecha", hoy)
            put("xp", xp)
            put("monedas", monedas)
            put("tipo", tipo)
        }
        return db.insert("historial_progreso", null, valores)
    }

    fun obtenerHistorialCompleto(correo: String): List<Progreso> {
        val lista = mutableListOf<Progreso>()
        val db = dbHelper.database
        val cursor = db.rawQuery("SELECT * FROM historial_progreso WHERE correo_usuario = ?", arrayOf(correo))
        if (cursor.moveToFirst()) {
            do {
                lista.add(Progreso(
                    cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("correo_usuario")),
                    cursor.getString(cursor.getColumnIndexOrThrow("fecha")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("xp")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("monedas")),
                    cursor.getString(cursor.getColumnIndexOrThrow("tipo"))
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return lista
    }

    fun obtenerXPSemanal(correo: String): Map<String, Int> {
        val mapa = mutableMapOf<String, Int>()
        val db = dbHelper.database
        val cursor = db.rawQuery("""
            SELECT fecha, SUM(xp) as total_xp 
            FROM historial_progreso 
            WHERE correo_usuario = ? 
            GROUP BY fecha 
            ORDER BY fecha DESC 
            LIMIT 7
        """.trimIndent(), arrayOf(correo))
        
        if (cursor.moveToFirst()) {
            do {
                val fecha = cursor.getString(cursor.getColumnIndexOrThrow("fecha"))
                val total = cursor.getInt(cursor.getColumnIndexOrThrow("total_xp"))
                mapa[fecha] = total
            } while (cursor.moveToNext())
        }
        cursor.close()
        return mapa
    }

    fun actualizarRacha(correo: String) {
        val usuario = obtenerUsuarioLogueado(correo) ?: return
        val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        if (usuario.ultimaFechaActividad == hoy) return

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        cal.add(Calendar.DATE, -1)
        val ayer = sdf.format(cal.time)

        val nuevaRacha = if (usuario.ultimaFechaActividad == ayer) usuario.rachaActual + 1 else 1
        val nuevaRachaMaxima = if (nuevaRacha > usuario.rachaMaxima) nuevaRacha else usuario.rachaMaxima

        val db = dbHelper.database
        val valores = ContentValues().apply {
            put("rachaActual", nuevaRacha)
            put("rachaMaxima", nuevaRachaMaxima)
            put("ultimaFechaActividad", hoy)
        }
        db.update("usuarios", valores, "correo = ?", arrayOf(correo))
    }

    fun actualizarAtributos(correo: String, fza: Double = 0.0, int: Double = 0.0, con: Double = 0.0, per: Double = 0.0, puntosUsados: Int = 0) {
        val usuario = obtenerUsuarioLogueado(correo) ?: return
        val db = dbHelper.database
        val nuevosPuntos = (usuario.puntosDisponibles - puntosUsados).coerceAtLeast(0)
        val valores = ContentValues().apply {
            put("fuerza", usuario.fuerza + fza)
            put("inteligencia", usuario.inteligencia + int)
            put("constitucion", usuario.constitucion + con)
            put("percepcion", usuario.percepcion + per)
            put("puntosDisponibles", nuevosPuntos)
        }
        db.update("usuarios", valores, "correo = ?", arrayOf(correo))
    }

    fun upsertUsuario(usuario: Usuario) {
        val db = dbHelper.writableDatabase
        val valores = ContentValues().apply {
            put("nombre", usuario.nombre)
            put("correo", usuario.correo)
            put("contrasena", usuario.contrasena)
            put("nivel", usuario.nivel)
            put("experiencia", usuario.experiencia)
            put("monedas", usuario.monedas)
            put("hp", usuario.hp)
            put("fuerza", usuario.fuerza)
            put("inteligencia", usuario.inteligencia)
            put("constitucion", usuario.constitucion)
            put("percepcion", usuario.percepcion)
            put("puntosDisponibles", usuario.puntosDisponibles)
            put("totalHabitos", usuario.totalHabitos)
            put("rachaActual", usuario.rachaActual)
            put("rachaMaxima", usuario.rachaMaxima)
            put("ultimaFechaActividad", usuario.ultimaFechaActividad)
        }
        val rows = db.update("usuarios", valores, "correo = ?", arrayOf(usuario.correo))
        if (rows == 0) {
            db.insert("usuarios", null, valores)
        }
    }
}
