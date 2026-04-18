package com.example.xpeando.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.xpeando.repository.RepositoryProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val repository = RepositoryProvider.taskRepository
        val prefs = context.getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE)
        val correo = prefs.getString("correo_usuario", "") ?: ""
        val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        if (correo.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val dailies = repository.obtenerTodasDailies(correo)
                    // Comprobamos la fecha real de Firestore para cada daily
                    val pendientes = dailies.count { it.ultimaVezCompletada != hoy }

                    if (pendientes > 0) {
                        NotificationHelper.enviarNotificacionRecordatorio(
                            context,
                            "¡Atención Héroe!",
                            "Tienes $pendientes misiones diarias pendientes. ¡No dejes que el Dragón de la Pereza gane hoy!"
                        )
                    } else if (pendientes == 0 && dailies.isNotEmpty()) {
                        // Opcional: Notificación de felicitación si ya hizo todo
                        // NotificationHelper.enviarNotificacionLogro(context, "¡Todo listo!", "Has cumplido con tus deberes de hoy.")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        
        // Reprogramar la alarma para mañana a la misma hora
        NotificationHelper.programarRecordatorioDiario(context)
    }
}
