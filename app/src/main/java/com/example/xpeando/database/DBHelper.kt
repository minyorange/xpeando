package com.example.xpeando.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.xpeando.model.*
import com.example.xpeando.database.daos.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DBHelper(private val context: Context) : SQLiteOpenHelper(context, "xpeando_db", null, 42) {

    private var dbShared: SQLiteDatabase? = null

    val database: SQLiteDatabase
        get() {
            if (dbShared == null || !dbShared!!.isOpen) {
                dbShared = writableDatabase
            }
            return dbShared!!
        }

    // Instancias de DAOs
    val usuarioDao = UsuarioDao(this)
    val tareaDao = TareaDao(this)
    val rpgDao = RpgDao(this)
    val logroDao = LogroDao(this)
    val notaDao = NotaDao(this)

    override fun close() {
        dbShared?.close()
        super.close()
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE habitos (id INTEGER PRIMARY KEY AUTOINCREMENT, correo_usuario TEXT, nombre TEXT, experiencia INTEGER DEFAULT 10, monedas INTEGER DEFAULT 5, completadoHoy INTEGER DEFAULT 0, atributo TEXT DEFAULT 'Fuerza', vecesCompletado INTEGER DEFAULT 0)")
        db.execSQL("CREATE TABLE tareas (id INTEGER PRIMARY KEY AUTOINCREMENT, correo_usuario TEXT, nombre TEXT, dificultad INTEGER DEFAULT 1, experiencia INTEGER DEFAULT 20, monedas INTEGER DEFAULT 10, completada INTEGER DEFAULT 0)")
        db.execSQL("CREATE TABLE usuarios (id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT, correo TEXT UNIQUE, contrasena TEXT, nivel INTEGER DEFAULT 1, experiencia INTEGER DEFAULT 0, monedas INTEGER DEFAULT 0, hp INTEGER DEFAULT 50, fuerza REAL DEFAULT 1.0, inteligencia REAL DEFAULT 1.0, constitucion REAL DEFAULT 1.0, percepcion REAL DEFAULT 1.0, puntosDisponibles INTEGER DEFAULT 0, totalHabitos INTEGER DEFAULT 0, rachaActual INTEGER DEFAULT 0, rachaMaxima INTEGER DEFAULT 0, ultimaFechaActividad TEXT)")
        db.execSQL("CREATE TABLE recompensas (id INTEGER PRIMARY KEY AUTOINCREMENT, correo_usuario TEXT, nombre TEXT, precio INTEGER, icono TEXT)")
        db.execSQL("CREATE TABLE dailies (id INTEGER PRIMARY KEY AUTOINCREMENT, correo_usuario TEXT, nombre TEXT, dificultad INTEGER DEFAULT 1, experiencia INTEGER DEFAULT 15, monedas INTEGER DEFAULT 10, completadaHoy INTEGER DEFAULT 0, ultimaVezCompletada TEXT)")
        
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
                equipado INTEGER DEFAULT 0,
                cantidad INTEGER DEFAULT 1
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

        db.execSQL("CREATE TABLE jefes (id INTEGER PRIMARY KEY AUTOINCREMENT, correo_usuario TEXT, nombre TEXT, descripcion TEXT, hpMax INTEGER, hpActual INTEGER, recompensaMonedas INTEGER, recompensaXP INTEGER, icono TEXT, derrotado INTEGER DEFAULT 0, fechaMuerte LONG DEFAULT 0, nivel INTEGER DEFAULT 1, armadura INTEGER DEFAULT 0)")
        db.execSQL("CREATE TABLE notas (id INTEGER PRIMARY KEY AUTOINCREMENT, correo_usuario TEXT, titulo TEXT, contenido TEXT, fecha LONG)")
        db.execSQL("CREATE TABLE logros_usuario (id INTEGER PRIMARY KEY AUTOINCREMENT, correo TEXT, nombre_logro TEXT, fecha TEXT)")
        db.execSQL("CREATE TABLE historial_progreso (id INTEGER PRIMARY KEY AUTOINCREMENT, correo_usuario TEXT, fecha TEXT, xp INTEGER, monedas INTEGER, tipo TEXT)")

        // ÍNDICES
        db.execSQL("CREATE INDEX idx_usuarios_correo ON usuarios(correo)")
        db.execSQL("CREATE INDEX idx_habitos_correo ON habitos(correo_usuario)")
        db.execSQL("CREATE INDEX idx_tareas_correo ON tareas(correo_usuario)")
        db.execSQL("CREATE INDEX idx_dailies_correo ON dailies(correo_usuario)")
        db.execSQL("CREATE INDEX idx_inventario_correo ON inventario(correo_usuario)")
        db.execSQL("CREATE INDEX idx_historial_correo ON historial_progreso(correo_usuario)")

        insertarObjetosIniciales(db)
    }

    private fun insertarObjetosIniciales(db: SQLiteDatabase) {
        val items = listOf(
            arrayOf("Espada de Madera", "EQUIPO", "ARMA", "100", "2", "0", "0", "0", "0", "wooden-sword"),
            arrayOf("Escudo de Cartón", "EQUIPO", "ARMADURA", "120", "0", "0", "3", "0", "0", "round-shield"),
            arrayOf("Gafas de Estudioso", "EQUIPO", "ARMA", "150", "0", "4", "0", "0", "0", "spectacles"),
            arrayOf("Poción de Salud", "CONSUMIBLE", "POCION", "50", "0", "0", "0", "0", "20", "pocion_vida")
        )
        for (item in items) {
            val v = ContentValues().apply {
                put("nombre", item[0]); put("tipo", item[1]); put("subtipo", item[2]); put("precio", item[3].toInt())
                put("bonusFza", item[4].toInt()); put("bonusInt", item[5].toInt()); put("bonusCon", item[6].toInt())
                put("bonusPer", item[7].toInt()); put("bonusHp", item[8].toInt()); put("icono", item[9])
            }
            db.insert("tienda_rpg", null, v)
        }

        val recompensasDefault = listOf(
            arrayOf("Cena Especial", "50", "premios"),
            arrayOf("Ver Película", "30", "premios"),
            arrayOf("Dormir Siesta", "20", "premios")
        )
        for (r in recompensasDefault) {
            val v = ContentValues().apply {
                put("correo_usuario", null as String?)
                put("nombre", r[0]); put("precio", r[1].toInt()); put("icono", r[2])
            }
            db.insert("recompensas", null, v)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS habitos"); db.execSQL("DROP TABLE IF EXISTS tareas")
        db.execSQL("DROP TABLE IF EXISTS usuarios"); db.execSQL("DROP TABLE IF EXISTS recompensas")
        db.execSQL("DROP TABLE IF EXISTS dailies"); db.execSQL("DROP TABLE IF EXISTS inventario")
        db.execSQL("DROP TABLE IF EXISTS tienda_rpg"); db.execSQL("DROP TABLE IF EXISTS jefes")
        db.execSQL("DROP TABLE IF EXISTS logros_usuario"); db.execSQL("DROP TABLE IF EXISTS historial_progreso")
        db.execSQL("DROP TABLE IF EXISTS notas")
        context.getSharedPreferences("TutorialPrefs", Context.MODE_PRIVATE).edit().clear().apply()
        onCreate(db)
    }

    // --- FIN DE DBHelper ---
}
