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

    // --- USUARIO Y PROGRESO ---
    suspend fun obtenerUsuarioLogueado(correo: String): Usuario? = withContext(Dispatchers.IO) {
        val snapshot = db.collection("usuarios").document(correo).get().await()
        snapshot.toObject(Usuario::class.java)
    }

    suspend fun registrarUsuario(usuario: Usuario): Long = withContext(Dispatchers.IO) {
        db.collection("usuarios").document(usuario.correo).set(usuario).await()
        1L
    }

    suspend fun actualizarProgresoUsuario(correo: String, xpBase: Int, monedasBase: Int, hpCambioBase: Int = 0, tipoAccion: String? = null, atributoAIncrementar: String? = null) = withContext(Dispatchers.IO) {
        val userRef = db.collection("usuarios").document(correo)
        val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        // El inventario se lee desde RpgRepository pero aquí se usa para los bonus de cálculo
        val equippedSnap = userRef.collection("inventario").whereEqualTo("equipado", true).get().await()
        val itemsEquipados = equippedSnap.toObjects(Articulo::class.java)
        val bonusFza = itemsEquipados.sumOf { it.bonusFza }
        val bonusInt = itemsEquipados.sumOf { it.bonusInt }
        val bonusCon = itemsEquipados.sumOf { it.bonusCon }
        val bonusPer = itemsEquipados.sumOf { it.bonusPer }

        db.runTransaction { transaction ->
            val u = transaction.get(userRef).toObject(Usuario::class.java) ?: return@runTransaction
            
            val bonusRacha = when {
                u.rachaActual >= 7 -> 1.25
                u.rachaActual >= 3 -> 1.10
                else -> 1.0
            }

            val multFza = u.fuerza + (bonusFza / 10.0)
            val multInt = u.inteligencia + (bonusInt / 10.0)
            val multCon = u.constitucion + (bonusCon / 10.0)
            val multPer = u.percepcion + (bonusPer / 10.0)

            val xpFinal = if (xpBase > 0) (xpBase * multInt * bonusRacha).toInt() else xpBase
            val monedasFinal = if (monedasBase > 0) (monedasBase * multPer * bonusRacha).toInt() else monedasBase
            val hpFinalCambio = if (hpCambioBase < 0) (hpCambioBase / multCon).toInt() else hpCambioBase

            var nuevoXp = u.experiencia + xpFinal
            var nuevoNivel = u.nivel
            var nuevasMonedas = u.monedas + monedasFinal
            var nuevaHp = u.hp + hpFinalCambio
            var nuevosPuntos = u.puntosDisponibles
            
            var nuevaFza = u.fuerza
            var nuevaInt = u.inteligencia
            var nuevaCon = u.constitucion
            var nuevaPer = u.percepcion

            if (tipoAccion == "HABITO" && atributoAIncrementar != null) {
                val incremento = 0.05
                when (atributoAIncrementar.lowercase()) {
                    "fuerza" -> nuevaFza += incremento
                    "inteligencia" -> nuevaInt += incremento
                    "constitucion" -> nuevaCon += incremento
                    "percepcion" -> nuevaPer += incremento
                }
            }

            var totalTareas = u.totalTareasCompletadas
            var totalDailies = u.totalDailiesCompletadas
            var totalHabitos = u.totalHabitosCompletados

            when(tipoAccion) {
                "TAREA" -> totalTareas++
                "DAILY" -> totalDailies++
                "HABITO" -> totalHabitos++
            }

            var xpParaSiguienteNivel = nuevoNivel * 100
            while (nuevoXp >= xpParaSiguienteNivel) {
                nuevoXp -= xpParaSiguienteNivel
                nuevoNivel++
                xpParaSiguienteNivel = nuevoNivel * 100
                nuevaHp = 50 
                nuevosPuntos += 3
            }

            if (nuevaHp <= 0) {
                nuevaHp = 0
                nuevasMonedas = (nuevasMonedas * 0.8).toInt()
            }

            nuevaHp = nuevaHp.coerceIn(0, 50)
            nuevasMonedas = nuevasMonedas.coerceAtLeast(0)

            transaction.update(userRef, mapOf(
                "experiencia" to nuevoXp,
                "nivel" to nuevoNivel,
                "monedas" to nuevasMonedas,
                "hp" to nuevaHp,
                "puntosDisponibles" to nuevosPuntos,
                "fuerza" to nuevaFza,
                "inteligencia" to nuevaInt,
                "constitucion" to nuevaCon,
                "percepcion" to nuevaPer,
                "totalTareasCompletadas" to totalTareas,
                "totalDailiesCompletadas" to totalDailies,
                "totalHabitosCompletados" to totalHabitos
            ))

            if (xpFinal > 0) {
                val histRef = userRef.collection("historial_progreso").document()
                transaction.set(histRef, Progreso(id = histRef.id.hashCode(), correo_usuario = correo, fecha = hoy, xp = xpFinal))
            }
        }.await()
    }

    suspend fun actualizarAtributos(correo: String, fza: Double, int: Double, con: Double, per: Double, puntos: Int) = withContext(Dispatchers.IO) {
        val userRef = db.collection("usuarios").document(correo)
        db.runTransaction { transaction ->
            val u = transaction.get(userRef).toObject(Usuario::class.java) ?: return@runTransaction
            if (u.puntosDisponibles >= puntos) {
                transaction.update(userRef, mapOf(
                    "fuerza" to u.fuerza + fza,
                    "inteligencia" to u.inteligencia + int,
                    "constitucion" to u.constitucion + con,
                    "percepcion" to u.percepcion + per,
                    "puntosDisponibles" to u.puntosDisponibles - puntos
                ))
            }
        }.await()
    }

    suspend fun actualizarRacha(correo: String) = withContext(Dispatchers.IO) {
        val userRef = db.collection("usuarios").document(correo)
        db.runTransaction { transaction ->
            val u = transaction.get(userRef).toObject(Usuario::class.java) ?: return@runTransaction
            val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            if (u.ultimaFechaActividad != hoy) {
                val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                val ayer = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
                val nuevaRacha = if (u.ultimaFechaActividad == ayer) u.rachaActual + 1 else 1
                transaction.update(userRef, mapOf("rachaActual" to nuevaRacha, "rachaMaxima" to if (nuevaRacha > u.rachaMaxima) nuevaRacha else u.rachaMaxima, "ultimaFechaActividad" to hoy))
            }
        }.await()
    }

    suspend fun procesarDailiesFallidas(correo: String, dias: Int): Pair<Int, Boolean> = withContext(Dispatchers.IO) {
        val userRef = db.collection("usuarios").document(correo)
        var danioTotal = 0
        var murio = false
        
        db.runTransaction { transaction ->
            val u = transaction.get(userRef).toObject(Usuario::class.java) ?: return@runTransaction
            if (dias > 1) transaction.update(userRef, "rachaActual", 0)
            danioTotal = 5 * dias
            val nuevaHp = (u.hp - danioTotal).coerceAtLeast(0)
            murio = (nuevaHp <= 0 && u.hp > 0)
            
            var nuevasMonedas = u.monedas
            if (nuevaHp <= 0) {
                nuevasMonedas = (u.monedas * 0.8).toInt()
            }
            
            transaction.update(userRef, mapOf("hp" to nuevaHp, "monedas" to nuevasMonedas))
        }.await()
        Pair(danioTotal, murio)
    }

    suspend fun actualizarRecompensaDiaria(correo: String, fecha: String) = withContext(Dispatchers.IO) {
        db.collection("usuarios").document(correo).update("ultimaFechaRecompensa", fecha).await()
    }

    suspend fun marcarTutorialComoVisto(correo: String) = withContext(Dispatchers.IO) {
        db.collection("usuarios").document(correo).update("tutorialVisto", true).await()
    }

    // --- LOGROS ---
    suspend fun esLogroDesbloqueado(correo: String, nombre: String): Boolean = withContext(Dispatchers.IO) {
        !db.collection("usuarios").document(correo).collection("logros").whereEqualTo("nombre", nombre).get().await().isEmpty
    }

    suspend fun desbloquearLogro(correo: String, nombre: String) = withContext(Dispatchers.IO) {
        db.collection("usuarios").document(correo).collection("logros").document(nombre).set(mapOf("nombre" to nombre, "fecha" to System.currentTimeMillis())).await()
    }

    // --- RECOMPENSAS PERSONALES ---
    suspend fun insertarRecompensa(recompensa: Recompensa) = withContext(Dispatchers.IO) {
        val docRef = db.collection("usuarios").document(recompensa.correo_usuario).collection("recompensas").document()
        val id = docRef.id.hashCode()
        db.collection("usuarios").document(recompensa.correo_usuario).collection("recompensas")
            .document(id.toString()).set(recompensa.copy(id = id)).await()
    }

    suspend fun eliminarRecompensa(id: Int, correo: String) = withContext(Dispatchers.IO) {
        db.collection("usuarios").document(correo).collection("recompensas").document(id.toString()).delete().await()
    }
}
