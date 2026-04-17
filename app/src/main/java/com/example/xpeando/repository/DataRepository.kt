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

    suspend fun actualizarProgresoUsuario(correo: String, xpBase: Int, monedasBase: Int, hpCambioBase: Int = 0, tipoAccion: String? = null) = withContext(Dispatchers.IO) {
        val userRef = db.collection("usuarios").document(correo)
        val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        // Cargar bonos de equipo antes de la transacción
        val equippedSnap = userRef.collection("inventario").whereEqualTo("equipado", true).get().await()
        val itemsEquipados = equippedSnap.toObjects(Articulo::class.java)
        val bonusFza = itemsEquipados.sumOf { it.bonusFza }
        val bonusInt = itemsEquipados.sumOf { it.bonusInt }
        val bonusCon = itemsEquipados.sumOf { it.bonusCon }
        val bonusPer = itemsEquipados.sumOf { it.bonusPer }

        db.runTransaction { transaction ->
            val u = transaction.get(userRef).toObject(Usuario::class.java) ?: return@runTransaction
            
            val jefeRef = userRef.collection("rpg").document("jefe_activo")
            val jefeSnap = if (tipoAccion != null) transaction.get(jefeRef) else null
            val jefe = jefeSnap?.toObject(Jefe::class.java)

            val bonusRacha = when {
                u.rachaActual >= 7 -> 1.25
                u.rachaActual >= 3 -> 1.10
                else -> 1.0
            }

            // Aplicar multiplicadores base + bonos de equipo (10 bonus = +1.0 al multiplicador)
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
            
            var totalTareas = u.totalTareasCompletadas
            var totalDailies = u.totalDailiesCompletadas
            var totalHabitos = u.totalHabitosCompletados

            when(tipoAccion) {
                "TAREA" -> totalTareas++
                "DAILY" -> totalDailies++
                "HABITO" -> totalHabitos++
            }

            if (jefe != null && !jefe.derrotado && tipoAccion != null) {
                val danioBase = when(tipoAccion) {
                    "TAREA" -> 25
                    "DAILY" -> 35
                    "HABITO" -> 15
                    else -> 0
                }
                val danioTotal = (danioBase * multFza).toInt()
                val nuevaHpJefe = (jefe.hpActual - danioTotal).coerceAtLeast(0)
                
                transaction.update(jefeRef, "hpActual", nuevaHpJefe)
                
                if (nuevaHpJefe <= 0) {
                    transaction.update(jefeRef, mapOf(
                        "derrotado" to true,
                        "fechaMuerte" to System.currentTimeMillis()
                    ))

                    val histJefeRef = userRef.collection("rpg_historial").document()
                    transaction.set(histJefeRef, jefe.copy(hpActual = 0, derrotado = true, fechaMuerte = System.currentTimeMillis()))

                    nuevoXp += jefe.recompensaXP
                    nuevasMonedas += jefe.recompensaMonedas
                }
            }

            // Subida de nivel (incluye posible XP del jefe)
            var xpParaSiguienteNivel = nuevoNivel * 100
            while (nuevoXp >= xpParaSiguienteNivel) {
                nuevoXp -= xpParaSiguienteNivel
                nuevoNivel++
                xpParaSiguienteNivel = nuevoNivel * 100
                nuevaHp = 50 
                nuevosPuntos += 3
            }

            // Lógica de muerte: si el HP llega a 0, se queda en 0 para disparar el diálogo
            // y se aplica una penalización de monedas (20%)
            if (nuevaHp <= 0) {
                nuevaHp = 0
                nuevasMonedas = (nuevasMonedas * 0.8).toInt()
            }

            nuevaHp = nuevaHp.coerceIn(0, 50)
            nuevasMonedas = nuevasMonedas.coerceAtLeast(0)
            nuevoXp = nuevoXp.coerceAtLeast(0)

            // --- ESCRITURAS (WRITES) ---
            transaction.update(userRef, mapOf(
                "experiencia" to nuevoXp,
                "nivel" to nuevoNivel,
                "monedas" to nuevasMonedas,
                "hp" to nuevaHp,
                "puntosDisponibles" to nuevosPuntos,
                "totalTareasCompletadas" to totalTareas,
                "totalDailiesCompletadas" to totalDailies,
                "totalHabitosCompletados" to totalHabitos
            ))

            // VERIFICACIÓN DE LOGROS DENTRO DE LA TRANSACCIÓN
            if (tipoAccion != null) {
                // Tareas
                if (u.totalTareasCompletadas < 1 && totalTareas >= 1) transaction.set(userRef.collection("logros").document("Primeros Pasos"), mapOf("nombre" to "Primeros Pasos", "fecha" to System.currentTimeMillis()))
                if (u.totalTareasCompletadas < 10 && totalTareas >= 10) transaction.set(userRef.collection("logros").document("Cazador de Misiones"), mapOf("nombre" to "Cazador de Misiones", "fecha" to System.currentTimeMillis()))
                if (u.totalTareasCompletadas < 50 && totalTareas >= 50) transaction.set(userRef.collection("logros").document("Héroe Legendario"), mapOf("nombre" to "Héroe Legendario", "fecha" to System.currentTimeMillis()))
                
                // Dailies
                if (u.totalDailiesCompletadas < 5 && totalDailies >= 5) transaction.set(userRef.collection("logros").document("Rutina de Hierro"), mapOf("nombre" to "Rutina de Hierro", "fecha" to System.currentTimeMillis()))
                if (u.totalDailiesCompletadas < 30 && totalDailies >= 30) transaction.set(userRef.collection("logros").document("Inquebrantable"), mapOf("nombre" to "Inquebrantable", "fecha" to System.currentTimeMillis()))
                
                // Hábitos
                if (u.totalHabitosCompletados < 20 && totalHabitos >= 20) transaction.set(userRef.collection("logros").document("Maestro de Hábitos"), mapOf("nombre" to "Maestro de Hábitos", "fecha" to System.currentTimeMillis()))
            }
            
            // Monedas y Nivel
            if (u.monedas < 500 && nuevasMonedas >= 500) transaction.set(userRef.collection("logros").document("Ahorrador"), mapOf("nombre" to "Ahorrador", "fecha" to System.currentTimeMillis()))
            if (u.monedas < 1000 && nuevasMonedas >= 1000) transaction.set(userRef.collection("logros").document("El Rey Midas"), mapOf("nombre" to "El Rey Midas", "fecha" to System.currentTimeMillis()))
            if (u.nivel < 5 && nuevoNivel >= 5) transaction.set(userRef.collection("logros").document("Ascensión I"), mapOf("nombre" to "Ascensión I", "fecha" to System.currentTimeMillis()))

            // Registrar en historial para los gráficos si se ganó XP
            if (xpFinal > 0) {
                val histRef = userRef.collection("historial_progreso").document()
                val progreso = Progreso(id = histRef.id.hashCode(), correo_usuario = correo, fecha = hoy, xp = xpFinal)
                transaction.set(histRef, progreso)
            }
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
            
            // Si el jefe está derrotado, comprobamos si ha pasado el tiempo de respawn (21h)
            val ultimaMuerte = jefe?.fechaMuerte ?: 0L
            val tiempoRespawn = 21 * 60 * 60 * 1000L
            if (System.currentTimeMillis() < (ultimaMuerte + tiempoRespawn)) {
                return@withContext null // Aún no debe reaparecer
            }
        }
        
        // Si llegamos aquí, o no hay jefe o ya debe reaparecer uno nuevo
        val u = userRef.get().await().toObject(Usuario::class.java) ?: return@withContext null
        val nuevoJefe = generarNuevoJefe(u.nivel)
        jefeRef.set(nuevoJefe).await()
        nuevoJefe
    }

    private fun generarNuevoJefe(nivelUsuario: Int): Jefe {
        val hpBase = 100 + (nivelUsuario * 50)
        
        return Jefe(
            id = System.currentTimeMillis().toInt(),
            nombre = "Dragón Pereza (Nivel $nivelUsuario)",
            descripcion = "Un desafío formidable de nivel $nivelUsuario.",
            hpMax = hpBase,
            hpActual = hpBase,
            recompensaXP = 50 + (nivelUsuario * 10),
            recompensaMonedas = 100 + (nivelUsuario * 20),
            icono = "dragon_pereza",
            derrotado = false,
            nivel = nivelUsuario
        )
    }

    suspend fun atacarJefe(danio: Int, correo: String): Boolean = withContext(Dispatchers.IO) {
        val userRef = db.collection("usuarios").document(correo)
        val jefeRef = userRef.collection("rpg").document("jefe_activo")
        
        val equippedSnap = userRef.collection("inventario").whereEqualTo("equipado", true).get().await()
        val bonusFza = equippedSnap.toObjects(Articulo::class.java).sumOf { it.bonusFza }

        db.runTransaction { transaction ->
            val u = transaction.get(userRef).toObject(Usuario::class.java) ?: return@runTransaction false
            val jefe = transaction.get(jefeRef).toObject(Jefe::class.java) ?: return@runTransaction false
            val danioTotal = (danio * (u.fuerza + (bonusFza / 10.0))).toInt()
            val nuevaHp = jefe.hpActual - danioTotal
            
            if (nuevaHp <= 0) {
                transaction.update(jefeRef, "hpActual", 0, "derrotado", true, "fechaMuerte", System.currentTimeMillis())
                
                // Guardar en el historial de jefes derrotados
                val histJefeRef = userRef.collection("rpg_historial").document()
                transaction.set(histJefeRef, jefe.copy(hpActual = 0, derrotado = true, fechaMuerte = System.currentTimeMillis()))

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
        val docId = articulo.nombre.replace(" ", "_").lowercase()
        val itemRef = userRef.collection("inventario").document(docId)
        
        try {
            db.runTransaction { transaction ->
                val userSnapshot = transaction.get(userRef)
                // Usamos toObject para asegurar consistencia con el modelo y evitar problemas de tipos Long/Int
                val u = userSnapshot.toObject(Usuario::class.java) ?: return@runTransaction false
                
                if (u.monedas >= articulo.precio) {
                    val itemSnapshot = transaction.get(itemRef)
                    
                    if (itemSnapshot.exists()) {
                        if (articulo.tipo == "EQUIPO") {
                            return@runTransaction false // Ya tiene este equipo
                        }
                        val cantActual = itemSnapshot.getLong("cantidad")?.toInt() ?: 1
                        transaction.update(itemRef, "cantidad", cantActual + 1)
                    } else {
                        val articuloPropio = articulo.copy(
                            id = docId.hashCode(),
                            esPropio = true,
                            cantidad = 1,
                            equipado = false
                        )
                        transaction.set(itemRef, articuloPropio)
                    }

                    transaction.update(userRef, "monedas", u.monedas - articulo.precio)
                    true
                } else {
                    false // Monedas insuficientes
                }
            }.await()
        } catch (e: Exception) {
            android.util.Log.e("DataRepository", "Error en compra: ${e.message}")
            false
        }
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
        val invRef = db.collection("usuarios").document(correo).collection("inventario")
        val snap = invRef.whereEqualTo("id", id).get().await()
        if (snap.isEmpty) return@withContext
        
        db.runTransaction { transaction ->
            val docSnap = transaction.get(snap.documents[0].reference)
            val cantActual = docSnap.getLong("cantidad") ?: 1L
            if (cantActual > 1) {
                transaction.update(docSnap.reference, "cantidad", cantActual - 1)
            } else {
                transaction.delete(docSnap.reference)
            }
        }.await()
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

    suspend fun actualizarTarea(tarea: Tarea) = withContext(Dispatchers.IO) {
        val snap = db.collection("usuarios").document(tarea.correo_usuario).collection("tareas").whereEqualTo("id", tarea.id).get().await()
        snap.documents.forEach { it.reference.set(tarea).await() }
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
        listOf(
            Articulo(
                nombre = "Poción de Vida",
                tipo = "CONSUMIBLE",
                subtipo = "POCION",
                precio = 100,
                bonusHp = 50,
                icono = "pocion_vida"
            ),
            Articulo(
                nombre = "Gafas de Estudios",
                tipo = "EQUIPO",
                subtipo = "ACCESORIO",
                precio = 250,
                bonusInt = 2,
                icono = "gafas_estudioso"
            ),
            Articulo(
                nombre = "Espada de Madera",
                tipo = "EQUIPO",
                subtipo = "ARMA",
                precio = 300,
                bonusFza = 3,
                icono = "espada_madera"
            ),
            Articulo(
                nombre = "Escudo de Cartón",
                tipo = "EQUIPO",
                subtipo = "ARMADURA",
                precio = 200,
                bonusCon = 2,
                icono = "escudo_carton"
            )
        )
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
            var nuevaHp = u.hp - danioTotal
            var nuevasMonedas = u.monedas

            if (nuevaHp <= 0) {
                nuevaHp = 0
                nuevasMonedas = (nuevasMonedas * 0.8).toInt()
            }

            transaction.update(userRef, mapOf(
                "hp" to nuevaHp.coerceIn(0, 50),
                "monedas" to nuevasMonedas.coerceAtLeast(0)
            ))
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

    suspend fun insertarHabito(habito: Habito): Long = withContext(Dispatchers.IO) {
        val ref = db.collection("usuarios").document(habito.correo_usuario).collection("habitos").document()
        val id = ref.id.hashCode()
        ref.set(habito.copy(id = id)).await()
        id.toLong()
    }

    suspend fun eliminarHabito(id: Int, correo: String) = withContext(Dispatchers.IO) {
        val snap = db.collection("usuarios").document(correo).collection("habitos").whereEqualTo("id", id).get().await()
        snap.documents.forEach { it.reference.delete().await() }
    }

    // --- LOGROS Y ESTADISTICAS ---
    suspend fun esLogroDesbloqueado(correo: String, nombre: String): Boolean = withContext(Dispatchers.IO) {
        val snap = db.collection("usuarios").document(correo).collection("logros").whereEqualTo("nombre", nombre).get().await()
        !snap.isEmpty
    }

    suspend fun desbloquearLogro(correo: String, nombre: String) = withContext(Dispatchers.IO) {
        db.collection("usuarios").document(correo).collection("logros").document(nombre).set(mapOf("nombre" to nombre, "fecha" to System.currentTimeMillis())).await()
    }

    suspend fun obtenerHistorialCompleto(correo: String): List<Progreso> = withContext(Dispatchers.IO) {
        val snap = db.collection("usuarios").document(correo).collection("historial_progreso").get().await()
        snap.toObjects(Progreso::class.java)
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

    suspend fun actualizarNota(nota: Nota) = withContext(Dispatchers.IO) {
        val colRef = db.collection("usuarios").document(nota.correo_usuario).collection("notas")
        val snap = colRef.whereEqualTo("id", nota.id).get().await()
        for (doc in snap.documents) {
            doc.reference.set(nota).await()
        }
    }

    suspend fun eliminarNota(id: Int, correo: String) = withContext(Dispatchers.IO) {
        val colRef = db.collection("usuarios").document(correo).collection("notas")
        val snap = colRef.whereEqualTo("id", id).get().await()
        for (doc in snap.documents) {
            doc.reference.delete().await()
        }
    }
}
