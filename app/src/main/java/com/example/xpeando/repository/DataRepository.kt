package com.example.xpeando.repository

import com.example.xpeando.database.DBHelper
import com.example.xpeando.database.daos.*
import com.example.xpeando.model.*

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DataRepository(private val dbHelper: DBHelper) {

    private val usuarioDao = dbHelper.usuarioDao
    private val rpgDao = dbHelper.rpgDao
    private val logroDao = dbHelper.logroDao
    private val tareaDao = dbHelper.tareaDao
    private val notaDao = dbHelper.notaDao

    // --- USUARIO ---
    suspend fun registrarUsuario(usuario: Usuario) = withContext(Dispatchers.IO) {
        usuarioDao.registrarUsuario(usuario)
    }

    suspend fun validarUsuario(correo: String, contrasena: String) = withContext(Dispatchers.IO) {
        usuarioDao.validarUsuario(correo, contrasena)
    }

    suspend fun obtenerUsuarioLogueado(correo: String): Usuario? = withContext(Dispatchers.IO) {
        usuarioDao.obtenerUsuarioLogueado(correo)
    }

    suspend fun actualizarProgresoUsuario(correo: String, xp: Int, monedas: Int, hp: Int = 0) = withContext(Dispatchers.IO) {
        usuarioDao.actualizarProgresoUsuario(correo, xp, monedas, hp)
    }

    suspend fun actualizarRacha(correo: String) = withContext(Dispatchers.IO) {
        usuarioDao.actualizarRacha(correo)
    }

    suspend fun actualizarAtributos(correo: String, fza: Double, int: Double, con: Double, per: Double, puntos: Int) = withContext(Dispatchers.IO) {
        usuarioDao.actualizarAtributos(correo, fza, int, con, per, puntos)
    }

    suspend fun upsertUsuario(usuario: Usuario) = withContext(Dispatchers.IO) {
        usuarioDao.upsertUsuario(usuario)
    }

    suspend fun obtenerXPSemanal(correo: String) = withContext(Dispatchers.IO) {
        usuarioDao.obtenerXPSemanal(correo)
    }

    suspend fun obtenerHistorialCompleto(correo: String): List<Progreso> = withContext(Dispatchers.IO) {
        usuarioDao.obtenerHistorialCompleto(correo)
    }

    // --- RPG ---
    suspend fun obtenerTiendaRPG() = withContext(Dispatchers.IO) { rpgDao.obtenerTiendaRPG() }
    suspend fun comprarArticulo(correo: String, articulo: Articulo) = withContext(Dispatchers.IO) {
        rpgDao.comprarArticulo(correo, articulo)
    }
    suspend fun obtenerInventario(correo: String) = withContext(Dispatchers.IO) { rpgDao.obtenerInventario(correo) }
    suspend fun equiparDesequipar(correo: String, idArticulo: Int) = withContext(Dispatchers.IO) {
        rpgDao.equiparDesequipar(correo, idArticulo)
    }
    suspend fun eliminarDelInventario(id: Int) = withContext(Dispatchers.IO) { rpgDao.eliminarDelInventario(id) }
    suspend fun obtenerJefeActivo(correo: String) = withContext(Dispatchers.IO) { rpgDao.obtenerJefeActivo(correo) }
    suspend fun atacarJefe(danio: Int, correo: String) = withContext(Dispatchers.IO) { rpgDao.atacarJefe(danio, correo) }
    suspend fun obtenerJefesDerrotados(correo: String) = withContext(Dispatchers.IO) { rpgDao.obtenerJefesDerrotados(correo) }
    suspend fun obtenerUltimoJefeDerrotadoTime(correo: String) = withContext(Dispatchers.IO) { rpgDao.obtenerUltimoJefeDerrotadoTime(correo) }

    // --- RECOMPENSAS ---
    suspend fun insertarRecompensa(recompensa: Recompensa) = withContext(Dispatchers.IO) { rpgDao.insertarRecompensa(recompensa) }
    suspend fun obtenerTodasRecompensas(correo: String) = withContext(Dispatchers.IO) { rpgDao.obtenerTodasRecompensas(correo) }
    suspend fun eliminarRecompensa(id: Int) = withContext(Dispatchers.IO) { rpgDao.eliminarRecompensa(id) }

    // --- LOGROS ---
    suspend fun esLogroDesbloqueado(correo: String, nombre: String) = withContext(Dispatchers.IO) { logroDao.esLogroDesbloqueado(correo, nombre) }
    suspend fun desbloquearLogro(correo: String, nombre: String) = withContext(Dispatchers.IO) { logroDao.desbloquearLogro(correo, nombre) }

    // --- TAREAS, HABITOS, DAILIES ---
    suspend fun insertarHabito(habito: Habito) = withContext(Dispatchers.IO) { tareaDao.insertarHabito(habito) }
    suspend fun obtenerTodosHabitos(correo: String) = withContext(Dispatchers.IO) { tareaDao.obtenerTodosHabitos(correo) }
    suspend fun eliminarHabito(id: Int) = withContext(Dispatchers.IO) { tareaDao.eliminarHabito(id) }
    suspend fun actualizarEstadoHabito(habito: Habito) = withContext(Dispatchers.IO) {
        tareaDao.actualizarEstadoHabito(habito)
    }

    suspend fun insertarTarea(tarea: Tarea) = withContext(Dispatchers.IO) { tareaDao.insertarTarea(tarea) }
    suspend fun obtenerTodasLasTareas(correo: String) = withContext(Dispatchers.IO) { tareaDao.obtenerTodasLasTareas(correo) }
    suspend fun eliminarTarea(id: Int) = withContext(Dispatchers.IO) { tareaDao.eliminarTarea(id) }
    suspend fun actualizarTarea(tarea: Tarea) = withContext(Dispatchers.IO) { tareaDao.actualizarTarea(tarea) }

    suspend fun insertarDaily(daily: Daily) = withContext(Dispatchers.IO) { tareaDao.insertarDaily(daily) }
    suspend fun obtenerTodasDailies(correo: String) = withContext(Dispatchers.IO) { tareaDao.obtenerTodasDailies(correo) }
    suspend fun actualizarEstadoDaily(daily: Daily, completada: Boolean) = withContext(Dispatchers.IO) {
        tareaDao.actualizarEstadoDaily(daily, completada)
    }
    suspend fun eliminarDaily(id: Int) = withContext(Dispatchers.IO) { tareaDao.eliminarDaily(id) }
    
    suspend fun obtenerTotalTareasCompletadas(correo: String) = withContext(Dispatchers.IO) { tareaDao.obtenerTotalTareasCompletadas(correo) }
    suspend fun obtenerTotalDailiesCompletadas(correo: String) = withContext(Dispatchers.IO) { tareaDao.obtenerTotalDailiesCompletadas(correo) }
    suspend fun obtenerTotalHabitosCompletados(correo: String) = withContext(Dispatchers.IO) { tareaDao.obtenerTotalHabitosCompletados(correo) }
    suspend fun procesarDailiesFallidas(correo: String, mult: Int = 1) = withContext(Dispatchers.IO) {
        tareaDao.procesarDailiesFallidas(correo, mult)
    }

    // --- NOTAS ---
    suspend fun insertarNota(nota: Nota) = withContext(Dispatchers.IO) { notaDao.insertarNota(nota) }
    suspend fun obtenerTodasNotas(correo: String) = withContext(Dispatchers.IO) { notaDao.obtenerTodasNotas(correo) }
    suspend fun eliminarNota(id: Int) = withContext(Dispatchers.IO) { notaDao.eliminarNota(id) }
    suspend fun actualizarNota(nota: Nota) = withContext(Dispatchers.IO) { notaDao.actualizarNota(nota) }

}
