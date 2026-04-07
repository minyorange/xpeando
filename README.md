# 🛡️ Xpeando: Guía de Sistema y Mecánicas (v1.2)

**Xpeando** ha evolucionado de una simple lista de tareas a un **RPG de productividad completo**. Este documento detalla la lógica matemática, el sistema de progresión y la arquitectura técnica actualizada.

---

## 📊 1. Sistema de Atributos RPG (Impacto Real)
Los atributos no son solo números; afectan directamente a cada recompensa y penalización mediante multiplicadores.

| Atributo | Nombre | Impacto en Gameplay | Fórmula de Cálculo |
| :--- | :--- | :--- | :--- |
| **FZA** | Fuerza | Daño a los Jefes | `Daño_Final = Daño_Base * (Fza_Base + Bono_Equipo/10)` |
| **INT** | Inteligencia | Multiplicador de XP | `XP_Final = XP_Base * (Int_Base + Bono_Equipo/10)` |
| **PER** | Percepción | Multiplicador de Oro | `Oro_Final = Oro_Base * (Per_Base + Bono_Equipo/10)` |
| **CON** | Constitución | Escudo de Salud | `Daño_Recibido = Daño_Base / (Con_Base + Bono_Equipo/10)` |

> **Nota de Diseño:** Los multiplicadores se muestran en la UI redondeados a **1 decimal** (ej: 1.5x) para mantener una estética limpia y evitar el "min-maxing" excesivo por parte del jugador.

---

## 💀 2. Sistema de Muerte y Resurrección
Centralizado en `MainActivity.kt` para garantizar que el jugador nunca se quede bloqueado sin HP.

### Activación
Se dispara automáticamente cuando `HP <= 0`. El diálogo es modal y bloquea el avance hasta que se elige una opción.

### Opciones de Resurrección:
1.  **Poción de Vida:** Usa un objeto del inventario (subtipo `POCION`). Restaura **50 HP**.
2.  **Sacrificio de Oro:** Coste de **50 Monedas**. Restaura **25 HP**.
3.  **Resurrección Débil:** Gratis. Restaura **10 HP**.

---

## ⚔️ 3. Ciclo de Tareas y Jefes
El juego divide la productividad en tres pilares:

*   **Hábitos:** Acciones repetitivas (+/-). Afectan HP y XP inmediatamente.
*   **Dailies:** Rutinas diarias. Si no se completan al final del día, el jugador recibe daño (mitigado por **Constitución**).
*   **Tareas:** Objetivos únicos. Al completarlas, se inflige daño al **Jefe Activo** basado en la **Fuerza**.

### Mecánica de Jefes
*   Los jefes tienen **Armadura** que resta daño base.
*   Al derrotar un jefe, se desbloquean recompensas masivas de XP y Oro.
*   Se registra la fecha de derrota para estadísticas.

---

## 🎒 4. Inventario y Equipo
El sistema de equipo está vinculado directamente a los atributos.

*   **Bonus de Equipo:** Cada punto de bonus en un objeto (ej: Espada +2 FZA) equivale a un **+0.1** al multiplicador del atributo correspondiente.
*   **Gestión:** Los objetos se compran en la **Tienda** y se gestionan desde la **Mochila** (`FragmentPersonaje`).
*   **Consumibles:** Las pociones no se equipan, se "usan" directamente para recuperar HP.

---

## 📱 5. Estructura de Navegación
Organizada para separar la gestión rápida del análisis de progreso:

### Navegación Inferior (Acción Diaria)
*   **Hábitos:** El centro del día a día.
*   **Dailies:** Listado de tareas recurrentes.
*   **Tareas:** Lista de quehaceres únicos.
*   **Tienda:** Gasto de monedas en equipo y consumibles.

### Menú Lateral / Drawer (Progreso del Héroe)
*   **Mi Héroe:** Perfil, atributos y mochila (Tooltips informativos).
*   **Misiones de Jefe:** Estado del combate actual y lista de derrotados.
*   **Sala de Trofeos:** Logros desbloqueados.
*   **Estadísticas:** Gráficos de rendimiento.

---

## 🛠️ 6. Ficha Técnica
*   **Persistencia:** SQLite vía `DBHelper`.
*   **Versión de DB Actual:** 24 (Soporte para multiplicadores y equipo).
*   **Arquitectura:** Single Activity (`MainActivity`) con Navigation Component.
*   **Feedback:** Custom Tooltips mediante `AlertDialog` y Snackbars personalizados para recompensas.

---
*Actualizado el: 07/04/2026*
