package com.example.xpeando.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
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
    private lateinit var pbHP: ProgressBar
    private lateinit var pbXP: ProgressBar
    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = DBHelper(this)

        drawerLayout = findViewById(R.id.drawer_layout)
        val btnMenu = findViewById<ImageButton>(R.id.btn_menu)
        val navView = findViewById<NavigationView>(R.id.nav_view)
        val bottomNav = findViewById<BottomNavigationView>(R.id.navegacion_inferior)

        // Inicializar vistas del header superior
        tvNombre = findViewById(R.id.tv_header_nombre)
        tvNivel = findViewById(R.id.tv_header_nivel)
        pbHP = findViewById(R.id.pb_header_hp)
        pbXP = findViewById(R.id.pb_header_xp)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.contenedor_fragmentos) as NavHostFragment
        val navController = navHostFragment.navController
        
        // Configuración automática para BottomNav
        bottomNav.setupWithNavController(navController)

        // Configuración manual para el Drawer
        navView.setNavigationItemSelectedListener { menuItem ->
            val handled = when (menuItem.itemId) {
                R.id.item_salir -> {
                    mostrarDialogoCerrarSesion()
                    true
                }
                else -> {
                    NavigationUI.onNavDestinationSelected(menuItem, navController)
                }
            }
            
            if (handled) {
                drawerLayout.closeDrawer(GravityCompat.START)
            }
            handled
        }

        btnMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        navController.addOnDestinationChangedListener { _, _, _ ->
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            }
        }

        actualizarHeader()
    }

    private var isDeathDialogShowing = false

    fun mostrarDialogoMuerte() {
        if (isDeathDialogShowing) return
        isDeathDialogShowing = true

        val prefs = getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE)
        val correo = prefs.getString("correo_usuario", "") ?: ""
        val usuario = db.obtenerUsuarioLogueado(correo) ?: return
        val inventario = db.obtenerInventario(correo)
        val pocion = inventario.find { it.tipo == "CONSUMIBLE" && it.subtipo == "POCION" }

        val vista = layoutInflater.inflate(R.layout.dialogo_muerte, null)
        val tvTitulo = vista.findViewById<TextView>(R.id.tv_titulo_muerte)
        val btnPocion = vista.findViewById<Button>(R.id.btn_resucitar_pocion)
        val btnMonedas = vista.findViewById<Button>(R.id.btn_resucitar_monedas)
        val btnGratis = vista.findViewById<Button>(R.id.btn_resucitar_gratis)

        val dialog = AlertDialog.Builder(this)
            .setView(vista)
            .setCancelable(false)
            .create()

        // Opción 1: Poción
        if (pocion != null) {
            btnPocion.isEnabled = true
            btnPocion.text = "Usar ${pocion.nombre} (Cura 50 HP)"
            btnPocion.setOnClickListener {
                db.actualizarProgresoUsuario(correo, 0, 0, 50)
                db.eliminarDelInventario(pocion.id)
                finalizarMuerte(dialog)
            }
        } else {
            btnPocion.isEnabled = false
            btnPocion.alpha = 0.5f
            btnPocion.text = "Sin pociones de vida"
        }

        // Opción 2: Monedas
        if (usuario.monedas >= 50) {
            btnMonedas.isEnabled = true
            btnMonedas.setOnClickListener {
                db.actualizarProgresoUsuario(correo, 0, -50, 25)
                finalizarMuerte(dialog)
            }
        } else {
            btnMonedas.isEnabled = false
            btnMonedas.alpha = 0.5f
            btnMonedas.text = "Falta Oro (Necesitas 50)"
        }

        // Opción 3: Gratis
        btnGratis.setOnClickListener {
            db.actualizarProgresoUsuario(correo, 0, 0, 10)
            finalizarMuerte(dialog)
        }

        dialog.show()
    }

    private fun finalizarMuerte(dialog: AlertDialog) {
        actualizarHeader()
        isDeathDialogShowing = false
        dialog.dismiss()
        // Notificar al fragmento actual que refresque si es necesario
        supportFragmentManager.fragments.forEach { fragment ->
            (fragment as? com.example.xpeando.fragments.FragmentHabitos)?.actualizarLista()
        }
    }

    private fun mostrarDialogoCerrarSesion() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro de que quieres abandonar la aventura?")
            .setPositiveButton("Salir") { _, _ ->
                cerrarSesion()
            }
            .setNegativeButton("Cancelar", null)
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
            tvNivel.text = "Nivel ${it.nivel}"
            
            pbHP.progress = it.hp
            pbXP.progress = it.experiencia.toInt()

            // Si el HP llega a 0, activamos el sistema de resurrección
            if (it.hp <= 0 && !isDeathDialogShowing) {
                mostrarDialogoMuerte()
            }

            val navView = findViewById<NavigationView>(R.id.nav_view)
            if (navView.headerCount > 0) {
                val headerView = navView.getHeaderView(0)
                // Uso de safe call para evitar errores si los IDs no coinciden exactamente
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
