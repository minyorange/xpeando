package com.example.xpeando.model

data class Usuario(
    val id: Int = 0, // En Firestore usaremos el correo o el UID como ID principal, pero mantenemos esto por compatibilidad
    val nombre: String = "",
    val correo: String = "",
    val nivel: Int = 1,
    val experiencia: Int = 0,
    val monedas: Int = 0,
    val hp: Int = 50,
    val fuerza: Double = 1.0,
    val inteligencia: Double = 1.0,
    val constitucion: Double = 1.0,
    val percepcion: Double = 1.0,
    val puntosDisponibles: Int = 0,
    val totalHabitos: Int = 0,
    val rachaActual: Int = 0,
    val rachaMaxima: Int = 0,
    val ultimaFechaActividad: String? = null,
    // Nuevos campos para Estadísticas y Notificaciones Cloud
    val totalTareasCompletadas: Int = 0,
    val totalDailiesCompletadas: Int = 0,
    val totalHabitosCompletados: Int = 0,
    val preferenciaNotificacion: String = "08:00",
    val ultimaSincronizacion: Long = System.currentTimeMillis(),
    val ultimaFechaConexion: String = "", // Nueva fecha para penalizaciones Cloud
    val ultimaFechaRecompensa: String = "", // Nueva fecha para recompensa diaria Cloud
    val tutorialVisto: Boolean = false // Nuevo: Para que no salga al reinstalar
)
