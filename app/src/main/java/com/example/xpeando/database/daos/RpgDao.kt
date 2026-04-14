package com.example.xpeando.database.daos

import android.content.ContentValues
import com.example.xpeando.database.DBHelper
import com.example.xpeando.model.Articulo
import com.example.xpeando.model.Jefe
import com.example.xpeando.model.Recompensa

class RpgDao(private val dbHelper: DBHelper) {

    // --- TIENDA Y ARTÍCULOS ---
    fun obtenerTiendaRPG(): List<Articulo> {
        val lista = mutableListOf<Articulo>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM tienda_rpg", null)
        if (cursor.moveToFirst()) {
            do {
                lista.add(Articulo(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                    tipo = cursor.getString(cursor.getColumnIndexOrThrow("tipo")),
                    subtipo = cursor.getString(cursor.getColumnIndexOrThrow("subtipo")),
                    precio = cursor.getInt(cursor.getColumnIndexOrThrow("precio")),
                    bonusFza = cursor.getInt(cursor.getColumnIndexOrThrow("bonusFza")),
                    bonusInt = cursor.getInt(cursor.getColumnIndexOrThrow("bonusInt")),
                    bonusCon = cursor.getInt(cursor.getColumnIndexOrThrow("bonusCon")),
                    bonusPer = cursor.getInt(cursor.getColumnIndexOrThrow("bonusPer")),
                    bonusHp = cursor.getInt(cursor.getColumnIndexOrThrow("bonusHp")),
                    icono = cursor.getString(cursor.getColumnIndexOrThrow("icono")),
                    equipado = false,
                    esPropio = false
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return lista
    }

    fun comprarArticulo(correo: String, articulo: Articulo): Boolean {
        val db = dbHelper.writableDatabase
        
        if (articulo.tipo == "CONSUMIBLE") {
            val cursor = db.rawQuery(
                "SELECT id, cantidad FROM inventario WHERE correo_usuario = ? AND nombre = ? AND tipo = 'CONSUMIBLE'",
                arrayOf(correo, articulo.nombre)
            )
            if (cursor.moveToFirst()) {
                val idExistente = cursor.getInt(0)
                val cantidadActual = cursor.getInt(1)
                db.execSQL("UPDATE inventario SET cantidad = ? WHERE id = ?", arrayOf(cantidadActual + 1, idExistente))
                cursor.close()
                return true
            }
            cursor.close()
        }

        val valores = ContentValues().apply {
            put("correo_usuario", correo)
            put("nombre", articulo.nombre)
            put("tipo", articulo.tipo)
            put("subtipo", articulo.subtipo)
            put("bonusFza", articulo.bonusFza)
            put("bonusInt", articulo.bonusInt)
            put("bonusCon", articulo.bonusCon)
            put("bonusPer", articulo.bonusPer)
            put("bonusHp", articulo.bonusHp)
            put("icono", articulo.icono)
            put("equipado", 0)
            put("cantidad", 1)
        }
        db.insert("inventario", null, valores)
        return true
    }

    fun obtenerInventario(correo: String): List<Articulo> {
        val lista = mutableListOf<Articulo>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM inventario WHERE correo_usuario = ?", arrayOf(correo))
        if (cursor.moveToFirst()) {
            do {
                lista.add(Articulo(
                    cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                    cursor.getString(cursor.getColumnIndexOrThrow("tipo")),
                    cursor.getString(cursor.getColumnIndexOrThrow("subtipo")),
                    0,
                    cursor.getInt(cursor.getColumnIndexOrThrow("bonusFza")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("bonusInt")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("bonusCon")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("bonusPer")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("bonusHp")),
                    cursor.getString(cursor.getColumnIndexOrThrow("icono")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("equipado")) == 1,
                    true,
                    cursor.getInt(cursor.getColumnIndexOrThrow("cantidad"))
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return lista
    }

    fun equiparDesequipar(correo: String, idArticulo: Int) {
        val db = dbHelper.writableDatabase
        val cursor = db.rawQuery("SELECT subtipo, equipado FROM inventario WHERE id = ?", arrayOf(idArticulo.toString()))
        if (cursor.moveToFirst()) {
            val subtipo = cursor.getString(0)
            val actualmenteEquipado = cursor.getInt(1) == 1
            if (actualmenteEquipado) {
                db.execSQL("UPDATE inventario SET equipado = 0 WHERE id = ?", arrayOf(idArticulo))
            } else {
                db.execSQL("UPDATE inventario SET equipado = 0 WHERE correo_usuario = ? AND subtipo = ?", arrayOf(correo, subtipo))
                db.execSQL("UPDATE inventario SET equipado = 1 WHERE id = ?", arrayOf(idArticulo))
            }
        }
        cursor.close()
    }

    fun eliminarDelInventario(id: Int) {
        val db = dbHelper.writableDatabase
        db.delete("inventario", "id = ?", arrayOf(id.toString()))
    }

    // --- RECOMPENSAS ---
    fun insertarRecompensa(recompensa: Recompensa): Long {
        val db = dbHelper.writableDatabase
        val valores = ContentValues().apply {
            put("correo_usuario", recompensa.correo_usuario)
            put("nombre", recompensa.nombre)
            put("precio", recompensa.precio)
            put("icono", recompensa.icono)
        }
        val id = db.insert("recompensas", null, valores)
        return id
    }

    fun obtenerTodasRecompensas(correo: String): List<Recompensa> {
        val lista = mutableListOf<Recompensa>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM recompensas WHERE correo_usuario = ? OR correo_usuario IS NULL", arrayOf(correo))
        if (cursor.moveToFirst()) {
            do {
                lista.add(Recompensa(
                    cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("correo_usuario")) ?: "",
                    cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("precio")),
                    cursor.getString(cursor.getColumnIndexOrThrow("icono"))
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return lista
    }

    fun eliminarRecompensa(id: Int) {
        val db = dbHelper.writableDatabase
        db.delete("recompensas", "id = ?", arrayOf(id.toString()))
    }

    fun obtenerJefeActivo(correo: String): Jefe? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM jefes WHERE correo_usuario = ? AND derrotado = 0 LIMIT 1", arrayOf(correo))
        var jefe: Jefe? = null
        if (cursor.moveToFirst()) {
            jefe = Jefe(
                cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                cursor.getString(cursor.getColumnIndexOrThrow("descripcion")),
                cursor.getInt(cursor.getColumnIndexOrThrow("hpMax")),
                cursor.getInt(cursor.getColumnIndexOrThrow("hpActual")),
                cursor.getInt(cursor.getColumnIndexOrThrow("recompensaMonedas")),
                cursor.getInt(cursor.getColumnIndexOrThrow("recompensaXP")),
                cursor.getString(cursor.getColumnIndexOrThrow("icono")),
                cursor.getInt(cursor.getColumnIndexOrThrow("derrotado")) == 1,
                cursor.getInt(cursor.getColumnIndexOrThrow("nivel")),
                cursor.getInt(cursor.getColumnIndexOrThrow("armadura"))
            )
        } else {
            val cursorMuerto = db.rawQuery("SELECT * FROM jefes WHERE correo_usuario = ? AND derrotado = 1 ORDER BY fechaMuerte DESC LIMIT 1", arrayOf(correo))
            if (cursorMuerto.moveToFirst()) {
                val fechaMuerte = cursorMuerto.getLong(cursorMuerto.getColumnIndexOrThrow("fechaMuerte"))
                if (System.currentTimeMillis() - fechaMuerte >= 21 * 60 * 60 * 1000) {
                    val idJefe = cursorMuerto.getInt(cursorMuerto.getColumnIndexOrThrow("id"))
                    val nivelAnterior = cursorMuerto.getInt(cursorMuerto.getColumnIndexOrThrow("nivel"))
                    val nuevoNivel = nivelAnterior + 1
                    val nuevoHpMax = 200 + (nuevoNivel - 1) * 50
                    val dbWrite = dbHelper.writableDatabase
                    val v = ContentValues().apply {
                        put("nombre", "Dragón Pere (Nivel $nuevoNivel)")
                        put("derrotado", 0)
                        put("hpMax", nuevoHpMax)
                        put("hpActual", nuevoHpMax)
                        put("fechaMuerte", 0)
                        put("nivel", nuevoNivel)
                    }
                    dbWrite.update("jefes", v, "id = ?", arrayOf(idJefe.toString()))
                    cursorMuerto.close()
                    return obtenerJefeActivo(correo)
                }
            }
            cursorMuerto.close()
        }
        cursor.close()
        return jefe
    }

    fun atacarJefe(danioBase: Int, correo: String): Boolean {
        val jefe = obtenerJefeActivo(correo) ?: return false
        val usuario = dbHelper.usuarioDao.obtenerUsuarioLogueado(correo) ?: return false
        val inventario = obtenerInventario(correo).filter { it.equipado }
        
        val bonusFzaEquipo = inventario.sumOf { it.bonusFza }.toDouble()
        val fuerzaTotal = usuario.fuerza + (bonusFzaEquipo / 10.0)

        val danioTrasArmadura = if (danioBase > 0) (danioBase - jefe.armadura).coerceAtLeast(1) else danioBase
        val danioReal = (danioTrasArmadura * fuerzaTotal).toInt()
        val nuevoHp = (jefe.hpActual - danioReal).coerceIn(0, jefe.hpMax)

        val db = dbHelper.writableDatabase
        val valores = ContentValues().apply {
            put("hpActual", nuevoHp)
            if (nuevoHp == 0) {
                put("derrotado", 1)
                put("fechaMuerte", System.currentTimeMillis())
            }
        }
        db.update("jefes", valores, "id = ?", arrayOf(jefe.id.toString()))

        if (nuevoHp == 0) {
            dbHelper.usuarioDao.actualizarProgresoUsuario(correo, jefe.recompensaXP, jefe.recompensaMonedas)
            return true
        }
        return false
    }

    fun obtenerJefesDerrotados(correo: String): List<Jefe> {
        val lista = mutableListOf<Jefe>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM jefes WHERE correo_usuario = ? AND derrotado = 1 ORDER BY fechaMuerte DESC", arrayOf(correo))
        if (cursor.moveToFirst()) {
            do {
                lista.add(Jefe(
                    cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                    cursor.getString(cursor.getColumnIndexOrThrow("descripcion")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("hpMax")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("hpActual")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("recompensaMonedas")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("recompensaXP")),
                    cursor.getString(cursor.getColumnIndexOrThrow("icono")),
                    true,
                    cursor.getInt(cursor.getColumnIndexOrThrow("nivel")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("armadura")),
                    fechaMuerte = cursor.getLong(cursor.getColumnIndexOrThrow("fechaMuerte"))
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return lista
    }

    fun obtenerUltimoJefeDerrotadoTime(correo: String): Long {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT fechaMuerte FROM jefes WHERE correo_usuario = ? AND derrotado = 1 ORDER BY fechaMuerte DESC LIMIT 1", arrayOf(correo))
        var time = 0L
        if (cursor.moveToFirst()) {
            time = cursor.getLong(0)
        }
        cursor.close()
        return time
    }
}
