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
        val db = dbHelper.writableDatabase // Cambiado a writable para la corrección
        
        // CORRECCIÓN DE ICONOS ANTIGUOS
        db.execSQL("UPDATE tienda_rpg SET icono = 'espada_madera' WHERE icono = 'wooden-sword'")
        db.execSQL("UPDATE tienda_rpg SET icono = 'escudo_carton' WHERE icono = 'round-shield'")
        db.execSQL("UPDATE tienda_rpg SET icono = 'gafas_estudioso' WHERE icono = 'spectacles'")

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
        val db = dbHelper.writableDatabase // Cambiado a writable para la corrección
        
        // CORRECCIÓN DE ICONOS EN INVENTARIO
        db.execSQL("UPDATE inventario SET icono = 'espada_madera' WHERE icono = 'wooden-sword' AND correo_usuario = ?", arrayOf(correo))
        db.execSQL("UPDATE inventario SET icono = 'escudo_carton' WHERE icono = 'round-shield' AND correo_usuario = ?", arrayOf(correo))
        db.execSQL("UPDATE inventario SET icono = 'gafas_estudioso' WHERE icono = 'spectacles' AND correo_usuario = ?", arrayOf(correo))

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
        val db = dbHelper.database
        db.beginTransaction()
        try {
            // CORRECCIÓN FORZADA DE NOMBRE E ICONO:
            db.execSQL("UPDATE jefes SET nombre = 'Dragón Pereza (Nivel 1)', icono = 'dragon_pereza' WHERE (nombre = 'Dragón Pere' OR nombre = 'Dragón Pereza' OR icono = 'ic_boss_dragon') AND nivel = 1")

            val cursor = db.rawQuery("SELECT * FROM jefes WHERE correo_usuario = ? AND derrotado = 0 LIMIT 1", arrayOf(correo))
            
            if (cursor.moveToFirst()) {
                val jefe = Jefe(
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
                cursor.close()
                db.setTransactionSuccessful()
                return jefe
            }
            cursor.close()

            // Si no hay jefe activo, buscamos el último derrotado para ver si debe reaparecer
            val cursorMuerto = db.rawQuery("SELECT * FROM jefes WHERE correo_usuario = ? AND derrotado = 1 ORDER BY fechaMuerte DESC LIMIT 1", arrayOf(correo))
            if (cursorMuerto.moveToFirst()) {
                val fechaMuerte = cursorMuerto.getLong(cursorMuerto.getColumnIndexOrThrow("fechaMuerte"))
                val idJefe = cursorMuerto.getInt(cursorMuerto.getColumnIndexOrThrow("id"))
                val nivelActual = cursorMuerto.getInt(cursorMuerto.getColumnIndexOrThrow("nivel"))
                
                val tiempoTranscurrido = System.currentTimeMillis() - fechaMuerte
                val tiempoEspera = 21 * 60 * 60 * 1000L

                if (tiempoTranscurrido >= tiempoEspera) {
                    val nuevoNivel = nivelActual + 1
                    val nuevoHpMax = 500 + (nuevoNivel - 1) * 250
                    
                    val v = ContentValues().apply {
                        put("nombre", "Dragón Pereza (Nivel $nuevoNivel)")
                        put("derrotado", 0)
                        put("hpMax", nuevoHpMax)
                        put("hpActual", nuevoHpMax)
                        put("fechaMuerte", 0)
                        put("nivel", nuevoNivel)
                        put("recompensaMonedas", 150 + (nuevoNivel * 50))
                        put("recompensaXP", 250 + (nuevoNivel * 75))
                        put("icono", "dragon_pereza")
                    }
                    db.update("jefes", v, "id = ?", arrayOf(idJefe.toString()))
                    db.setTransactionSuccessful()
                    cursorMuerto.close()
                    db.endTransaction() 
                    return obtenerJefeActivo(correo)
                } else {
                    // EL JEFE ESTÁ MUERTO Y AÚN NO HA PASADO EL TIEMPO
                    cursorMuerto.close()
                    db.setTransactionSuccessful()
                    return null
                }
            } else {
                // NO HAY NINGÚN JEFE EN LA HISTORIA (NI VIVO NI MUERTO)
                val vInicial = ContentValues().apply {
                    put("correo_usuario", correo)
                    put("nombre", "Dragón Pereza (Nivel 1)")
                    put("descripcion", "El guardián de las tareas pendientes. ¡Derrótalo para ganar XP!")
                    put("hpMax", 500)
                    put("hpActual", 500)
                    put("recompensaMonedas", 150)
                    put("recompensaXP", 250)
                    put("icono", "dragon_pereza")
                    put("derrotado", 0)
                    put("nivel", 1)
                    put("armadura", 0)
                }
                db.insert("jefes", null, vInicial)
                db.setTransactionSuccessful()
                db.endTransaction()
                return obtenerJefeActivo(correo)
            }
        } finally {
            if (db.inTransaction()) db.endTransaction()
        }
    }

    fun atacarJefe(danioBase: Int, correo: String): Boolean {
        val db = dbHelper.database
        db.beginTransaction()
        try {
            val jefe = obtenerJefeActivo(correo) ?: return false
            val usuario = dbHelper.usuarioDao.obtenerUsuarioLogueado(correo) ?: return false
            val inventario = obtenerInventario(correo).filter { it.equipado }
            
            val bonusFzaEquipo = inventario.sumOf { it.bonusFza }.toDouble()
            val fuerzaTotal = usuario.fuerza + (bonusFzaEquipo / 10.0)

            val danioTrasArmadura = if (danioBase > 0) (danioBase - jefe.armadura).coerceAtLeast(1) else danioBase
            val danioReal = (danioTrasArmadura * fuerzaTotal).toInt()
            val nuevoHp = (jefe.hpActual - danioReal).coerceIn(0, jefe.hpMax)

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
            }
            
            db.setTransactionSuccessful()
            return nuevoHp == 0
        } finally {
            db.endTransaction()
        }
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
