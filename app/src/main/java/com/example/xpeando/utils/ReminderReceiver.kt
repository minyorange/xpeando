package com.example.xpeando.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.xpeando.database.DBHelper
import com.example.xpeando.repository.DataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val repository = DataRepository(DBHelper(context))
        val prefs = context.getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE)
        val correo = prefs.getString("correo_usuario", "") ?: ""

        if (correo.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                val dailies = repository.obtenerTodasDailies(correo)
                val pendientes = dailies.count { !it.completadaHoy }

                if (pendientes > 0) {
                    NotificationHelper.enviarNotificacionRecordatorio(
                        context,
                        "¡No olvides tus Dailies!",
                        "Tienes $pendientes tareas diarias pendientes por completar hoy."
                    )
                }
            }
        }
        
        // Re-programar para el día siguiente si es necesario (aunque el Helper lo hace al iniciar la app)
        NotificationHelper.programarRecordatorioDiario(context)
    }
}
