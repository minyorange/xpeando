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

class DBHelper(context: Context) : SQLiteOpenHelper(context, "xpeando_db", null, 24) { // Subido a 24 para equilibrar atributos

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
                descripcion TEXT,
                hpMax INTEGER,
                hpActual INTEGER,
                recompensaMonedas INTEGER,
                recompensaXP INTEGER,
                icono TEXT,
                derrotado INTEGER DEFAULT 0,
                fechaMuerte LONG DEFAULT 0,
                nivel INTEGER DEFAULT 1,
                armadura INTEGER DEFAULT 0
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
            put("descripcion", "Se alimenta de tus tareas pendientes. ¡Derrótalo antes de que sea demasiado tarde!")
            put("hpMax", 200)
            put("hpActual", 200)
            put("recompensaMonedas", 500)
            put("recompensaXP", 150)
            put("icono", "dragon")
            put("derrotado", 0)
            put("fechaMuerte", 0L)
            put("nivel", 1)
            put("armadura", 0)
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
            // No hay jefe activo, veamos si debe reaparecer (21h = 75600000 ms)
            val cursorMuerto = db.rawQuery("SELECT * FROM jefes WHERE derrotado = 1 ORDER BY fechaMuerte DESC LIMIT 1", null)
            if (cursorMuerto.moveToFirst()) {
                val fechaMuerte = cursorMuerto.getLong(cursorMuerto.getColumnIndexOrThrow("fechaMuerte"))
                val ahora = System.currentTimeMillis()
                if (ahora - fechaMuerte >= 21 * 60 * 60 * 1000) {
                    // Resucitar jefe incrementando dificultad
                    val idJefe = cursorMuerto.getInt(cursorMuerto.getColumnIndexOrThrow("id"))
                    val nivelAnterior = cursorMuerto.getInt(cursorMuerto.getColumnIndexOrThrow("nivel"))
                    val nuevoNivel = nivelAnterior + 1
                    
                    // Incremento: +50 HP por nivel y +5 Armadura por nivel
                    val nuevoHpMax = 200 + (nuevoNivel - 1) * 50
                    val nuevaArmadura = (nuevoNivel - 1) * 5
                    
                    // Escalar recompensas: +10% de oro y XP por cada nivel adicional
                    val nuevaRecompensaMonedas = (500 * (1 + (nuevoNivel - 1) * 0.1)).toInt()
                    val nuevaRecompensaXP = (150 * (1 + (nuevoNivel - 1) * 0.1)).toInt()
                    
                    // Cambiar nombre y descripción según el nivel
                    val (nuevoNombre, nuevaDesc) = when {
                        nuevoNivel == 1 -> Pair("Dragón de la Procrastinación", "Se alimenta de tus tareas pendientes. ¡Derrótalo antes de que sea demasiado tarde!")
                        nuevoNivel == 2 -> Pair("Soberano de la Pereza", "Un ente que ralentiza tus movimientos. Solo la constancia puede vencerlo.")
                        nuevoNivel == 3 -> Pair("Titán del Desorden", "Su mera presencia desorganiza tu mente. Pon orden en tu vida para debilitarlo.")
                        nuevoNivel == 4 -> Pair("Espectro de las Tareas Pendientes", "La acumulación de días sin acción le ha dado una forma física aterradora.")
                        nuevoNivel >= 5 -> Pair("Señor del Caos Infinito (Nvl $nuevoNivel)", "La entidad definitiva. El reto final para un héroe de la productividad.")
                        else -> Pair("Dragón de la Procrastinación", "Se alimenta de tus tareas pendientes.")
                    }
                    
                    val dbWrite = this.writableDatabase
                    val v = ContentValues().apply {
                        put("nombre", nuevoNombre)
                        put("descripcion", nuevaDesc)
                        put("derrotado", 0)
                        put("hpMax", nuevoHpMax)
                        put("hpActual", nuevoHpMax)
                        put("recompensaMonedas", nuevaRecompensaMonedas)
                        put("recompensaXP", nuevaRecompensaXP)
                        put("fechaMuerte", 0)
                        put("nivel", nuevoNivel)
                        put("armadura", nuevaArmadura)
                    }
                    dbWrite.update("jefes", v, "id = ?", arrayOf(idJefe.toString()))
                    dbWrite.close()
                    
                    // Recursividad para obtener el jefe ahora que está activo
                    cursorMuerto.close()
                    db.close()
                    return obtenerJefeActivo()
                }
            }
            cursorMuerto.close()
        }
        cursor.close()
        db.close()
        return jefe
    }

    fun obtenerUltimoJefeDerrotadoTime(): Long {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT fechaMuerte FROM jefes WHERE derrotado = 1 ORDER BY fechaMuerte DESC LIMIT 1", null)
        var time = 0L
        if (cursor.moveToFirst()) {
            time = cursor.getLong(0)
        }
        cursor.close()
        db.close()
        return time
    }

    fun dañarJefe(dañoBase: Int, correo: String): Boolean {
        val jefe = obtenerJefeActivo() ?: return false
        val usuario = obtenerUsuarioLogueado(correo) ?: return false
        val inventario = obtenerInventario(correo).filter { it.equipado }

        // Sumar bonos del equipo a la fuerza (Ej: Arma +2 Fza)
        val bonusFzaEquipo = inventario.sumOf { it.bonusFza }.toDouble()
        val fuerzaTotal = usuario.fuerza + (bonusFzaEquipo / 10.0) // 1 punto equipo = +0.1 multiplicador

        // El daño se reduce por la armadura del jefe (mínimo 1 de daño si el ataque es positivo)
        val dañoTrasArmadura = if (dañoBase > 0) {
            (dañoBase - jefe.armadura).coerceAtLeast(1)
        } else {
            dañoBase // Curación (daño negativo) no se ve afectada por armadura
        }

        val dañoReal = (dañoTrasArmadura * fuerzaTotal).toInt()
        val nuevoHp = (jefe.hpActual - dañoReal).coerceIn(0, jefe.hpMax)

        val db = this.writableDatabase
        val valores = ContentValues().apply {
            put("hpActual", nuevoHp)
            if (nuevoHp == 0) {
                put("derrotado", 1)
                put("fechaMuerte", System.currentTimeMillis())
            }
        }
        db.update("jefes", valores, "id = ?", arrayOf(jefe.id.toString()))
        db.close()

        if (nuevoHp == 0) {
            actualizarProgresoUsuario(correo, jefe.recompensaXP, jefe.recompensaMonedas)
            return true
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
        val inventario = obtenerInventario(correo).filter { it.equipado }

        // --- APLICAR MODIFICADORES DE ATRIBUTOS (BASE + EQUIPO) ---
        val intTotal = usuario.inteligencia + (inventario.sumOf { it.bonusInt } / 10.0)
        val perTotal = usuario.percepcion + (inventario.sumOf { it.bonusPer } / 10.0)
        val conTotal = usuario.constitucion + (inventario.sumOf { it.bonusCon } / 10.0)

        // Inteligencia potencia la XP ganada (si es positiva)
        val xpFinal = if (xpBase > 0) (xpBase * intTotal).toInt() else xpBase
        
        // Percepción potencia las monedas ganadas (si es positiva)
        val monedasFinal = if (monedasBase > 0) (monedasBase * perTotal).toInt() else monedasBase
        
        // Constitución reduce el daño recibido (si hpCambio es negativo)
        val hpFinalCambio = if (hpCambioBase < 0) {
            (hpCambioBase / conTotal).toInt()
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

    fun obtenerJefesDerrotados(): List<Jefe> {
        val lista = mutableListOf<Jefe>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM jefes WHERE derrotado = 1 ORDER BY fechaMuerte DESC", null)
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
                    cursor.getInt(cursor.getColumnIndexOrThrow("armadura"))
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return lista
    }
}
