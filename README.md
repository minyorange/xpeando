# Xpeando - Tu Vida, Tu Aventura RPG

**Xpeando** es una aplicación de productividad gamificada para Android que transforma tus tareas diarias, hábitos y objetivos en una aventura de rol (RPG). Motívate completando tus deberes para subir de nivel, ganar oro, equipar a tu héroe y derrotar a los jefes de la procrastinación.

## 🚀 Características Principales

### ⚔️ Sistema de Progresión RPG
*   **Atributos Dinámicos:** Mejora tu personaje en cuatro áreas clave:
    *   **Fuerza:** Aumenta el daño que infliges a los jefes.
    *   **Inteligencia:** Multiplica la experiencia (XP) ganada.
    *   **Constitución:** Reduce el daño recibido al fallar tareas.
    *   **Percepción:** Incrementa la obtención de monedas.
*   **Niveles y XP:** Sube de nivel para obtener puntos de atributo y mejorar tus estadísticas.
*   **HP (Puntos de Vida):** ¡Cuidado! Fallar tus tareas diarias te restará vida.

### 📋 Gestión de Tareas
*   **Hábitos:** Acciones recurrentes que puedes completar varias veces al día para fortalecer atributos específicos.
*   **Dailies (Diarias):** Tareas que deben completarse cada día. Si las olvidas, recibirás daño.
*   **Tareas:** Objetivos de una sola vez con diferentes niveles de dificultad.
*   **Recompensas:** Canjea tus monedas ganadas por premios personalizados que tú mismo definas.

### 🎒 Inventario y Tienda
*   **Tienda RPG:** Compra equipo (espadas, escudos, pociones) para mejorar tus estadísticas.
*   **Inventario:** Gestiona y equipa tus objetos para prepararte para el combate.

### 🐲 Combate contra Jefes
*   Enfréntate a enemigos como el "Dragón de la Procrastinación". El daño que les infliges depende de tu constancia y tu nivel de Fuerza.

### 🏆 Logros y Estadísticas
*   Desbloquea medallas por tus hazañas.
*   Visualiza tu progreso histórico con un panel de estadísticas detallado.

## 🛠️ Tecnologías Utilizadas

*   **Lenguaje:** Kotlin
*   **Base de Datos:** SQLite (Local)
*   **Interfaz:** Material Design, View Binding.
*   **Navegación:** Jetpack Navigation Component (Bottom Navigation & Navigation Drawer).
*   **Arquitectura:** Basada en componentes estándar de Android (Activities, Fragments, Adapters, Models).

## 📂 Estructura del Proyecto

*   `activities/`: Actividades principales (Login, Registro, Main).
*   `fragments/`: Pantallas principales de la aplicación (Tareas, Personaje, Tienda, etc.).
*   `model/`: Clases de datos (Usuario, Tarea, Habito, Articulo, etc.).
*   `database/`: Gestión de la persistencia de datos con `DBHelper`.
*   `adapters/`: Adaptadores para los RecyclerViews de la interfaz.
*   `utils/`: Clases de utilidad.

## 📸 Capturas de Pantalla
*(Próximamente)*

---
**Xpeando** - ¡Convierte tu rutina en una misión épica!
