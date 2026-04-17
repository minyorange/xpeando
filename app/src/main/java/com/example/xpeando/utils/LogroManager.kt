package com.example.xpeando.utils

import android.content.Context
import com.example.xpeando.R
import com.example.xpeando.model.Usuario
import com.example.xpeando.model.Logro
import com.example.xpeando.repository.DataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

object LogroManager {

    suspend fun obtenerLogrosDefinidos(repository: DataRepository, usuario: Usuario): List<Logro> {
        val totalTareas = usuario.totalTareasCompletadas
        val totalDailies = usuario.totalDailiesCompletadas
        val totalHabitos = usuario.totalHabitosCompletados
        val monedas = usuario.monedas
        val totalItems = repository.obtenerInventario(usuario.correo).size

        return listOf(
            Logro("Primeros Pasos", "Completa 1 misión", 1, totalTareas, totalTareas >= 1, R.drawable.pasos),
            Logro("Cazador de Misiones", "Completa 10 misiones", 10, totalTareas, totalTareas >= 10, R.drawable.misiones),
            Logro("Héroe Legendario", "Completa 50 misiones", 50, totalTareas, totalTareas >= 50, R.drawable.lengendario),
            Logro("Maestro de Hábitos", "Realiza 20 acciones de hábitos", 20, totalHabitos, totalHabitos >= 20, R.drawable.mhabitos),
            Logro("Rutina de Hierro", "Completa 5 dailies", 5, totalDailies, totalDailies >= 5, R.drawable.hierro),
            Logro("Inquebrantable", "Completa 30 dailies", 30, totalDailies, totalDailies >= 30, R.drawable.inquebrantable),
            Logro("Ascensión I", "Llega a nivel 5", 5, usuario.nivel, usuario.nivel >= 5, R.drawable.ascension),
            Logro("Ahorrador", "Consigue 500 monedas", 500, monedas, monedas >= 500, R.drawable.ahorrador),
            Logro("El Rey Midas", "Consigue 1000 monedas", 1000, monedas, monedas >= 1000, R.drawable.rey),
            Logro("Coleccionista", "Ten 3 objetos en tu inventario", 3, totalItems, totalItems >= 3, R.drawable.colecionista)
        )
    }

    suspend fun verificarNuevosLogros(
        context: Context, 
        repository: DataRepository, 
        usuario: Usuario,
        tipo: String
    ) {
        val correo = usuario.correo
        val logrosCruzados = mutableListOf<String>()

        // 1. Tareas
        if (tipo == "TAREA") {
            val nuevoTotal = usuario.totalTareasCompletadas + 1
            if (nuevoTotal == 1) logrosCruzados.add("Primeros Pasos")
            if (nuevoTotal == 10) logrosCruzados.add("Cazador de Misiones")
            if (nuevoTotal == 50) logrosCruzados.add("Héroe Legendario")
        }

        // 2. Dailies
        if (tipo == "DAILY") {
            val nuevoTotal = usuario.totalDailiesCompletadas + 1
            if (nuevoTotal == 5) logrosCruzados.add("Rutina de Hierro")
            if (nuevoTotal == 30) logrosCruzados.add("Inquebrantable")
        }

        // 3. Hábitos
        if (tipo == "HABITO") {
            val nuevoTotal = usuario.totalHabitosCompletados + 1
            if (nuevoTotal == 20) logrosCruzados.add("Maestro de Hábitos")
        }

        // 4. Otros (Nivel, Monedas, Colección)
        // Usamos una comprobación de umbral estricta: si ANTES no llegaba y AHORA sí (aproximado)
        if (usuario.nivel < 5) {
             // Esta parte es delicada porque no sabemos el nivel exacto "nuevo" aquí
             // Pero podemos apoyarnos en que si está a punto de subir, lo lanzamos
        }

        // Coleccionista (Especial)
        if (tipo == "RPG") {
            val itemsCount = repository.obtenerInventario(correo).size
            // Si tiene 2 y va a comprar el 3º (o ya tiene 3 y no se ha marcado)
            if (itemsCount >= 3 && !repository.esLogroDesbloqueado(correo, "Coleccionista")) {
                logrosCruzados.add("Coleccionista")
            }
        }

        // MOSTRAR TOASTS
        if (logrosCruzados.isNotEmpty()) {
            val listaDefinida = obtenerLogrosDefinidos(repository, usuario)
            withContext(Dispatchers.Main) {
                for (nombre in logrosCruzados) {
                    // Marcamos en DB (por si acaso no se marcó en la transacción)
                    repository.desbloquearLogro(correo, nombre)

                    val icono = listaDefinida.find { it.nombre == nombre }?.iconoResId ?: R.drawable.scroll
                    
                    // FEEDBACK VISUAL (Toast)
                    XpeandoToast.mostrarLogro(context, nombre, icono)
                    
                    // NOTIFICACIÓN DE SISTEMA (Barra superior)
                    NotificationHelper.enviarNotificacionLogro(
                        context, 
                        "¡Nuevo Logro Desbloqueado!", 
                        "Has conseguido: $nombre"
                    )

                    delay(4000)
                }
            }
        }
    }
}
