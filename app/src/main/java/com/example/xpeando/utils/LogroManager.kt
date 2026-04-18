package com.example.xpeando.utils

import android.content.Context
import com.example.xpeando.R
import com.example.xpeando.model.Usuario
import com.example.xpeando.model.Logro
import com.example.xpeando.repository.DataRepository
import com.example.xpeando.repository.RpgRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

object LogroManager {

    suspend fun obtenerLogrosDefinidos(
        rpgRepository: RpgRepository, 
        usuario: Usuario
    ): List<Logro> {
        val totalTareas = usuario.totalTareasCompletadas
        val totalDailies = usuario.totalDailiesCompletadas
        val totalHabitos = usuario.totalHabitosCompletados
        val monedas = usuario.monedas
        val totalItems = rpgRepository.obtenerInventario(usuario.correo).size

        return listOf(
            Logro("Primeros Pasos", "Completa 1 misión", 1, totalTareas, totalTareas >= 1, R.drawable.pasos),
            Logro("Cazador de Misiones", "Completa 10 misiones", 10, totalTareas, totalTareas >= 10, R.drawable.misiones),
            Logro("Héroe Legendario", "Completa 50 misiones", 50, totalTareas, totalTareas >= 50, R.drawable.lengendario),
            Logro("Maestro de Hábitos", "Realiza 20 acciones de hábitos", 20, totalHabitos, totalHabitos >= 20, R.drawable.mhabitos),
            Logro("Rutina de Hierro", "Completa 5 dailies", 5, totalDailies, totalDailies >= 5, R.drawable.hierro),
            Logro("Inquebrantable", "Completa 30 dailies", 30, totalDailies, totalDailies >= 30, R.drawable.inquebrantable),
            Logro("Ascensión I", "Llega a nivel 5", 5, usuario.nivel, usuario.nivel >= 5, R.drawable.ascension),
            Logro("Ahorrador", "Consigue 500 monedas", 500, monedas, monedas >= 500, R.drawable.ahorrador),
            Logro("El Rey Midas", "Consigue 1000 monedas", 1000, monedas, monedas >= 1000, R.drawable.almondi),
            Logro("Coleccionista", "Ten 3 objetos en tu inventario", 3, totalItems, totalItems >= 3, R.drawable.colecionista),
            Logro("Constancia", "Mantén una racha de 3 días", 3, usuario.rachaActual, usuario.rachaActual >= 3, R.drawable.rey)
        )
    }

    suspend fun verificarNuevosLogros(
        context: Context, 
        userRepository: DataRepository,
        rpgRepository: RpgRepository,
        usuario: Usuario,
        tipo: String
    ) {
        val correo = usuario.correo
        val logrosCruzados = mutableListOf<String>()

        // 1. Tareas
        val t = usuario.totalTareasCompletadas
        if (t >= 1 && !userRepository.esLogroDesbloqueado(correo, "Primeros Pasos")) logrosCruzados.add("Primeros Pasos")
        if (t >= 10 && !userRepository.esLogroDesbloqueado(correo, "Cazador de Misiones")) logrosCruzados.add("Cazador de Misiones")
        if (t >= 50 && !userRepository.esLogroDesbloqueado(correo, "Héroe Legendario")) logrosCruzados.add("Héroe Legendario")

        // 2. Dailies
        val d = usuario.totalDailiesCompletadas
        if (d >= 5 && !userRepository.esLogroDesbloqueado(correo, "Rutina de Hierro")) logrosCruzados.add("Rutina de Hierro")
        if (d >= 30 && !userRepository.esLogroDesbloqueado(correo, "Inquebrantable")) logrosCruzados.add("Inquebrantable")

        // 3. Hábitos
        val h = usuario.totalHabitosCompletados
        if (h >= 20 && !userRepository.esLogroDesbloqueado(correo, "Maestro de Hábitos")) logrosCruzados.add("Maestro de Hábitos")

        // 4. Racha
        if (usuario.rachaActual >= 3 && !userRepository.esLogroDesbloqueado(correo, "Constancia")) {
            logrosCruzados.add("Constancia")
        }

        // 5. Nivel y Monedas
        if (usuario.nivel >= 5 && !userRepository.esLogroDesbloqueado(correo, "Ascensión I")) logrosCruzados.add("Ascensión I")
        if (usuario.monedas >= 500 && !userRepository.esLogroDesbloqueado(correo, "Ahorrador")) logrosCruzados.add("Ahorrador")
        if (usuario.monedas >= 1000 && !userRepository.esLogroDesbloqueado(correo, "El Rey Midas")) logrosCruzados.add("El Rey Midas")

        // 6. Coleccionista (Especial RPG)
        val itemsCount = rpgRepository.obtenerInventario(correo).size
        if (itemsCount >= 3 && !userRepository.esLogroDesbloqueado(correo, "Coleccionista")) {
            logrosCruzados.add("Coleccionista")
        }

        // MOSTRAR TOASTS Y NOTIFICACIONES
        if (logrosCruzados.isNotEmpty()) {
            val listaDefinida = obtenerLogrosDefinidos(rpgRepository, usuario)
            withContext(Dispatchers.Main) {
                for (nombre in logrosCruzados) {
                    userRepository.desbloquearLogro(correo, nombre)
                    val icono = listaDefinida.find { it.nombre == nombre }?.iconoResId ?: R.drawable.scroll
                    
                    XpeandoToast.mostrarLogro(context, nombre, icono)
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
