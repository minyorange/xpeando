package com.example.xpeando.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.xpeando.model.Habito
import com.example.xpeando.model.Recompensa
import com.example.xpeando.model.Tarea
import com.example.xpeando.model.Usuario
import com.example.xpeando.model.Daily
import com.example.xpeando.model.Articulo
import com.example.xpeando.model.Jefe
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DBHelper(context: Context) : SQLiteOpenHelper(context, "xpeando_db", null, 20) { // Incrementamos para añadir contador de hábitos

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE habitos (id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT, experiencia INTEGER DEFAULT 10, monedas INTEGER DEFAULT 5, completadoHoy INTEGER DEFAULT 0, atributo TEXT DEFAULT 'Fuerza')")
        db.execSQL("CREATE TABLE tareas (id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT, dificultad INTEGER DEFAULT 1, experiencia INTEGER DEFAULT 20, monedas INTEGER DEFAULT 10, completada INTEGER DEFAULT 0)")
        db.execSQL("CREATE TABLE usuarios (id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT, correo TEXT UNIQUE, contrasena TEXT, nivel INTEGER DEFAULT 1, experiencia INTEGER DEFAULT 0, monedas INTEGER DEFAULT 0, hp INTEGER DEFAULT 50, fuerza REAL DEFAULT 1.0, inteligencia REAL DEFAULT 1.0, constitucion REAL DEFAULT 1.0, percepcion REAL DEFAULT 1.0, puntosDisponibles INTEGER DEFAULT 0, totalHabitos INTEGER DEFAULT 0)")
        db.execSQL("CREATE TABLE recompensas (id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT, precio INTEGER, icono TEXT)")
        db.execSQL("CREATE TABLE dailies (id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT, experiencia INTEGER DEFAULT 15, monedas INTEGER DEFAULT 10, completadaHoy INTEGER DEFAULT 0, ultimaVezCompletada TEXT)")
        
        db.execSQL("""
            CREATE TABLE inventario (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                correo_usuario TEXT,
                nombre TEXT,
                tipo TEXT,
                subtipo TEXT,
                bonusFza INTEGER,
                bonusInt INTEGER,
                bonusCon INTEGER,
                bonusPer INTEGER,
                bonusHp INTEGER,
                icono TEXT,
                equipado INTEGER DEFAULT 0
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE tienda_rpg (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT,
                tipo TEXT DEFAULT 'EQUIPO',
                subtipo TEXT,
                precio INTEGER,
                bonusFza INTEGER DEFAULT 0,
                bonusInt INTEGER DEFAULT 0,
                bonusCon INTEGER DEFAULT 0,
                bonusPer INTEGER DEFAULT 0,
                bonusHp INTEGER DEFAULT 0,
                icono TEXT
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE jefes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT,
                hpMax INTEGER,
                hpActual INTEGER,
                recompensaMonedas INTEGER,
                recompensaXP INTEGER,
                icono TEXT,
                derrotado INTEGER DEFAULT 0
            )
        """.trimIndent())

        db.execSQL("CREATE TABLE logros_usuario (id INTEGER PRIMARY KEY AUTOINCREMENT, correo TEXT, nombre_logro TEXT, fecha TEXT)")

        insertarObjetosIniciales(db)
    }

    private fun insertarObjetosIniciales(db: SQLiteDatabase) {
        val items = listOf(
            arrayOf("Espada de Madera", "EQUIPO", "ARMA", "100", "2", "0", "0", "0", "0", "wooden-sword"),
            arrayOf("Escudo de Cartón", "EQUIPO", "ARMADURA", "120", "0", "0", "3", "0", "0", "round-shield"),
            arrayOf("Gafas de Estudioso", "EQUIPO", "ARMA", "150", "0", "4", "0", "0", "0", "spectacles"),
            arrayOf("Poción de Salud", "CONSUMIBLE", "POCION", "50", "0", "0", "0", "0", "20", "health-potion")
        )
        for (item in items) {
            val v = ContentValues().apply {
                put("nombre", item[0])
                put("tipo", item[1])
                put("subtipo", item[2])
                put("precio", item[3].toInt())
                put("bonusFza", item[4].toInt())
                put("bonusInt", item[5].toInt())
                put("bonusCon", item[6].toInt())
                put("bonusPer", item[7].toInt())
                put("bonusHp", item[8].toInt())
                put("icono", item[9])
            }
            db.insert("tienda_rpg", null, v)
        }

        val recompensasDefault = listOf(
            arrayOf("Cena Especial", "50", "chicken-leg"),
            arrayOf("Ver Película", "30", "clapperboard"),
            arrayOf("Dormir Siesta", "20", "bed")
        )
        for (r in recompensasDefault) {
            val v = ContentValues().apply {
                put("nombre", r[0])
                put("precio", r[1].toInt())
                put("icono", r[2])
            }
            db.insert("recompensas", null, v)
        }

        // Insertar Jefe Inicial
        val vJefe = ContentValues().apply {
            put("nombre", "Dragón de la Procrastinación")
            put("hpMax", 200)
            put("hpActual", 200)
            put("recompensaMonedas", 500)
            put("recompensaXP", 150)
            put("icono", "dragon")
            put("derrotado", 0)
        }
        db.insert("jefes", null, vJefe)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS habitos")
        db.execSQL("DROP TABLE IF EXISTS tareas")
        db.execSQL("DROP TABLE IF EXISTS usuarios")
        db.execSQL("DROP TABLE IF EXISTS recompensas")
        db.execSQL("DROP TABLE IF EXISTS dailies")
        db.execSQL("DROP TABLE IF EXISTS inventario")
        db.execSQL("DROP TABLE IF EXISTS tienda_rpg")
        db.execSQL("DROP TABLE IF EXISTS jefes")
        db.execSQL("DROP TABLE IF EXISTS logros_usuario")
        onCreate(db)
    }

    // --- MÉTODOS PARA JEFES ---

    fun obtenerJefeActivo(): Jefe? {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM jefes WHERE derrotado = 0 LIMIT 1", null)
        var jefe: Jefe? = null
        if (cursor.moveToFirst()) {
            jefe = Jefe(
                cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                cursor.getInt(cursor.getColumnIndexOrThrow("hpMax")),
                cursor.getInt(cursor.getColumnIndexOrThrow("hpActual")),
                cursor.getInt(cursor.getColumnIndexOrThrow("recompensaMonedas")),
                cursor.getInt(cursor.getColumnIndexOrThrow("recompensaXP")),
                cursor.getString(cursor.getColumnIndexOrThrow("icono")),
                cursor.getInt(cursor.getColumnIndexOrThrow("derrotado")) == 1
            )
        }
        cursor.close()
        db.close()
        return jefe
    }

    fun dañarJefe(dañoBase: Int, correo: String): Boolean {
        val jefe = obtenerJefeActivo() ?: return false
        val usuario = obtenerUsuarioLogueado(correo) ?: return false
        
        // El daño real depende de la Fuerza del usuario
        val dañoReal = (dañoBase * usuario.fuerza).toInt()
        val nuevoHp = (jefe.hpActual - dañoReal).coerceAtLeast(0)

        val db = this.writableDatabase
        val valores = ContentValues().apply {
            put("hpActual", nuevoHp)
            if (nuevoHp == 0) {
                put("derrotado", 1)
            }
        }
        db.update("jefes", valores, "id = ?", arrayOf(jefe.id.toString()))
        db.close()

        if (nuevoHp == 0) {
            actualizarProgresoUsuario(correo, jefe.recompensaXP, jefe.recompensaMonedas)
            return true // Jefe derrotado
        }
        return false
    }

    // --- MÉTODOS PARA USUARIOS ---

    fun registrarUsuario(usuario: Usuario): Long {
        val db = this.writableDatabase
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
        }
        val id = db.insert("usuarios", null, valores)
        db.close()
        return id
    }

    fun validarUsuario(correo: String, contrasena: String): Boolean {
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM usuarios WHERE correo = ? AND contrasena = ?",
            arrayOf(correo, contrasena)
        )
        val existe = cursor.count > 0
        cursor.close()
        db.close()
        return existe
    }

    fun obtenerUsuarioLogueado(correo: String): Usuario? {
        val db = this.readableDatabase
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
                cursor.getInt(cursor.getColumnIndexOrThrow("totalHabitos"))
            )
        }
        cursor.close()
        db.close()
        return usuario
    }

    fun actualizarProgresoUsuario(correo: String, xpBase: Int, monedasBase: Int, hpCambioBase: Int = 0) {
        val usuario = obtenerUsuarioLogueado(correo) ?: return
        
        // --- APLICAR MODIFICADORES DE ATRIBUTOS ---
        
        // Inteligencia potencia la XP ganada (si es positiva)
        val xpFinal = if (xpBase > 0) (xpBase * usuario.inteligencia).toInt() else xpBase
        
        // Percepción potencia las monedas ganadas (si es positiva)
        val monedasFinal = if (monedasBase > 0) (monedasBase * usuario.percepcion).toInt() else monedasBase
        
        // Constitución reduce el daño recibido (si hpCambio es negativo)
        val hpFinalCambio = if (hpCambioBase < 0) {
            (hpCambioBase / usuario.constitucion).toInt() 
        } else {
            hpCambioBase
        }

        var nuevaXp = usuario.experiencia + xpFinal
        var nuevoNivel = usuario.nivel
        var nuevasMonedas = usuario.monedas + monedasFinal
        var nuevoHp = usuario.hp + hpFinalCambio
        var nuevosPuntos = usuario.puntosDisponibles

        // Subida de nivel
        while (nuevaXp >= 100) {
            nuevaXp -= 100
            nuevoNivel++
            nuevoHp = 50 
            nuevosPuntos += 3
        }
        
        if (nuevaXp < 0) nuevaXp = 0
        if (nuevasMonedas < 0) nuevasMonedas = 0
        
        if (nuevoHp > 50) nuevoHp = 50
        if (nuevoHp < 0) nuevoHp = 0

        val db = this.writableDatabase
        val valores = ContentValues().apply {
            put("nivel", nuevoNivel)
            put("experiencia", nuevaXp)
            put("monedas", nuevasMonedas)
            put("hp", nuevoHp)
            put("puntosDisponibles", nuevosPuntos)
            if (xpBase != 0 || hpCambioBase != 0) { // Si hubo acción (aunque sea daño)
                put("totalHabitos", usuario.totalHabitos + 1)
            }
        }
        db.update("usuarios", valores, "correo = ?", arrayOf(correo))
        db.close()
    }

    fun actualizarAtributos(correo: String, fza: Double = 0.0, int: Double = 0.0, con: Double = 0.0, per: Double = 0.0, puntosUsados: Int = 0) {
        val usuario = obtenerUsuarioLogueado(correo) ?: return
        val db = this.writableDatabase
        val valores = ContentValues().apply {
            put("fuerza", usuario.fuerza + fza)
            put("inteligencia", usuario.inteligencia + int)
            put("constitucion", usuario.constitucion + con)
            put("percepcion", usuario.percepcion + per)
            put("puntosDisponibles", usuario.puntosDisponibles - puntosUsados)
        }
        db.update("usuarios", valores, "correo = ?", arrayOf(correo))
        db.close()
    }

    // --- MÉTODOS PARA HÁBITOS ---

    fun insertarHabito(habito: Habito): Long {
        val db = this.writableDatabase
        val valores = ContentValues().apply {
            put("nombre", habito.nombre)
            put("experiencia", habito.experiencia)
            put("monedas", habito.monedas)
            put("completadoHoy", if (habito.completadoHoy) 1 else 0)
            put("atributo", habito.atributo)
        }
        val id = db.insert("habitos", null, valores)
        db.close()
        return id
    }

    fun obtenerTodosHabitos(): List<Habito> {
        val lista = mutableListOf<Habito>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM habitos", null)
        if (cursor.moveToFirst()) {
            do {
                lista.add(Habito(
                    cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("experiencia")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("monedas")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("completadoHoy")) == 1,
                    cursor.getString(cursor.getColumnIndexOrThrow("atributo"))
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return lista
    }

    fun eliminarHabito(id: Int) {
        val db = this.writableDatabase
        db.delete("habitos", "id = ?", arrayOf(id.toString()))
        db.close()
    }

    fun actualizarEstadoHabito(habito: Habito) {
        val db = this.writableDatabase
        val valores = ContentValues().apply {
            put("completadoHoy", if (habito.completadoHoy) 1 else 0)
        }
        db.update("habitos", valores, "id = ?", arrayOf(habito.id.toString()))
        db.close()
    }

    // --- MÉTODOS PARA TAREAS ---

    fun insertarTarea(tarea: Tarea): Long {
        val db = this.writableDatabase
        val valores = ContentValues().apply {
            put("nombre", tarea.nombre)
            put("dificultad", tarea.dificultad)
            put("experiencia", tarea.experiencia)
            put("monedas", tarea.monedas)
            put("completada", if (tarea.completada) 1 else 0)
        }
        val id = db.insert("tareas", null, valores)
        db.close()
        return id
    }

    fun obtenerTodasLasTareas(): List<Tarea> {
        val lista = mutableListOf<Tarea>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM tareas", null)
        if (cursor.moveToFirst()) {
            do {
                lista.add(Tarea(
                    cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("dificultad")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("experiencia")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("monedas")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("completada")) == 1
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return lista
    }

    fun eliminarTarea(id: Int) {
        val db = this.writableDatabase
        db.delete("tareas", "id = ?", arrayOf(id.toString()))
        db.close()
    }

    fun actualizarTarea(tarea: Tarea) {
        val db = this.writableDatabase
        val valores = ContentValues().apply {
            put("nombre", tarea.nombre)
            put("dificultad", tarea.dificultad)
            put("experiencia", tarea.experiencia)
            put("monedas", tarea.monedas)
            put("completada", if (tarea.completada) 1 else 0)
        }
        db.update("tareas", valores, "id = ?", arrayOf(tarea.id.toString()))
        db.close()
    }

    // --- MÉTODOS PARA RECOMPENSAS ---

    fun insertarRecompensa(recompensa: Recompensa): Long {
        val db = this.writableDatabase
        val valores = ContentValues().apply {
            put("nombre", recompensa.nombre)
            put("precio", recompensa.precio)
            put("icono", recompensa.icono)
        }
        val id = db.insert("recompensas", null, valores)
        db.close()
        return id
    }

    fun obtenerTodasRecompensas(): List<Recompensa> {
        val lista = mutableListOf<Recompensa>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM recompensas", null)
        if (cursor.moveToFirst()) {
            do {
                lista.add(Recompensa(
                    cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("precio")),
                    cursor.getString(cursor.getColumnIndexOrThrow("icono"))
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return lista
    }

    fun eliminarRecompensa(id: Int) {
        val db = this.writableDatabase
        db.delete("recompensas", "id = ?", arrayOf(id.toString()))
        db.close()
    }

    // --- MÉTODOS PARA DAILIES ---

    fun insertarDaily(daily: Daily): Long {
        val db = this.writableDatabase
        val valores = ContentValues().apply {
            put("nombre", daily.nombre)
            put("experiencia", daily.experiencia)
            put("monedas", daily.monedas)
            put("completadaHoy", 0)
            put("ultimaVezCompletada", "")
        }
        val id = db.insert("dailies", null, valores)
        db.close()
        return id
    }

    fun obtenerTodasDailies(): List<Daily> {
        val lista = mutableListOf<Daily>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM dailies", null)
        val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        if (cursor.moveToFirst()) {
            do {
                val ultimaVez = cursor.getString(cursor.getColumnIndexOrThrow("ultimaVezCompletada"))
                val completadaHoy = ultimaVez == hoy
                
                lista.add(Daily(
                    cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("experiencia")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("monedas")),
                    completadaHoy,
                    ultimaVez
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return lista
    }

    fun actualizarEstadoDaily(daily: Daily, completada: Boolean) {
        val db = this.writableDatabase
        val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val valores = ContentValues().apply {
            put("completadaHoy", if (completada) 1 else 0)
            put("ultimaVezCompletada", if (completada) hoy else "")
        }
        db.update("dailies", valores, "id = ?", arrayOf(daily.id.toString()))
        db.close()
    }
    
    fun eliminarDaily(id: Int) {
        val db = this.writableDatabase
        db.delete("dailies", "id = ?", arrayOf(id.toString()))
        db.close()
    }

    fun procesarDailiesFallidas(correo: String, multiplicadorDias: Int = 1): Int {
        val dbRead = this.readableDatabase
        val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        val cursor = dbRead.rawQuery("SELECT COUNT(*) FROM dailies WHERE ultimaVezCompletada != ? OR ultimaVezCompletada IS NULL", arrayOf(hoy))
        var fallidas = 0
        if (cursor.moveToFirst()) {
            fallidas = cursor.getInt(0)
        }
        cursor.close()
        dbRead.close()

        if (fallidas > 0) {
            val dañoTotal = fallidas * 5 * multiplicadorDias
            actualizarProgresoUsuario(correo, 0, 0, -dañoTotal)
            return dañoTotal
        }
        return 0
    }

    // --- MÉTODOS PARA INVENTARIO Y TIENDA RPG ---

    fun obtenerTiendaRPG(): List<Articulo> {
        val lista = mutableListOf<Articulo>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM tienda_rpg", null)
        if (cursor.moveToFirst()) {
            do {
                lista.add(Articulo(
                    cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                    cursor.getString(cursor.getColumnIndexOrThrow("tipo")),
                    cursor.getString(cursor.getColumnIndexOrThrow("subtipo")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("precio")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("bonusFza")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("bonusInt")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("bonusCon")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("bonusPer")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("bonusHp")),
                    cursor.getString(cursor.getColumnIndexOrThrow("icono")),
                    false,
                    false
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return lista
    }

    fun comprarArticulo(correo: String, articulo: Articulo): Boolean {
        val usuario = obtenerUsuarioLogueado(correo) ?: return false
        if (usuario.monedas < articulo.precio) return false

        actualizarProgresoUsuario(correo, 0, -articulo.precio)

        val db = this.writableDatabase
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
        }
        db.insert("inventario", null, valores)
        db.close()
        return true
    }

    fun obtenerInventario(correo: String): List<Articulo> {
        val lista = mutableListOf<Articulo>()
        val db = this.readableDatabase
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
                    true
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return lista
    }

    fun equiparDesequipar(correo: String, idArticulo: Int) {
        val db = this.writableDatabase
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
        db.close()
    }

    fun eliminarDelInventario(id: Int) {
        val db = this.writableDatabase
        db.delete("inventario", "id = ?", arrayOf(id.toString()))
        db.close()
    }

    // --- MÉTODOS PARA LOGROS ---

    fun esLogroDesbloqueado(correo: String, nombreLogro: String): Boolean {
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM logros_usuario WHERE correo = ? AND nombre_logro = ?",
            arrayOf(correo, nombreLogro)
        )
        val desbloqueado = cursor.count > 0
        cursor.close()
        db.close()
        return desbloqueado
    }

    fun desbloquearLogro(correo: String, nombreLogro: String) {
        val db = this.writableDatabase
        val hoy = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val valores = ContentValues().apply {
            put("correo", correo)
            put("nombre_logro", nombreLogro)
            put("fecha", hoy)
        }
        db.insert("logros_usuario", null, valores)
        db.close()
    }

    // --- MÉTODOS PARA ESTADÍSTICAS ---

    fun obtenerTotalTareasCompletadas(): Int {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM tareas WHERE completada = 1", null)
        var total = 0
        if (cursor.moveToFirst()) total = cursor.getInt(0)
        cursor.close()
        db.close()
        return total
    }

    fun obtenerTotalDailiesCompletadas(): Int {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM dailies WHERE ultimaVezCompletada != ''", null)
        var total = 0
        if (cursor.moveToFirst()) total = cursor.getInt(0)
        cursor.close()
        db.close()
        return total
    }

    fun obtenerTotalHabitosCompletados(): Int {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM habitos WHERE completadoHoy = 1", null)
        var total = 0
        if (cursor.moveToFirst()) total = cursor.getInt(0)
        cursor.close()
        db.close()
        return total
    }
}
