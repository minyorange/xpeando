# 🛡️ Xpeando: RPG de Productividad (v0.2.0)

**Xpeando** es un ecosistema RPG de productividad diseñado para transformar la disciplina diaria en una aventura heroica. Esta versión consolida la migración total a la nube, el sistema de combate contra jefes y la gestión avanzada de cuenta.

---

## 🚀 Novedades de la Versión 0.2.0
*   **Gestión de Cuenta  Total:** Nueva sección de **Ajustes** con capacidad de **Borrado Permanente de Cuenta**, eliminando de forma recursiva todos los datos en Firestore y Firebase Auth.
*   **Sistema de Dailies (One-Shot):** Las tareas diarias ahora se comportan como misiones de un solo uso; al completarlas, se eliminan permanentemente para una lista siempre limpia.
*   **Corrección de Combate:** Sistema de protección contra "victorias infinitas". Los ataques solo cuentan si el jefe está vivo.
*   **Cronología de Jefes:** Historial de victorias ordenado cronológicamente y contador de reaparición de 21h sincronizado con la última muerte real.

---

## ☁️ 1. Arquitectura Cloud (Firestore)
El proyecto utiliza una estructura de documentos y subcolecciones para garantizar la integridad de los datos:
*   **Data Sincronizada:** Uso de `SnapshotListeners` para actualizaciones en tiempo real del HP del héroe y del jefe.
*   **Borrado Recursivo:** Implementación de limpieza manual de subcolecciones (`dailies`, `rpg`, `inventario`, etc.) para evitar datos huérfanos tras eliminar un usuario.

---

## 🏗️ 2. Patrón de Repositorio
El sistema se divide en cuatro motores especializados:
*   **`DataRepository`**: Gestión de perfil, XP, HP, Atributos y Racha.
*   **`RpgRepository`**: Lógica de combate, generación de jefes, tienda e inventario.
*   **`TaskRepository`**: Ciclo de vida de Tareas, Dailies y Hábitos.
*   **`NotesRepository`**: Gestión de notas rápidas y recordatorios.

---

## 📊 3. Sistema de Atributos RPG
| Atributo | Impacto en Gameplay |
| :--- | :--- |
| **FZA (Fuerza)** | Multiplica el daño infligido a los jefes. |
| **INT (Inteligencia)** | Aumenta el multiplicador de XP obtenida. |
| **PER (Percepción)** | Incrementa la ganancia de monedas de oro. |
| **CON (Constitución)** | Mitiga el daño recibido por tareas fallidas. |

---

## ⚔️ 4. El Ciclo de Combate
1.  **Atacar:** Completar Hábitos (+), Dailies o Tareas inflige daño al **Jefe Activo**.
2.  **Derrotar:** Al llegar a 0 HP, el jefe muere, se guarda en el **Historial de Victorias** y se otorgan grandes recompensas.
3.  **Reaparecer:** Tras 21 horas, un nuevo jefe escalado al nivel del jugador aparece automáticamente.
4.  **Penalización:** Las Dailies no realizadas dañan al jugador al final del día (mitigado por Constitución).

---

## 📂 5. Estructura Extendida del Proyecto
```text
Xpeando/
├── app/src/main/java/com/example/xpeando/
│   ├── activities/
│   │   ├── LoginActivity.kt        # Gestión de acceso y Auth inicial.
│   │   ├── RegistroActivity.kt     # Creación de nuevos héroes.
│   │   └── MainActivity.kt         # Contenedor principal y navegación.
│   ├── fragments/
│   │   ├── FragmentPersonaje.kt    # Visualización del héroe y atributos.
│   │   ├── FragmentJefes.kt        # Sistema de combate y respawn.
│   │   ├── FragmentDailies.kt      # Misiones diarias (One-shot).
│   │   ├── FragmentHabitos.kt      # Gestión de rutinas +/-.
│   │   ├── FragmentTareas.kt       # Listado de misiones únicas.
│   │   ├── FragmentRecompensas.kt  # Tienda de premios personales.
│   │   ├── FragmentLogros.kt       # Sala de trofeos y medallas.
│   │   ├── FragmentEstadisticas.kt # Gráficos de progresión.
│   │   ├── FragmentNotas.kt        # Notas rápidas de apoyo.
│   │   ├── FragmentAjustes.kt      # Configuración y borrado de cuenta.
│   │   └── FragmentFAQ.kt          # Guía de ayuda al usuario.
│   ├── viewmodel/
│   │   ├── UsuarioViewModel.kt     # Lógica central del usuario y Auth.
│   │   ├── RpgViewModel.kt         # Control de combate y jefes.
│   │   ├── DailiesViewModel.kt     # Gestión de tareas diarias.
│   │   ├── TareasViewModel.kt      # Lógica de misiones únicas.
│   │   ├── EstadisticasViewModel.kt# Procesamiento de datos para gráficos.
│   │   └── ViewModelFactory.kt     # Inyección de dependencias manual.
│   ├── repository/
│   │   ├── DataRepository.kt       # Transacciones de perfil y racha.
│   │   ├── RpgRepository.kt        # Acceso a jefes, inventario y tienda.
│   │   ├── TaskRepository.kt       # CRUD de misiones y rutinas.
│   │   └── NotesRepository.kt      # Almacenamiento de notas.
│   ├── adapters/
│   │   ├── DailiesAdapter.kt       # Visualización de listas diarias.
│   │   ├── HistorialJefesAdapter.kt# Renderizado de victorias pasadas.
│   │   └── InventarioAdapter.kt    # Gestión visual de equipo.
│   ├── model/
│   │   ├── Usuario.kt | Jefe.kt    # Entidades principales.
│   │   ├── Daily.kt | Tarea.kt     # Modelos de misión.
│   │   └── Articulo.kt | Logro.kt  # Economía y hitos.
│   └── utils/
│       ├── LogroManager.kt         # Sistema de detección de hitos.
│       ├── NotificationHelper.kt   # Motor de alertas y recordatorios.
│       └── XpeandoToast.kt         # UI de feedback personalizado.
├── app/src/main/res/
│   ├── layout/                         # Definición de interfaces XML (UI).
│   │   ├── activities/
│   │   │   ├── activity_main.xml       # Contenedor con NavigationDrawer.
│   │   │   ├── activity_login.xml      # Pantalla de acceso.
│   │   │   └── activity_registro.xml   # Pantalla de creación de cuenta.
│   │   ├── fragments/
│   │   │   ├── fragment_personaje.xml  # Dashboard del héroe y stats.
│   │   │   ├── fragment_jefes.xml      # Arena de combate.
│   │   │   ├── fragment_dailies.xml    # Lista de misiones diarias.
│   │   │   ├── fragment_habitos.xml    # Tracker de hábitos.
│   │   │   ├── fragment_tareas.xml     # Tablón de misiones únicas.
│   │   │   ├── fragment_recompensas.xml# Catálogo de la tienda.
│   │   │   ├── fragment_logros.xml     # Vitrina de medallas.
│   │   │   ├── fragment_estadisticas.xml# Gráficos de rendimiento.
│   │   │   ├── fragment_notas.xml      # Bloc de notas.
│   │   │   ├── fragment_ajustes.xml    # Panel de configuración.
│   │   │   └── fragment_faq.xml        # Guía de usuario.
│   │   ├── items/                      # Filas para RecyclerViews.
│   │   │   ├── item_daily.xml | item_habito.xml | item_tarea.xml
│   │   │   ├── item_inventario.xml | item_recompensa.xml
│   │   │   ├── item_nota.xml | item_logro.xml
│   │   │   └── item_historial_jefe.xml
│   │   ├── dialogos/                   # Ventanas emergentes y modales.
│   │   │   ├── dialogo_muerte.xml | dialogo_subida_nivel.xml
│   │   │   ├── dialogo_nueva_daily.xml | dialogo_nuevo_habito.xml
│   │   │   ├── dialogo_nueva_tarea.xml | dialogo_nueva_recompensa.xml
│   │   │   ├── dialogo_mochila.xml | dialogo_tutorial_*.xml
│   │   │   └── dialogo_recompensa_diaria.xml
│   │   └── toasts/                     # Feedback visual personalizado.
│   │       ├── layout_custom_toast.xml
│   │       ├── layout_toast_logro.xml
│   │       └── layout_toast_progreso.xml
│   ├── drawable/                       # Recursos gráficos y formas.
│   ├── menu/                           # Menú lateral y barra inferior.
│   ├── navigation/                     # Grafo de navegación (Jetpack Nav).
│   └── values/                         # Themes, Colors y Strings.
└── README.md
```

---

## 🛠️ Tecnologías
*   **Lenguaje:** Kotlin.
*   **Base de Datos:** Firebase Firestore.
*   **Autenticación:** Firebase Auth.
*   **UI:** Material Design + Custom Views.
*   **Gráficos:** MPAndroidChart.

---
*Última actualización: 20 de Abril 20226*
