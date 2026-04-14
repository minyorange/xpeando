package com.example.xpeando.repository

import com.example.xpeando.database.DBHelper
import com.example.xpeando.database.daos.*
import com.example.xpeando.model.*

class DataRepository(private val dbHelper: DBHelper) {

    private val usuarioDao = UsuarioDao(dbHelper)
    private val rpgDao = RpgDao(dbHelper)
    private val logroDao = LogroDao(dbHelper)
    private val tareaDao = TareaDao(dbHelper)
    private val notaDao = NotaDao(dbHelper)

    // --- USUARIO ---
    fun registrarUsuario(usuario: Usuario) = usuarioDao.registrarUsuario(usuario)
    fun validarUsuario(correo: String, contrasena: String) = usuarioDao.validarUsuario(correo, contrasena)
    fun obtenerUsuarioLogueado(correo: String) = usuarioDao.obtenerUsuarioLogueado(correo)
    fun actualizarProgresoUsuario(correo: String, xp: Int, monedas: Int, hp: Int = 0) = 
        usuarioDao.actualizarProgresoUsuario(correo, xp, monedas, hp)
    fun actualizarRacha(correo: String) = usuarioDao.actualizarRacha(correo)
    fun actualizarAtributos(correo: String, fza: Double, int: Double, con: Double, per: Double, puntos: Int) =
        usuarioDao.actualizarAtributos(correo, fza, int, con, per, puntos)
    fun obtenerXPSemanal(correo: String) = usuarioDao.obtenerXPSemanal(correo)

    // --- RPG ---
    fun obtenerTiendaRPG() = rpgDao.obtenerTiendaRPG()
    fun comprarArticulo(correo: String, articulo: Articulo) = rpgDao.comprarArticulo(correo, articulo)
    fun obtenerInventario(correo: String) = rpgDao.obtenerInventario(correo)
    fun equiparDesequipar(correo: String, idArticulo: Int) = rpgDao.equiparDesequipar(correo, idArticulo)
    fun eliminarDelInventario(id: Int) = rpgDao.eliminarDelInventario(id)
    fun obtenerJefeActivo(correo: String) = rpgDao.obtenerJefeActivo(correo)
    fun atacarJefe(danio: Int, correo: String) = rpgDao.atacarJefe(danio, correo)
    fun obtenerJefesDerrotados(correo: String) = rpgDao.obtenerJefesDerrotados(correo)
    fun obtenerUltimoJefeDerrotadoTime(correo: String) = rpgDao.obtenerUltimoJefeDerrotadoTime(correo)

    // --- RECOMPENSAS ---
    fun insertarRecompensa(recompensa: Recompensa) = rpgDao.insertarRecompensa(recompensa)
    fun obtenerTodasRecompensas(correo: String) = rpgDao.obtenerTodasRecompensas(correo)
    fun eliminarRecompensa(id: Int) = rpgDao.eliminarRecompensa(id)

    // --- LOGROS ---
    fun esLogroDesbloqueado(correo: String, nombre: String) = logroDao.esLogroDesbloqueado(correo, nombre)
    fun desbloquearLogro(correo: String, nombre: String) = logroDao.desbloquearLogro(correo, nombre)

    // --- TAREAS, HABITOS, DAILIES ---
    fun insertarHabito(habito: Habito) = tareaDao.insertarHabito(habito)
    fun obtenerTodosHabitos(correo: String) = tareaDao.obtenerTodosHabitos(correo)
    fun eliminarHabito(id: Int) = tareaDao.eliminarHabito(id)
    fun actualizarEstadoHabito(habito: Habito) = tareaDao.actualizarEstadoHabito(habito)

    fun insertarTarea(tarea: Tarea) = tareaDao.insertarTarea(tarea)
    fun obtenerTodasLasTareas(correo: String) = tareaDao.obtenerTodasLasTareas(correo)
    fun eliminarTarea(id: Int) = tareaDao.eliminarTarea(id)
    fun actualizarTarea(tarea: Tarea) = tareaDao.actualizarTarea(tarea)

    fun insertarDaily(daily: Daily) = tareaDao.insertarDaily(daily)
    fun obtenerTodasDailies(correo: String) = tareaDao.obtenerTodasDailies(correo)
    fun actualizarEstadoDaily(daily: Daily, completada: Boolean) = tareaDao.actualizarEstadoDaily(daily, completada)
    fun eliminarDaily(id: Int) = tareaDao.eliminarDaily(id)
    
    fun obtenerTotalTareasCompletadas(correo: String) = tareaDao.obtenerTotalTareasCompletadas(correo)
    fun obtenerTotalDailiesCompletadas(correo: String) = tareaDao.obtenerTotalDailiesCompletadas(correo)
    fun obtenerTotalHabitosCompletados(correo: String) = tareaDao.obtenerTotalHabitosCompletados(correo)
    fun procesarDailiesFallidas(correo: String, mult: Int = 1) = tareaDao.procesarDailiesFallidas(correo, mult)

    // --- NOTAS ---
    fun insertarNota(nota: Nota) = notaDao.insertarNota(nota)
    fun obtenerTodasNotas(correo: String) = notaDao.obtenerTodasNotas(correo)
    fun eliminarNota(id: Int) = notaDao.eliminarNota(id)
    fun actualizarNota(nota: Nota) = notaDao.actualizarNota(nota)
}
