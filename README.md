# 🛡️ Xpeando: Guía de Sistema y Mecánicas (v2.1)

**Xpeando** ha evolucionado de una simple lista de tareas a un **RPG de productividad completo y multiusuario**. Este documento detalla la lógica matemática, el sistema de progresión y la arquitectura técnica actualizada.

---

## 👥 1. Aislamiento de Datos por Usuario
A partir de la versión 27 de la base de datos, Xpeando soporta múltiples cuentas con total privacidad.
*   **Identificación:** Cada registro en las tablas `habitos`, `tareas`, `dailies`, `recompensas`, `inventario`, `jefes` y `logros` está vinculado a un `correo_usuario`.
*   **Persistencia Local:** Los datos se filtran dinámicamente según la sesión activa, permitiendo que varios héroes compartan el mismo dispositivo sin interferir en sus misiones.

---

## 📊 2. Sistema de Atributos RPG (Impacto Real)
Los atributos no son solo números; afectan directamente a cada recompensa y penalización mediante multiplicadores.

| Atributo | Nombre | Impacto en Gameplay | Fórmula de Cálculo |
| :--- | :--- | :--- | :--- |
| **FZA** | Fuerza | Daño a los Jefes | `Danio_Final = Danio_Base * (Fza_Base + Bono_Equipo/10)` |
| **INT** | Inteligencia | Multiplicador de XP | `XP_Final = XP_Base * (Int_Base + Bono_Equipo/10)` |
| **PER** | Percepción | Multiplicador de Oro | `Oro_Final = Oro_Base * (Per_Base + Bono_Equipo/10)` |
| **CON** | Constitución | Escudo de Salud | `Danio_Recibido = Danio_Base / (Con_Base + Bono_Equipo/10)` |

---

## ⚡ 3. Sistema de Rachas (Streaks)
La constancia es premiada con bonificadores multiplicativos que se acumulan con los atributos del usuario.
*   **Mecánica:** Al completar tareas o hábitos positivos, se actualiza la racha diaria.
*   **Bonificadores:**
    *   **Racha de 3+ días:** +10% de oro y experiencia ganada.
    *   **Racha de 7+ días:** +25% de oro y experiencia ganada.
*   **Visualización:** El estado de la racha actual y máxima se muestra en el perfil del héroe y en las estadísticas.

---

## 💀 4. Sistema de Muerte y Resurrección
Centralizado en `MainActivity.kt` para garantizar que el jugador nunca se quede bloqueado sin HP.

### Activación
Se dispara automáticamente cuando `HP <= 0`. El diálogo es modal y bloquea el avance hasta que se elige una opción.

---

## ⚔️ 5. Ciclo de Tareas y Jefes
El juego divide la productividad en tres pilares:

*   **Hábitos:** Acciones repetitivas (+/-). Afectan HP y XP inmediatamente.
*   **Dailies:** Rutinas diarias. Si no se completan al final del día, el jugador recibe daño (mitigado por **Constitución**).
*   **Tareas:** Objetivos únicos. Al completarlas, se inflige daño al **Jefe Activo** basado en la **Fuerza**.

### Mecánica de Jefes (Per-User Boss)
*   **Instancia Personal:** Cada usuario tiene su propio jefe con su progreso de HP independiente.
*   **Escalado de Dificultad:** Al derrotar a un jefe, este entra en un periodo de reaparición (21h). Al volver, sube de **Nivel**, ganando **Armadura** y **HP máximo**.

---

## 🎒 6. Inventario y Equipo
*   **Stacking (Apilado):** Los objetos consumibles (Pociones) se apilan bajo una propiedad `cantidad`.
*   **Bonus de Equipo:** Cada punto de bonus en un objeto (ej: Espada +2 FZA) equivale a un **+0.1** al multiplicador del atributo correspondiente.

---

## 📂 7. Estructura del Proyecto (Tree)
```text
Xpeando/
├── app/src/main/java/com/example/xpeando/
│   ├── activities/      # Actividades principales (Login, Registro, Main)
│   ├── adapters/        # Adaptadores de RecyclerView (Habitos, Tareas, Logros, etc.)
│   ├── database/        # Gestión de SQLite (DBHelper)
│   ├── fragments/       # Fragmentos de UI (Personaje, Jefes, Tienda, Estadísticas)
│   ├── model/           # Clases de datos (Usuario, Jefe, Habito, Tarea, Articulo)
│   └── utils/           # Clases de utilidad (LogroManager)
├── app/src/main/res/
│   ├── layout/          # Definiciones de interfaz XML
│   ├── menu/            # Menús de navegación (Bottom y Drawer)
│   └── drawable/        # Iconos y recursos visuales del RPG
└── README.md            # Guía del sistema y mecánicas
```

---

## 🛠️ 8. Ficha Técnica
*   **Persistencia:** SQLite vía `DBHelper`.
*   **Versión de DB Actual:** 27 (Aislamiento de datos y escalado de jefes).
*   **Arquitectura:** Single Activity (`MainActivity`) con Navigation Component.
*   **Compatibilidad:** Código refactorizado para evitar caracteres no ASCII (ñ) en identificadores técnicos.

---
*Actualizado el: 08/04/2026*
