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

        // --- TUTORIAL DE BIENVENIDA ---
        val prefsXpeando = getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE)
        val correo = prefsXpeando.getString("correo_usuario", "") ?: ""
        
        val prefsTutorial = getSharedPreferences("TutorialPrefs", Context.MODE_PRIVATE)
        val tutorialVisto = prefsTutorial.getBoolean("tutorial_visto_$correo", false)
        
        if (correo.isNotEmpty() && !tutorialVisto) {
            mostrarTutorialBienvenida(correo)
        }
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
        prefs.edit().clear().apply()

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
            }
        }
    }

    override fun onResume() {
        super.onResume()
        actualizarHeader()
    }
}
