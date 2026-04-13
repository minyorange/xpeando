# 🛡️ Xpeando: Guía de Sistema y Mecánicas (v0.1)

**Xpeando** es un ecosistema RPG de productividad diseñado para transformar la disciplina diaria en una aventura heroica. Esta versión 0.1.2.13042026 consolida el sistema multiusuario, la economía interna y la progresión de combate.

---

## 👥 1. Arquitectura Multiusuario y Sesión
Xpeando garantiza la privacidad y el progreso individual mediante una arquitectura de aislamiento total.
*   **Identificación Única:** Todos los datos (tareas, inventario, progreso) se vinculan al `correo_usuario`.
*   **Gestión de Sesión:** Implementada en `LoginActivity.kt` y `MainActivity.kt`. La sesión persiste entre reinicios, pero permite el cierre seguro sin pérdida de configuraciones diarias.
*   **Base de Datos:** SQLite (v33) con limpieza automática de redundancias y soporte para apilado de objetos.

---

## 📊 2. Sistema de Atributos RPG (Impacto Real)
Los atributos no son solo números; afectan directamente a cada recompensa y penalización mediante multiplicadores.

| Atributo | Impacto en Gameplay | Cálculo de Bonificación |
| :--- | :--- | :--- |
| **FZA (Fuerza)** | Daño a Jefes | `Daño = Daño_Base * (Fuerza_Usuario + Bonus_Equipo/10)` |
| **INT (Inteligencia)** | Ganancia de XP | `XP_Final = XP_Base * (Int_Usuario + Bonus_Equipo/10)` |
| **PER (Percepción)** | Ganancia de Oro | `Oro_Final = Oro_Base * (Per_Usuario + Bonus_Equipo/10)` |
| **CON (Constitución)** | Resistencia al Daño | `Daño_Recibido = Daño_Base / (Con_Usuario + Bonus_Equipo/10)` |

---

## 🎁 3. Recompensa Diaria (Mini-juego RPG)
Un sistema de retención que premia la lealtad diaria con un mini-juego de azar probabilístico.
*   **Control de Fecha:** Sincronizado mediante `yyyy-MM-dd` para evitar duplicados.
*   **Probabilidades de Botín:**
    *   **Monedas (40%):** +100 Oro.
    *   **Bonus XP (35%):** +50 Experiencia.
    *   **Poción de Salud (20%):** Curación inmediata o almacenamiento en mochila.
    *   **Atributo (5%):** +1 Punto de Atributo gratuito.
*   **Persistencia:** El registro se guarda en el momento de la elección para prevenir abusos mediante reinicios de app.

---

## ⚡ 4. Sistema de Rachas (Streaks)
La constancia es premiada con bonificadores multiplicativos que se acumulan con los atributos del usuario.
*   **Mecánica:** Al completar tareas o hábitos positivos, se actualiza la racha diaria.
*   **Bonificadores:**
    *   **Racha de 3+ días:** +10% de oro y experiencia ganada.
    *   **Racha de 7+ días:** +25% de oro y experiencia ganada.

---

## 💀 5. Sistema de Muerte y Resurrección
Centralizado en `MainActivity.kt`. Al caer en combate (0 HP), el héroe puede elegir:
1.  **Uso de Poción:** Consume un objeto del inventario para recuperar HP.
2.  **Sacrificio de Oro:** Paga una multa en monedas para resucitar con HP parcial.
3.  **Resurrección Gratuita:** Recomienza con HP mínimo (10 HP) sin coste.

---

## ⚔️ 6. Ciclo de Tareas y Jefes
El juego divide la productividad en tres pilares:
*   **Hábitos:** Acciones repetitivas (+/-). Afectan HP y XP inmediatamente.
*   **Dailies:** Rutinas diarias. Si no se completan, el jugador recibe daño (mitigado por **Constitución**).
*   **Tareas:** Objetivos únicos. Al completarlas, se inflige daño al **Jefe Activo** basado en la **Fuerza**.

### Mecánica de Jefes (Per-User Boss)
*   **Instancia Personal:** Cada usuario tiene su propio jefe.
*   **Escalado de Dificultad:** Al derrotar a un jefe, este resucita tras 21 horas con **Nivel+, HP+ y Armadura+**.

---

## 🎒 7. Inventario y Equipo
*   **Stacking (Apilado):** Los objetos consumibles (Pociones) se apilan automáticamente.
*   **Bonus de Equipo:** Cada punto de bonus equivale a un **+0.1** al multiplicador del atributo.
*   **Equipamiento Inteligente:** Solo un objeto por subtipo (Arma, Armadura) puede estar equipado.

---

## 📂 8. Estructura del Proyecto (Tree)
```text
Xpeando/
├── app/src/main/java/com/example/xpeando/
│   ├── activities/      # Gestión de sesiones y diálogos globales
│   ├── adapters/        # Enlace de datos para Listas RPG
│   ├── database/        # Gestión de SQLite (DBHelper v33)
│   ├── fragments/       # UI Modular (Personaje, Tienda, Jefes)
│   ├── model/           # Entidades (Usuario, Jefe, Articulo)
│   └── utils/           # Lógica de Logros y Managers
└── README.md            # Documentación del sistema
```

---

## 🛠️ 9. Ficha Técnica
*   **Persistencia:** SQLite vía `DBHelper`.
*   **Versión de DB Actual:** 33.
*   **Arquitectura:** Single Activity con Navigation Component.
*   **Branding Visual:** Iconografía RPG personalizada (`dragon_pereza`, `pocion_vida`, `premios`).

---
*Actualizado el: 13/04/2026*
