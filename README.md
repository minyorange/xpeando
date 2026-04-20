# 🛡️ Xpeando: Guía de Sistema y Mecánicas (v0.1.5)

**Xpeando** es un ecosistema RPG de productividad diseñado para transformar la disciplina diaria en una aventura heroica. Esta versión 0.1.5.18042026 consolida la migración total a la nube (Firestore), el desacoplamiento de repositorios y un sistema de combate totalmente integrado con la productividad.

---

## ☁️ 1. Arquitectura Cloud (Firestore)
Xpeando ha evolucionado a una arquitectura distribuida y escalable mediante **Firebase Firestore**.
*   **Identificación Única:** Todos los datos (tareas, inventario, progreso) se vinculan al `correo_usuario` como clave primaria de documento.
*   **Sincronización en Tiempo Real:** Uso de `SnapshotListeners` para que los cambios en la vida del jefe o recompensas se reflejen instantáneamente en todos los dispositivos.
*   **Persistencia Total:** Se ha eliminado la dependencia de SQLite local, garantizando que el progreso del héroe nunca se pierda.

---

## 🏗️ 2. Patrón de Repositorio (Desacoplamiento)
Para mejorar la mantenibilidad, el antiguo "God Repository" se ha dividido en cuatro motores especializados:
*   **`DataRepository`**: Núcleo del usuario (Perfil, XP, HP, Atributos y Racha). Usa transacciones atómicas para cálculos críticos.
*   **`RpgRepository`**: Gestión de combate contra jefes, tienda RPG, inventario y equipo.
*   **`TaskRepository`**: Administra el ciclo de vida de Tareas, Dailies y Hábitos.
*   **`NotesRepository`**: Gestión de notas rápidas y recordatorios personales.

---

## 📊 3. Sistema de Atributos RPG (Impacto Real)
Los atributos escalan las recompensas y mitigan las penalizaciones mediante multiplicadores dinámicos.

| Atributo | Impacto en Gameplay | Cálculo de Bonificación |
| :--- | :--- | :--- |
| **FZA (Fuerza)** | Daño a Jefes | `Daño = XP_Base * (Fuerza_Usuario + Bonus_Equipo/10)` |
| **INT (Inteligencia)** | Ganancia de XP | `XP_Final = XP_Base * (Int_Usuario + Bonus_Equipo/10)` |
| **PER (Percepción)** | Ganancia de Oro | `Oro_Final = Oro_Base * (Per_Usuario + Bonus_Equipo/10)` |
| **CON (Constitución)** | Resistencia al Daño | `Daño_Recibido = Daño_Base / (Con_Usuario + Bonus_Equipo/10)` |

---

## ⚔️ 4. Ciclo de Tareas y Jefes (Productividad = Combate)
El sistema de combate está ahora 100% vinculado a tus acciones en la vida real:
*   **Hábitos (+/-):** Afectan HP y XP inmediatamente. Los positivos (+) infligen daño automático al jefe.
*   **Dailies:** Si no se completan, el jugador recibe daño (mitigado por **Constitución**). Al completarlas, el jefe recibe un ataque potente.
*   **Tareas:** Objetivos únicos que otorgan grandes recompensas e infligen daño crítico al **Jefe Activo**.

### Mecánica de Jefes (Boss Respawn)
*   **Instancia Personal:** Cada usuario tiene su propio jefe en Firestore.
*   **Escalado de Dificultad:** Al derrotar a un jefe, este reaparece tras **21 horas** con estadísticas escaladas a tu nuevo nivel.

---

## 🎒 5. Inventario y Consumibles
*   **Gestión de Duplicados:** IDs únicos basados en el `hashCode` del nombre del ítem para evitar objetos repetidos.
*   **Mecánica de Pociones:** Uso permitido incluso con 0 HP para resucitar. Al usarlas, se consumen físicamente de la mochila en la nube.
*   **Bonus de Equipo:** Equipar un objeto aplica inmediatamente su bonus a los multiplicadores de combate y progreso.

---

## 📂 6. Estructura del Proyecto (v2)
```text
Xpeando/
├── app/src/main/java/com/example/xpeando/
│   ├── activities/      # Login, Registro y Main (Contenedores globales)
│   ├── repository/      # Los 4 pilares de datos (Data, Rpg, Task, Notes)
│   ├── viewmodel/       # Lógica de negocio reactiva y ViewModelFactory
│   ├── model/           # Entidades (Usuario, Jefe, Articulo, Tarea)
│   ├── fragments/       # UI Modular (Personaje, Tienda, Jefes, Estadísticas)
│   └── utils/           # LogroManager, NotificationHelper, Toasts
└── README.md            # Documentación del sistema
```

---

## 🛠️ 7. Ficha Técnica
*   **Backend:** Firebase Firestore + Auth.
*   **Arquitectura:** MVVM + Repository Pattern.
*   **Manejo de Errores:** Bloques `try-catch` y `Log.e` en flujos de datos asíncronos.
*   **Gráficos:** MPAndroidChart para estadísticas de progreso semanal.

---
*Actualizado el: 20/04/2026*
