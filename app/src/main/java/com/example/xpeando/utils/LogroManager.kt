package com.example.xpeando.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.xpeando.R
import com.example.xpeando.repository.DataRepository
import com.example.xpeando.model.Logro
import com.example.xpeando.model.Usuario

object LogroManager {

    fun obtenerLogrosDefinidos(repository: DataRepository, usuario: Usuario): List<Logro> {
        val totalTareas = repository.obtenerTotalTareasCompletadas(usuario.correo)
        val totalDailies = repository.obtenerTotalDailiesCompletadas(usuario.correo)
        val totalHabitos = repository.obtenerTodosHabitos(usuario.correo).size
        val totalItems = repository.obtenerInventario(usuario.correo).size
        val monedas = usuario.monedas

        val logros = listOf(
            // --- MISIONES ---
            Logro("Primeros Pasos", "Completa 1 misión", 1, totalTareas, totalTareas >= 1, R.drawable.pasos),
            Logro("Cazador de Misiones", "Completa 10 misiones", 10, totalTareas, totalTareas >= 10, R.drawable.misiones),
            Logro("Héroe Legendario", "Completa 50 misiones", 50, totalTareas, totalTareas >= 50, R.drawable.lengendario),
            // --- DAILIES ---
            Logro("Rutina de Hierro", "Completa 5 dailies", 5, totalDailies, totalDailies >= 5, R.drawable.hierro),
            Logro("Inquebrantable", "Completa 30 dailies", 30, totalDailies, totalDailies >= 30, R.drawable.inquebrantable),
            // --- HÁBITOS ---
            Logro("Maestro de Hábitos", "Realiza 20 acciones de hábitos", 20, totalHabitos, totalHabitos >= 20, R.drawable.mhabitos),
            // --- NIVEL / PERSONAJE ---
            Logro("Ascensión I", "Llega a nivel 5", 5, usuario.nivel, usuario.nivel >= 5, R.drawable.ascension),
            // --- ECONOMÍA ---
            Logro("Ahorrador", "Consigue 500 monedas", 500, monedas, monedas >= 500, R.drawable.colecionista),
            // --- COLECCIÓN ---
            Logro("Coleccionista", "Ten 3 objetos en tu inventario", 3, totalItems, totalItems >= 3, R.drawable.rey),
            // --- LOGRO SECRETO ---
            Logro(
                if (monedas >= 1000) "El Rey Midas" else "???",
                if (monedas >= 1000) "Consigue 1000 monedas" else "Logro oculto: Sigue explorando...",
                1000, monedas, monedas >= 1000,
                R.drawable.almondi
            )
        )

        // Sincronizar con la base de datos para asegurar que los completados estén registrados
        for (logro in logros) {
            if (logro.completado && !repository.esLogroDesbloqueado(usuario.correo, logro.nombre)) {
                repository.desbloquearLogro(usuario.correo, logro.nombre)
            }
        }

        return logros
    }

    fun verificarNuevosLogros(context: Context, repository: DataRepository, usuario: Usuario, estadisticaAnterior: Int, estadisticaNueva: Int, tipo: String) {
        var nombreLogro = ""

        when (tipo) {
            "TAREA" -> {
                if (estadisticaAnterior < 1 && estadisticaNueva >= 1) nombreLogro = "Primeros Pasos"
                if (estadisticaAnterior < 10 && estadisticaNueva >= 10) nombreLogro = "Cazador de Misiones"
                if (estadisticaAnterior < 50 && estadisticaNueva >= 50) nombreLogro = "Héroe Legendario"
            }
            "DAILY" -> {
                if (estadisticaAnterior < 5 && estadisticaNueva >= 5) nombreLogro = "Rutina de Hierro"
                if (estadisticaAnterior < 30 && estadisticaNueva >= 30) nombreLogro = "Inquebrantable"
            }
            "HABITO" -> {
                if (estadisticaAnterior < 20 && estadisticaNueva >= 20) nombreLogro = "Maestro de Hábitos"
            }
            "MONEDAS" -> {
                if (estadisticaAnterior < 500 && estadisticaNueva >= 500) nombreLogro = "Ahorrador"
                if (estadisticaAnterior < 1000 && estadisticaNueva >= 1000) nombreLogro = "El Rey Midas"
            }
            "NIVEL" -> {
                if (estadisticaAnterior < 5 && estadisticaNueva >= 5) nombreLogro = "Ascensión I"
            }
            "COLECCION" -> {
                if (estadisticaAnterior < 3 && estadisticaNueva >= 3) nombreLogro = "Coleccionista"
            }
        }

        if (nombreLogro.isNotEmpty()) {
            // Solo mostrar si no estaba desbloqueado previamente en la DB
            if (!repository.esLogroDesbloqueado(usuario.correo, nombreLogro)) {
                repository.desbloquearLogro(usuario.correo, nombreLogro)
                mostrarToastPersonalizado(context, nombreLogro)
                NotificationHelper.enviarNotificacionLogro(context, "¡Nuevo Logro Desbloqueado!", "Has conseguido: $nombreLogro")
            }
        }
    }

    private fun mostrarToastPersonalizado(context: Context, nombre: String) {
        val inflater = LayoutInflater.from(context)
        val layout: View = inflater.inflate(R.layout.layout_toast_logro, null)

        val text: TextView = layout.findViewById(R.id.tv_logro_toast_nombre)
        text.text = nombre

        with(Toast(context)) {
            duration = Toast.LENGTH_LONG
            view = layout
            show()
        }
    }
}
