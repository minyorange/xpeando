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
            text = "Pagar $costo Oro (+25 HP)"
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
        
        // Forzar refresco del fragmento actual para actualizar listas
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
            
            // --- ACTUALIZAR RACHA 🔥 ---
            if (it.rachaActual > 0) {
                tvRacha.text = "🔥 ${it.rachaActual}"
                cvRacha.visibility = View.VISIBLE
            } else {
                cvRacha.visibility = View.GONE
            }

            pbHP.progress = it.hp
            pbXP.progress = it.experiencia.toInt()

            // Si el HP llega a 0, activamos el sistema de resurrección
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
