package com.example.xpeando.repository

import com.example.xpeando.model.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class DataRepository {

    private val db = FirebaseFirestore.getInstance()

    // --- USUARIO (HÉROE / ATRIBUTOS) ---
    suspend fun obtenerUsuarioLogueado(correo: String): Usuario? = withContext(Dispatchers.IO) {
        val snapshot = db.collection("usuarios").document(correo).get().await()
        snapshot.toObject(Usuario::class.java)
    }

    suspend fun registrarUsuario(usuario: Usuario): Long = withContext(Dispatchers.IO) {
        db.collection("usuarios").document(usuario.correo).set(usuario).await()
        1L
    }

    suspend fun actualizarProgresoUsuario(correo: String, xpBase: Int, monedasBase: Int, hpCambioBase: Int = 0) = withContext(Dispatchers.IO) {
        val userRef = db.collection("usuarios").document(correo)
        db.runTransaction { transaction ->
            val u = transaction.get(userRef).toObject(Usuario::class.java) ?: return@runTransaction
            
            val bonusRacha = when {
                u.rachaActual >= 7 -> 1.25
                u.rachaActual >= 3 -> 1.10
                else -> 1.0
            }

            val xpFinal = if (xpBase > 0) (xpBase * u.inteligencia * bonusRacha).toInt() else xpBase
            val monedasFinal = if (monedasBase > 0) (monedasBase * u.percepcion * bonusRacha).toInt() else monedasBase
            val hpFinalCambio = if (hpCambioBase < 0) (hpCambioBase / u.constitucion).toInt() else hpCambioBase

            var nuevoXp = u.experiencia + xpFinal
            var nuevoNivel = u.nivel
            var nuevasMonedas = u.monedas + monedasFinal
            var nuevaHp = u.hp + hpFinalCambio
            var nuevosPuntos = u.puntosDisponibles

            var xpParaSiguienteNivel = nuevoNivel * 100
            while (nuevoXp >= xpParaSiguienteNivel) {
                nuevoXp -= xpParaSiguienteNivel
                nuevoNivel++
                xpParaSiguienteNivel = nuevoNivel * 100
                nuevaHp = 50 
                nuevosPuntos += 3
            }

            nuevaHp = nuevaHp.coerceIn(0, 50)
            nuevasMonedas = nuevasMonedas.coerceAtLeast(0)
            nuevoXp = nuevoXp.coerceAtLeast(0)

            transaction.update(userRef, mapOf(
                "experiencia" to nuevoXp,
                "nivel" to nuevoNivel,
                "monedas" to nuevasMonedas,
                "hp" to nuevaHp,
                "puntosDisponibles" to nuevosPuntos
            ))
        }.await()
    }

    suspend fun actualizarAtributos(correo: String, fza: Double, int: Double, con: Double, per: Double, puntos: Int) = withContext(Dispatchers.IO) {
        val userRef = db.collection("usuarios").document(correo)
        db.runTransaction { transaction ->
            val u = transaction.get(userRef).toObject(Usuario::class.java) ?: return@runTransaction
            transaction.update(userRef, mapOf(
                "fuerza" to u.fuerza + fza,
                "inteligencia" to u.inteligencia + int,
                "constitucion" to u.constitucion + con,
                "percepcion" to u.percepcion + per,
                "puntosDisponibles" to u.puntosDisponibles - puntos
            ))
        }.await()
    }

    // --- RPG ---
    suspend fun obtenerJefeActivo(correo: String): Jefe? = withContext(Dispatchers.IO) {
        val userRef = db.collection("usuarios").document(correo)
        val jefeRef = userRef.collection("rpg").document("jefe_activo")
        val snap = jefeRef.get().await()
        if (snap.exists()) {
            val jefe = snap.toObject(Jefe::class.java)
            if (jefe != null && !jefe.derrotado) return@withContext jefe
        }
        null
    }

    suspend fun atacarJefe(danio: Int, correo: String): Boolean = withContext(Dispatchers.IO) {
        val userRef = db.collection("usuarios").document(correo)
        val jefeRef = userRef.collection("rpg").document("jefe_activo")
        
        db.runTransaction { transaction ->
            val u = transaction.get(userRef).toObject(Usuario::class.java) ?: return@runTransaction false
            val jefe = transaction.get(jefeRef).toObject(Jefe::class.java) ?: return@runTransaction false
            val danioTotal = (danio * u.fuerza).toInt()
            val nuevaHp = jefe.hpActual - danioTotal
            
            if (nuevaHp <= 0) {
                transaction.update(jefeRef, "hpActual", 0, "derrotado", true, "fechaMuerte", System.currentTimeMillis())
                transaction.update(userRef, "experiencia", u.experiencia + jefe.recompensaXP, "monedas", u.monedas + jefe.recompensaMonedas)
                true
            } else {
                transaction.update(jefeRef, "hpActual", nuevaHp)
                false
            }
        }.await()
    }

    // --- INVENTARIO Y TIENDA ---
    suspend fun obtenerInventario(correo: String): List<Articulo> = withContext(Dispatchers.IO) {
        val snap = db.collection("usuarios").document(correo).collection("inventario").get().await()
        snap.toObjects(Articulo::class.java)
    }

    suspend fun comprarArticulo(correo: String, articulo: Articulo): Boolean = withContext(Dispatchers.IO) {
        val userRef = db.collection("usuarios").document(correo)
        db.runTransaction { transaction ->
            val u = transaction.get(userRef).toObject(Usuario::class.java) ?: return@runTransaction false
            if (u.monedas >= articulo.precio) {
                transaction.update(userRef, "monedas", u.monedas - articulo.precio)
                val invRef = userRef.collection("inventario").document()
                transaction.set(invRef, articulo.copy(id = invRef.id.hashCode()))
                true
            } else false
        }.await()
    }

    suspend fun equiparDesequipar(correo: String, idArticulo: Int) = withContext(Dispatchers.IO) {
        val invRef = db.collection("usuarios").document(correo).collection("inventario")
        val snap = invRef.whereEqualTo("id", idArticulo).get().await()
        if (snap.isEmpty) return@withContext
        val doc = snap.documents[0]
        val estado = doc.getBoolean("equipado") ?: false
        doc.reference.update("equipado", !estado).await()
    }

    suspend fun eliminarDelInventario(id: Int, correo: String) = withContext(Dispatchers.IO) {
        val snap = db.collection("usuarios").document(correo).collection("inventario").whereEqualTo("id", id).get().await()
        snap.documents.forEach { it.reference.delete().await() }
    }

    // --- TAREAS, DAILIES, HABITOS ---
    suspend fun obtenerTodasLasTareas(correo: String): List<Tarea> = withContext(Dispatchers.IO) {
        val snap = db.collection("usuarios").document(correo).collection("tareas").get().await()
        snap.toObjects(Tarea::class.java)
    }

    suspend fun insertarTarea(tarea: Tarea): Long = withContext(Dispatchers.IO) {
        val ref = db.collection("usuarios").document(tarea.correo_usuario).collection("tareas").document()
        val id = ref.id.hashCode()
        ref.set(tarea.copy(id = id)).await()
        id.toLong()
    }

    suspend fun eliminarTarea(id: Int, correo: String) = withContext(Dispatchers.IO) {
        val snap = db.collection("usuarios").document(correo).collection("tareas").whereEqualTo("id", id).get().await()
        snap.documents.forEach { it.reference.delete().await() }
    }

    // --- RECOMPENSAS Y TIENDA ---
    suspend fun actualizarRecompensaDiaria(correo: String, fecha: String) = withContext(Dispatchers.IO) {
        db.collection("usuarios").document(correo).update("ultimaFechaRecompensa", fecha).await()
    }

    suspend fun obtenerTodasRecompensas(correo: String): List<Recompensa> = withContext(Dispatchers.IO) {
        val snap = db.collection("usuarios").document(correo).collection("recompensas").get().await()
        snap.toObjects(Recompensa::class.java)
    }

    suspend fun insertarRecompensa(recompensa: Recompensa): Long = withContext(Dispatchers.IO) {
        val ref = db.collection("usuarios").document(recompensa.correo_usuario).collection("recompensas").document()
        val id = ref.id.hashCode()
        ref.set(recompensa.copy(id = id)).await()
        id.toLong()
    }

    suspend fun eliminarRecompensa(id: Int, correo: String) = withContext(Dispatchers.IO) {
        val snap = db.collection("usuarios").document(correo).collection("recompensas").whereEqualTo("id", id).get().await()
        snap.documents.forEach { it.reference.delete().await() }
    }

    suspend fun obtenerTiendaRPG(): List<Articulo> = withContext(Dispatchers.IO) {
        val snap = db.collection("tienda_global").get().await()
        snap.toObjects(Articulo::class.java)
    }

    suspend fun obtenerHistorialJefes(correo: String): List<Jefe> = withContext(Dispatchers.IO) {
        val snap = db.collection("usuarios").document(correo).collection("rpg_historial")
            .orderBy("nivel", com.google.firebase.firestore.Query.Direction.DESCENDING).get().await()
        snap.toObjects(Jefe::class.java)
    }

    suspend fun obtenerTodasDailies(correo: String): List<Daily> = withContext(Dispatchers.IO) {
        val snap = db.collection("usuarios").document(correo).collection("dailies").get().await()
        val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        snap.documents.mapNotNull { it.toObject(Daily::class.java)?.copy(completadaHoy = it.getString("ultimaVezCompletada") == hoy) }
    }

    suspend fun actualizarEstadoDaily(daily: Daily, completada: Boolean) = withContext(Dispatchers.IO) {
        val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val snap = db.collection("usuarios").document(daily.correo_usuario).collection("dailies").whereEqualTo("id", daily.id).get().await()
        snap.documents.forEach { it.reference.update("ultimaVezCompletada", if (completada) hoy else "").await() }
    }

    suspend fun actualizarRacha(correo: String) = withContext(Dispatchers.IO) {
        val userRef = db.collection("usuarios").document(correo)
        db.runTransaction { transaction ->
            val u = transaction.get(userRef).toObject(Usuario::class.java) ?: return@runTransaction
            val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            
            if (u.ultimaFechaActividad != hoy) {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -1)
                val ayer = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
                
                val nuevaRacha = if (u.ultimaFechaActividad == ayer) u.rachaActual + 1 else 1
                val nuevaRachaMax = if (nuevaRacha > u.rachaMaxima) nuevaRacha else u.rachaMaxima
                
                transaction.update(userRef, mapOf(
                    "rachaActual" to nuevaRacha,
                    "rachaMaxima" to nuevaRachaMax,
                    "ultimaFechaActividad" to hoy
                ))
            }
        }.await()
    }

    suspend fun procesarDailiesFallidas(correo: String, dias: Int): Int = withContext(Dispatchers.IO) {
        var danioTotal = 0
        val userRef = db.collection("usuarios").document(correo)
        db.runTransaction { transaction ->
            val u = transaction.get(userRef).toObject(Usuario::class.java) ?: return@runTransaction
            
            // Si pasan días sin actividad, se pierde la racha
            if (dias > 1) {
                transaction.update(userRef, "rachaActual", 0)
            }

            danioTotal = 5 * dias
            val nuevaHp = (u.hp - danioTotal).coerceAtLeast(0)
            transaction.update(userRef, "hp", nuevaHp)
        }.await()
        danioTotal
    }

    suspend fun insertarDaily(daily: Daily): Long = withContext(Dispatchers.IO) {
        val ref = db.collection("usuarios").document(daily.correo_usuario).collection("dailies").document()
        val id = ref.id.hashCode()
        ref.set(daily.copy(id = id)).await()
        id.toLong()
    }

    suspend fun eliminarDaily(id: Int, correo: String) = withContext(Dispatchers.IO) {
        val snap = db.collection("usuarios").document(correo).collection("dailies").whereEqualTo("id", id).get().await()
        snap.documents.forEach { it.reference.delete().await() }
    }

    suspend fun obtenerTodosHabitos(correo: String): List<Habito> = withContext(Dispatchers.IO) {
        val snap = db.collection("usuarios").document(correo).collection("habitos").get().await()
        snap.toObjects(Habito::class.java)
    }

    suspend fun actualizarEstadoHabito(habito: Habito, delta: Int) = withContext(Dispatchers.IO) {
        val snap = db.collection("usuarios").document(habito.correo_usuario).collection("habitos").whereEqualTo("id", habito.id).get().await()
        snap.documents.forEach { it.reference.update("vecesCompletado", habito.vecesCompletado + delta).await() }
    }

    // --- LOGROS Y ESTADISTICAS ---
    suspend fun esLogroDesbloqueado(correo: String, nombre: String): Boolean = withContext(Dispatchers.IO) {
        val snap = db.collection("usuarios").document(correo).collection("logros").whereEqualTo("nombre", nombre).get().await()
        !snap.isEmpty
    }

    suspend fun desbloquearLogro(correo: String, nombre: String) = withContext(Dispatchers.IO) {
        db.collection("usuarios").document(correo).collection("logros").document(nombre).set(mapOf("nombre" to nombre, "fecha" to System.currentTimeMillis())).await()
    }

    suspend fun obtenerTotalTareasCompletadas(correo: String): Int = withContext(Dispatchers.IO) {
        val snap = db.collection("usuarios").document(correo).collection("tareas").whereEqualTo("completada", true).get().await()
        snap.size()
    }

    suspend fun obtenerTotalDailiesCompletadas(correo: String): Int = withContext(Dispatchers.IO) {
        val snap = db.collection("usuarios").document(correo).collection("dailies").get().await()
        snap.documents.count { (it.getString("ultimaVezCompletada") ?: "").isNotEmpty() }
    }

    suspend fun obtenerTotalHabitosCompletados(correo: String): Int = withContext(Dispatchers.IO) {
        val snap = db.collection("usuarios").document(correo).collection("habitos").get().await()
        snap.toObjects(Habito::class.java).sumOf { it.vecesCompletado }
    }

    // --- NOTAS ---
    suspend fun obtenerTodasNotas(correo: String): List<Nota> = withContext(Dispatchers.IO) {
        val snap = db.collection("usuarios").document(correo).collection("notas").get().await()
        snap.toObjects(Nota::class.java)
    }

    suspend fun insertarNota(nota: Nota): Long = withContext(Dispatchers.IO) {
        val ref = db.collection("usuarios").document(nota.correo_usuario).collection("notas").document()
        val id = ref.id.hashCode()
        ref.set(nota.copy(id = id)).await()
        id.toLong()
    }

    suspend fun eliminarNota(id: Int, correo: String) = withContext(Dispatchers.IO) {
        val snap = db.collection("usuarios").document(correo).collection("notas").whereEqualTo("id", id).get().await()
        snap.documents.forEach { it.reference.delete().await() }
    }
}
