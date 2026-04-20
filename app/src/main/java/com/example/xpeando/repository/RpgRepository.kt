package com.example.xpeando.repository

import com.example.xpeando.model.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class RpgRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun obtenerJefeActivo(correo: String): Jefe? = withContext(Dispatchers.IO) {
        val userRef = db.collection("usuarios").document(correo)
        val jefeRef = userRef.collection("rpg").document("jefe_activo")
        val snap = jefeRef.get().await()
        
        if (snap.exists()) {
            val jefe = snap.toObject(Jefe::class.java)
            if (jefe != null && !jefe.derrotado) return@withContext jefe
            
            val ultimaMuerte = jefe?.fechaMuerte ?: 0L
            val tiempoRespawn = 21 * 60 * 60 * 1000L
            if (System.currentTimeMillis() < (ultimaMuerte + tiempoRespawn)) {
                return@withContext null
            }
        }
        
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
            
            // Si el jefe ya está derrotado, no hacemos nada
            if (jefe.derrotado) return@runTransaction false

            val danioTotal = (danio * (u.fuerza + (bonusFza / 10.0))).toInt()
            val nuevaHp = jefe.hpActual - danioTotal
            
            if (nuevaHp <= 0) {
                transaction.update(jefeRef, "hpActual", 0, "derrotado", true, "fechaMuerte", System.currentTimeMillis())
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

    suspend fun obtenerInventario(correo: String): List<Articulo> = withContext(Dispatchers.IO) {
        val snap = db.collection("usuarios").document(correo).collection("inventario").get().await()
        snap.toObjects(Articulo::class.java)
    }

    suspend fun comprarArticulo(correo: String, articulo: Articulo): Boolean = withContext(Dispatchers.IO) {
        val userRef = db.collection("usuarios").document(correo)
        // El ID debe ser consistente para evitar duplicados
        val itemId = articulo.nombre.hashCode()
        val itemRef = userRef.collection("inventario").document(itemId.toString())
        
        db.runTransaction { transaction ->
            val u = transaction.get(userRef).toObject(Usuario::class.java) ?: return@runTransaction false
            if (u.monedas >= articulo.precio) {
                val itemSnapshot = transaction.get(itemRef)
                
                if (itemSnapshot.exists()) {
                    // Si es equipo, no permitimos comprarlo dos veces (ya lo tiene)
                    if (articulo.tipo == "EQUIPO") return@runTransaction false
                    
                    // Si es consumible, aumentamos la cantidad
                    val cantActual = itemSnapshot.getLong("cantidad")?.toInt() ?: 1
                    transaction.update(itemRef, "cantidad", cantActual + 1)
                } else {
                    // Si no existe, lo creamos
                    transaction.set(itemRef, articulo.copy(
                        id = itemId, 
                        esPropio = true, 
                        cantidad = 1, 
                        equipado = false
                    ))
                }
                transaction.update(userRef, "monedas", u.monedas - articulo.precio)
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
        val invRef = db.collection("usuarios").document(correo).collection("inventario")
        val snap = invRef.whereEqualTo("id", id).get().await()
        if (snap.isEmpty) return@withContext
        db.runTransaction { transaction ->
            val docSnap = transaction.get(snap.documents[0].reference)
            val cantActual = docSnap.getLong("cantidad") ?: 1L
            if (cantActual > 1) transaction.update(docSnap.reference, "cantidad", cantActual - 1)
            else transaction.delete(docSnap.reference)
        }.await()
    }

    suspend fun obtenerTiendaRPG(): List<Articulo> = withContext(Dispatchers.IO) {
        listOf(
            Articulo(nombre = "Poción de Vida", tipo = "CONSUMIBLE", subtipo = "POCION", precio = 100, bonusHp = 50, icono = "pocion_vida"),
            Articulo(nombre = "Gafas de Estudios", tipo = "EQUIPO", subtipo = "ACCESORIO", precio = 250, bonusInt = 2, icono = "gafas_estudioso"),
            Articulo(nombre = "Espada de Madera", tipo = "EQUIPO", subtipo = "ARMA", precio = 300, bonusFza = 3, icono = "espada_madera"),
            Articulo(nombre = "Escudo de Cartón", tipo = "EQUIPO", subtipo = "ARMADURA", precio = 200, bonusCon = 2, icono = "escudo_carton")
        )
    }

    suspend fun obtenerHistorialJefes(correo: String): List<Jefe> = withContext(Dispatchers.IO) {
        try {
            val snap = db.collection("usuarios").document(correo).collection("rpg_historial")
                .orderBy("fechaMuerte", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get().await()
            snap.toObjects(Jefe::class.java)
        } catch (e: Exception) {
            // Si falla el orden por falta de índice, devolvemos sin ordenar para no romper la app
            val snap = db.collection("usuarios").document(correo).collection("rpg_historial").get().await()
            snap.toObjects(Jefe::class.java).sortedByDescending { it.fechaMuerte }
        }
    }
}
