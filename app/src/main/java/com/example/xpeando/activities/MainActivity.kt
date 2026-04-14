package com.example.xpeando.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.example.xpeando.R
import com.example.xpeando.database.DBHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView

import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.example.xpeando.utils.NotificationHelper

class MainActivity : AppCompatActivity() {

    private lateinit var db: DBHelper
    private lateinit var tvNombre: TextView
    private lateinit var tvNivel: TextView
    private lateinit var cvRacha: View
    private lateinit var tvRacha: TextView
    private lateinit var tvMonedas: TextView
    private lateinit var pbHP: ProgressBar
    private lateinit var pbXP: ProgressBar
    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = DBHelper(this)

        tvNombre = findViewById(R.id.tv_header_nombre)
        tvNivel = findViewById(R.id.tv_header_nivel)
        cvRacha = findViewById(R.id.cv_racha)
        tvRacha = findViewById(R.id.tv_header_racha)
        tvMonedas = findViewById(R.id.tv_header_monedas)
        pbHP = findViewById(R.id.pb_header_hp)
        pbXP = findViewById(R.id.pb_header_xp)
        drawerLayout = findViewById(R.id.drawer_layout)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.contenedor_fragmentos) as NavHostFragment
        val navController = navHostFragment.navController
        val bottomNav = findViewById<BottomNavigationView>(R.id.navegacion_inferior)
        bottomNav.setupWithNavController(navController)

        val navView = findViewById<NavigationView>(R.id.nav_view)
        navView.setupWithNavController(navController)

        findViewById<ImageButton>(R.id.btn_menu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.item_salir -> {
                    mostrarDialogoCerrarSesion()
                    true
                }
                else -> {
                    val handled = NavigationUI.onNavDestinationSelected(menuItem, navController)
                    if (handled) drawerLayout.closeDrawer(GravityCompat.START)
                    handled
                }
            }
        }

        actualizarHeader()

        // --- INICIALIZAR NOTIFICACIONES ---
        NotificationHelper.createNotificationChannels(this)
        NotificationHelper.programarRecordatorioDiario(this)
        pedirPermisoNotificaciones()

        // --- TUTORIAL DE BIENVENIDA ---
        val prefsXpeando = getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE)
        val correo = prefsXpeando.getString("correo_usuario", "") ?: ""
        
        val prefsTutorial = getSharedPreferences("TutorialPrefs", Context.MODE_PRIVATE)
        val tutorialVisto = prefsTutorial.getBoolean("tutorial_visto_$correo", false)
        
        if (correo.isNotEmpty() && !tutorialVisto) {
            mostrarTutorialBienvenida(correo)
        } else if (correo.isNotEmpty()) {
            verificarRecompensaDiaria(correo)
        }
    }

    private fun verificarRecompensaDiaria(correo: String) {
        val prefs = getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE)
        val hoy = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val ultimaRecompensa = prefs.getString("ultima_recompensa_diaria_$correo", "")

        if (ultimaRecompensa != hoy) {
            mostrarDialogoRecompensaDiaria(correo, hoy)
        }
    }

    private fun mostrarDialogoRecompensaDiaria(correo: String, hoy: String) {
        val vista = layoutInflater.inflate(R.layout.dialogo_recompensa_diaria, null)
        val dialog = AlertDialog.Builder(this)
            .setView(vista)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val card1 = vista.findViewById<View>(R.id.card_1)
        val card2 = vista.findViewById<View>(R.id.card_2)
        val card3 = vista.findViewById<View>(R.id.card_3)
        val tvMensaje = vista.findViewById<TextView>(R.id.tv_recompensa_mensaje)
        val btnAceptar = vista.findViewById<Button>(R.id.btn_recompensa_aceptar)

        val random = java.util.Random().nextInt(100)
        val premio = when {
            random < 40 -> "COINS" to "¡+100 Monedas de oro!" // 40%
            random < 75 -> "BONUS_XP" to "¡Bonus de +50 XP!" // 35%
            random < 95 -> "POCION" to "¡Has encontrado una Poción!" // 20%
            else -> "ATRIBUTO" to "¡+1 Punto de Atributo!" // 5%
        }

        fun elegirCarta(card: View, imageView: android.widget.ImageView) {
            // Guardar que ya se reclamó hoy inmediatamente para evitar duplicados por cierres inesperados
            getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE).edit()
                .putString("ultima_recompensa_diaria_$correo", hoy)
                .apply()

            // Deshabilitar clics para no elegir más de una
            card1.isEnabled = false
            card2.isEnabled = false
            card3.isEnabled = false

            tvMensaje.text = premio.second
            tvMensaje.visibility = View.VISIBLE
            btnAceptar.visibility = View.VISIBLE

            when (premio.first) {
                "BONUS_XP" -> {
                    db.actualizarProgresoUsuario(correo, 50, 0)
                    imageView.setImageResource(R.drawable.experiencia)
                }
                "POCION" -> {
                    db.regalarPocion(correo)
                    imageView.setImageResource(R.drawable.pocion_vida)
                }
                "COINS" -> {
                    db.actualizarProgresoUsuario(correo, 0, 100)
                    imageView.setImageResource(R.drawable.coins)
                }
                "ATRIBUTO" -> {
                    db.actualizarAtributos(correo, puntosUsados = -1)
                    imageView.setImageResource(R.drawable.fuerza)
                }
            }

            // Animación simple de escala al revelar
            card.animate().scaleX(1.1f).scaleY(1.1f).setDuration(300).withEndAction {
                card.animate().scaleX(1.0f).scaleY(1.0f).setDuration(300).start()
            }.start()
        }

        card1.setOnClickListener { elegirCarta(it, vista.findViewById(R.id.iv_card_1)) }
        card2.setOnClickListener { elegirCarta(it, vista.findViewById(R.id.iv_card_2)) }
        card3.setOnClickListener { elegirCarta(it, vista.findViewById(R.id.iv_card_3)) }

        btnAceptar.setOnClickListener {
            dialog.dismiss()
            actualizarHeader()
        }

        dialog.show()
    }

    private fun mostrarTutorialBienvenida(correo: String) {
        val vista = layoutInflater.inflate(R.layout.dialogo_tutorial_bienvenida, null)
        val flipper = vista.findViewById<android.widget.ViewFlipper>(R.id.view_flipper_bienvenida)
        val btnAtras = vista.findViewById<Button>(R.id.btn_bienvenida_anterior)
        val btnSig = vista.findViewById<Button>(R.id.btn_bienvenida_siguiente)

        val dialog = AlertDialog.Builder(this)
            .setView(vista)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnSig.setOnClickListener {
            if (flipper.displayedChild < flipper.childCount - 1) {
                flipper.setInAnimation(this, R.anim.slide_in_right)
                flipper.setOutAnimation(this, R.anim.slide_out_left)
                flipper.showNext()
                btnAtras.visibility = View.VISIBLE
                if (flipper.displayedChild == flipper.childCount - 1) {
                    btnSig.text = "¡Empezar Aventura!"
                }
            } else {
                val prefsTutorial = getSharedPreferences("TutorialPrefs", Context.MODE_PRIVATE)
                prefsTutorial.edit().putBoolean("tutorial_visto_$correo", true).apply()
                dialog.dismiss()
                mostrarTutorialAtributos(correo)
            }
        }

        btnAtras.setOnClickListener {
            if (flipper.displayedChild > 0) {
                flipper.setInAnimation(this, R.anim.slide_in_left)
                flipper.setOutAnimation(this, R.anim.slide_out_right)
                flipper.showPrevious()
                btnSig.text = "Siguiente"
                if (flipper.displayedChild == 0) {
                    btnAtras.visibility = View.INVISIBLE
                }
            }
        }

        dialog.show()
    }

    private fun mostrarTutorialAtributos(correo: String) {
        val vista = layoutInflater.inflate(R.layout.dialogo_tutorial_atributos, null)
        val flipper = vista.findViewById<android.widget.ViewFlipper>(R.id.view_flipper_tutorial)
        val btnAtras = vista.findViewById<Button>(R.id.btn_tutorial_anterior)
        val btnSig = vista.findViewById<Button>(R.id.btn_tutorial_siguiente)

        val dialog = AlertDialog.Builder(this)
            .setView(vista)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnSig.setOnClickListener {
            if (flipper.displayedChild < flipper.childCount - 1) {
                flipper.setInAnimation(this, R.anim.slide_in_right)
                flipper.setOutAnimation(this, R.anim.slide_out_left)
                flipper.showNext()
                btnAtras.visibility = View.VISIBLE
                if (flipper.displayedChild == flipper.childCount - 1) {
                    btnSig.text = "¡Todo Claro!"
                }
            } else {
                val prefsTutorial = getSharedPreferences("TutorialPrefs", Context.MODE_PRIVATE)
                prefsTutorial.edit().putBoolean("tutorial_atributos_visto_$correo", true).apply()
                dialog.dismiss()
                verificarRecompensaDiaria(correo) // Comprobar recompensa tras el tutorial
            }
        }

        btnAtras.setOnClickListener {
            if (flipper.displayedChild > 0) {
                flipper.setInAnimation(this, R.anim.slide_in_left)
                flipper.setOutAnimation(this, R.anim.slide_out_right)
                flipper.showPrevious()
                btnSig.text = "Siguiente"
                if (flipper.displayedChild == 0) {
                    btnAtras.visibility = View.INVISIBLE
                }
            }
        }

        dialog.show()
    }

    private var isDeathDialogShowing = false

    fun mostrarDialogoMuerte() {
        if (isDeathDialogShowing) return
        isDeathDialogShowing = true

        val prefs = getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE)
        val correo = prefs.getString("correo_usuario", "") ?: ""
        val usuario = db.obtenerUsuarioLogueado(correo) ?: return

        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialogo_muerte, null)
        builder.setView(dialogView)
        builder.setCancelable(false)

        val dialog = builder.create()

        dialogView.findViewById<Button>(R.id.btn_resucitar_pocion).apply {
            val pociones = db.obtenerInventario(correo).filter { it.tipo == "CONSUMIBLE" && it.subtipo == "POCION" }
            isEnabled = pociones.isNotEmpty()
            val pocion = pociones.firstOrNull()
            if (pocion != null) {
                text = "Usar ${pocion.nombre} (+${pocion.bonusHp} HP)"
                setOnClickListener {
                    db.actualizarProgresoUsuario(correo, 0, 0, pocion.bonusHp)
                    db.eliminarDelInventario(pocion.id)
                    finalizarMuerte(dialog)
                }
            } else {
                text = "Sin Pociones"
            }
        }

        dialogView.findViewById<Button>(R.id.btn_resucitar_monedas).apply {
            val costo = 50
            text = "Pagar $costo (+25 HP)"
            isEnabled = usuario.monedas >= costo
            setOnClickListener {
                db.actualizarProgresoUsuario(correo, 0, -costo, 25)
                finalizarMuerte(dialog)
            }
        }

        dialogView.findViewById<Button>(R.id.btn_resucitar_gratis).setOnClickListener {
            db.actualizarProgresoUsuario(correo, 0, 0, 10)
            finalizarMuerte(dialog)
        }

        dialog.show()
    }

    private fun finalizarMuerte(dialog: AlertDialog) {
        dialog.dismiss()
        isDeathDialogShowing = false
        actualizarHeader()
        
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.contenedor_fragmentos) as NavHostFragment
        val currentFragment = navHostFragment.childFragmentManager.fragments.firstOrNull()
        if (currentFragment is com.example.xpeando.fragments.FragmentHabitos) {
            currentFragment.actualizarLista()
        }
    }

    private fun mostrarDialogoCerrarSesion() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro de que quieres salir?")
            .setPositiveButton("Sí") { _, _ -> cerrarSesion() }
            .setNegativeButton("No", null)
            .show()
    }

    private fun cerrarSesion() {
        val prefs = getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("sesion_activa", false)
            .apply()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    fun actualizarHeader() {
        val prefs = getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE)
        val correo = prefs.getString("correo_usuario", "") ?: ""
        
        val usuario = db.obtenerUsuarioLogueado(correo)
        usuario?.let {
            tvNombre.text = it.nombre
            tvNivel.text = "Nvl ${it.nivel}"
            
            if (it.rachaActual > 0) {
                tvRacha.text = "🔥 ${it.rachaActual}"
                cvRacha.visibility = View.VISIBLE
            } else {
                cvRacha.visibility = View.GONE
            }

            tvMonedas.text = "${it.monedas}"

            pbHP.progress = it.hp
            pbXP.progress = it.experiencia.toInt()

            if (it.hp <= 0 && !isDeathDialogShowing) {
                mostrarDialogoMuerte()
            }

            val navView = findViewById<NavigationView>(R.id.nav_view)
            if (navView.headerCount > 0) {
                val headerView = navView.getHeaderView(0)
                headerView.findViewById<TextView>(R.id.tv_nav_header_nombre)?.text = it.nombre
                headerView.findViewById<TextView>(R.id.tv_nav_header_nivel)?.text = "Nivel ${it.nivel}"
                
                // Actualizar barras de vida y experiencia en el menú lateral
                headerView.findViewById<ProgressBar>(R.id.pb_nav_hp)?.progress = it.hp
                headerView.findViewById<ProgressBar>(R.id.pb_nav_xp)?.progress = it.experiencia.toInt()
            }
        }
    }

    private fun pedirPermisoNotificaciones() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        actualizarHeader()
    }
}
