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
import com.example.xpeando.repository.DataRepository
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView

import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.xpeando.utils.NotificationHelper
import com.example.xpeando.viewmodel.UsuarioViewModel
import com.example.xpeando.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val repository = DataRepository()
    private val viewModel: UsuarioViewModel by viewModels { ViewModelFactory(repository) }
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

        tvNombre = findViewById(R.id.tv_header_nombre)
        tvNivel = findViewById(R.id.tv_header_nivel)
        cvRacha = findViewById(R.id.cv_racha)
        tvRacha = findViewById(R.id.tv_header_racha)
        tvMonedas = findViewById(R.id.tv_header_monedas)
        pbHP = findViewById(R.id.pb_header_hp)
        pbXP = findViewById(R.id.pb_header_xp)
        drawerLayout = findViewById(R.id.drawer_layout)

        observarUsuario()

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
            drawerLayout.closeDrawer(GravityCompat.START)
            when (menuItem.itemId) {
                R.id.item_salir -> {
                    mostrarDialogoCerrarSesion()
                    true
                }
                else -> {
                    val handled = NavigationUI.onNavDestinationSelected(menuItem, navController)
                    if (!handled) {
                        try {
                            navController.navigate(menuItem.itemId)
                            true
                        } catch (e: Exception) {
                            false
                        }
                    } else {
                        true
                    }
                }
            }
        }

        // --- INICIALIZAR NOTIFICACIONES ---
        NotificationHelper.createNotificationChannels(this)
        NotificationHelper.programarRecordatorioDiario(this)
        pedirPermisoNotificaciones()

        // --- COMPROBAR EVENTOS DE BIENVENIDA ---
        val prefsXpeando = getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE)
        val correo = prefsXpeando.getString("correo_usuario", "") ?: ""
        
        if (correo.isNotEmpty()) {
            val prefsTutorial = getSharedPreferences("TutorialPrefs", Context.MODE_PRIVATE)
            val tutorialVisto = prefsTutorial.getBoolean("tutorial_v2_visto_$correo", false)
            
            if (!tutorialVisto) {
                mostrarTutorialBienvenida(correo)
            } else {
                verificarRecompensaDiaria(correo)
            }
        }
    }

    private fun verificarRecompensaDiaria(correo: String) {
        val hoy = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val usuario = viewModel.usuario.value
        
        if (usuario != null && usuario.ultimaFechaRecompensa != hoy) {
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
            random < 40 -> "COINS" to "¡+100 Monedas!" 
            random < 75 -> "BONUS_XP" to "¡+50 XP!" 
            random < 95 -> "POCION" to "¡Poción Encontrada!" 
            else -> "ATRIBUTO" to "¡+1 Punto de Atributo!"
        }

        fun elegirCarta(card: View, imageView: android.widget.ImageView) {
            lifecycleScope.launch {
                repository.actualizarRecompensaDiaria(correo, hoy)
            }

            card1.isEnabled = false
            card2.isEnabled = false
            card3.isEnabled = false

            tvMensaje.text = premio.second
            tvMensaje.visibility = View.VISIBLE
            btnAceptar.visibility = View.VISIBLE

            when (premio.first) {
                "BONUS_XP" -> viewModel.actualizarProgreso(correo, 50, 0)
                "COINS" -> viewModel.actualizarProgreso(correo, 0, 100)
                "POCION" -> viewModel.comprarArticulo(correo, com.example.xpeando.model.Articulo(nombre = "Poción de Vida", tipo = "CONSUMIBLE", subtipo = "POCION", bonusHp = 25, icono = "pocion_vida"))
                "ATRIBUTO" -> viewModel.subirAtributo(correo, "fza")
            }

            card.animate().scaleX(1.1f).scaleY(1.1f).setDuration(300).withEndAction {
                card.animate().scaleX(1.0f).scaleY(1.0f).setDuration(300).start()
            }.start()
        }

        card1.setOnClickListener { elegirCarta(it, vista.findViewById(R.id.iv_card_1)) }
        card2.setOnClickListener { elegirCarta(it, vista.findViewById(R.id.iv_card_2)) }
        card3.setOnClickListener { elegirCarta(it, vista.findViewById(R.id.iv_card_3)) }

        btnAceptar.setOnClickListener { dialog.dismiss() }
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
                flipper.showNext()
                btnAtras.visibility = View.VISIBLE
            } else {
                getSharedPreferences("TutorialPrefs", Context.MODE_PRIVATE).edit().putBoolean("tutorial_v2_visto_$correo", true).apply()
                dialog.dismiss()
                verificarRecompensaDiaria(correo)
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
        val usuario = viewModel.usuario.value ?: return

        val dialogView = layoutInflater.inflate(R.layout.dialogo_muerte, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).setCancelable(false).create()

        dialogView.findViewById<Button>(R.id.btn_resucitar_pocion).setOnClickListener {
            viewModel.actualizarProgreso(correo, 0, 0, 50)
            dialog.dismiss()
            isDeathDialogShowing = false
        }

        dialogView.findViewById<Button>(R.id.btn_resucitar_monedas).apply {
            isEnabled = usuario.monedas >= 50
            setOnClickListener {
                viewModel.actualizarProgreso(correo, 0, -50, 25)
                dialog.dismiss()
                isDeathDialogShowing = false
            }
        }

        dialogView.findViewById<Button>(R.id.btn_resucitar_gratis).setOnClickListener {
            viewModel.actualizarProgreso(correo, 0, 0, 10)
            dialog.dismiss()
            isDeathDialogShowing = false
        }

        dialog.show()
    }

    private fun mostrarDialogoCerrarSesion() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro?")
            .setPositiveButton("Sí") { _, _ -> 
                val prefs = getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("sesion_activa", false).apply()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun observarUsuario() {
        val prefs = getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE)
        val correo = prefs.getString("correo_usuario", "") ?: ""
        
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.usuario.collect { usuario ->
                    usuario?.let {
                        tvNombre.text = it.nombre
                        tvNivel.text = "Nvl ${it.nivel}"
                        tvMonedas.text = "${it.monedas}"
                        tvRacha.text = "🔥 ${it.rachaActual}"
                        cvRacha.visibility = if (it.rachaActual > 0) View.VISIBLE else View.GONE

                        pbHP.progress = it.hp
                        pbXP.max = it.nivel * 100
                        pbXP.progress = it.experiencia
                        
                        if (it.hp <= 0 && !isDeathDialogShowing) {
                            mostrarDialogoMuerte()
                        }
                    }
                }
            }
        }
        viewModel.cargarUsuario(correo)
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
        val correo = getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE).getString("correo_usuario", "") ?: ""
        if (correo.isNotEmpty()) viewModel.cargarUsuario(correo)
    }
}
