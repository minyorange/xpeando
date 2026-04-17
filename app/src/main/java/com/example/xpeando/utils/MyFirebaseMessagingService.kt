package com.example.xpeando.utils

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Log.d("FCM", "Mensaje recibido de: ${remoteMessage.from}")

        // 1. Extraer datos si los hay
        var titulo = "Xpeando"
        var cuerpo = ""

        // Si viene como notificación estándar
        remoteMessage.notification?.let {
            titulo = it.title ?: titulo
            cuerpo = it.body ?: ""
        }

        // Si viene como datos (más fiable para pruebas)
        if (remoteMessage.data.isNotEmpty()) {
            titulo = remoteMessage.data["title"] ?: titulo
            cuerpo = remoteMessage.data["body"] ?: cuerpo
            Log.d("FCM", "Datos recibidos: $titulo - $cuerpo")
        }

        // Forzar envío de notificación local
        if (cuerpo.isNotEmpty()) {
            NotificationHelper.enviarNotificacionRecordatorio(this, titulo, cuerpo)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Nuevo token: $token")
    }
}
