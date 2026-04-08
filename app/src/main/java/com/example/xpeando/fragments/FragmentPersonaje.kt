package com.example.xpeando.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.xpeando.R
import com.example.xpeando.activities.MainActivity
import com.example.xpeando.adapters.InventarioAdapter
import com.example.xpeando.database.DBHelper
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.Locale

class FragmentPersonaje : Fragment() {

    private lateinit var db: DBHelper
    private var correoUsuario: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_personaje, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = DBHelper(requireContext())
        
        val prefs = requireActivity().getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE)
        correoUsuario = prefs.getString("correo_usuario", "") ?: ""

        val fabMochila = view.findViewById<FloatingActionButton>(R.id.fab_mochila)
        fabMochila.setOnClickListener {
            mostrarMochila()
        }

        actualizarUI()
    }

    private fun actualizarUI() {
        val view = view ?: return
        val usuario = db.obtenerUsuarioLogueado(correoUsuario) ?: return

        val tvNombre = view.findViewById<TextView>(R.id.tv_nombre_personaje)
        val tvNivel = view.findViewById<TextView>(R.id.tv_nivel)
        val tvHPValor = view.findViewById<TextView>(R.id.tv_hp_valor)
        val pbHP = view.findViewById<ProgressBar>(R.id.pb_hp)
        val tvXPValor = view.findViewById<TextView>(R.id.tv_xp_valor)
        val pbExperiencia = view.findViewById<ProgressBar>(R.id.pb_experiencia)
        val tvMonedas = view.findViewById<TextView>(R.id.tv_monedas_personaje)
        
        val tvFuerza = view.findViewById<TextView>(R.id.tv_fuerza_valor)
        val tvInteligencia = view.findViewById<TextView>(R.id.tv_inteligencia_valor)
        val tvConstitucion = view.findViewById<TextView>(R.id.tv_constitucion_valor)
        val tvPercepcion = view.findViewById<TextView>(R.id.tv_percepcion_valor)
        
        val tvPuntos = view.findViewById<TextView>(R.id.tv_puntos_disponibles)
        val btnFza = view.findViewById<Button>(R.id.btn_subir_fza)
        val btnInt = view.findViewById<Button>(R.id.btn_subir_int)
        val btnCon = view.findViewById<Button>(R.id.btn_subir_con)
        val btnPer = view.findViewById<Button>(R.id.btn_subir_per)

        val inventario = db.obtenerInventario(correoUsuario)
        val equipado = inventario.filter { it.equipado }
        
        val bonusFza = equipado.sumOf { it.bonusFza }
        val bonusInt = equipado.sumOf { it.bonusInt }
        val bonusCon = equipado.sumOf { it.bonusCon }
        val bonusPer = equipado.sumOf { it.bonusPer }

        tvNombre.text = usuario.nombre
        tvNivel.text = "Nivel ${usuario.nivel}"
        tvHPValor.text = "${usuario.hp} / 50"
        pbHP.progress = usuario.hp
        tvXPValor.text = "${usuario.experiencia} / 100"
        pbExperiencia.progress = usuario.experiencia
        tvMonedas.text = "${usuario.monedas} Monedas"
        
        // Mostrar solo el número entero. El progreso decimal está guardado en la DB.
        tvFuerza.text = "${usuario.fuerza.toInt()} ${if (bonusFza > 0) "(+$bonusFza)" else ""}"
        tvInteligencia.text = "${usuario.inteligencia.toInt()} ${if (bonusInt > 0) "(+$bonusInt)" else ""}"
        tvConstitucion.text = "${usuario.constitucion.toInt()} ${if (bonusCon > 0) "(+$bonusCon)" else ""}"
        tvPercepcion.text = "${usuario.percepcion.toInt()} ${if (bonusPer > 0) "(+$bonusPer)" else ""}"

        // Configurar Tooltips al hacer clic en los valores
        val clickListener = View.OnClickListener { v ->
            val (titulo, desc) = when (v.id) {
                R.id.tv_fuerza_valor, R.id.tv_fuerza_label -> {
                    val mult = usuario.fuerza + (bonusFza / 10.0)
                    "Fuerza" to "Multiplica el daño que haces al jefe por ${String.format("%.1f", mult)}.\n(Base: ${String.format("%.1f", usuario.fuerza)} + Equipo: ${String.format("%.1f", bonusFza/10.0)})"
                }
                R.id.tv_inteligencia_valor, R.id.tv_inteligencia_label -> {
                    val mult = usuario.inteligencia + (bonusInt / 10.0)
                    "Inteligencia" to "Multiplica la XP ganada por ${String.format("%.1f", mult)}.\n(Base: ${String.format("%.1f", usuario.inteligencia)} + Equipo: ${String.format("%.1f", bonusInt/10.0)})"
                }
                R.id.tv_constitucion_valor, R.id.tv_constitucion_label -> {
                    val mult = usuario.constitucion + (bonusCon / 10.0)
                    "Constitución" to "Divide el daño que recibes por ${String.format("%.1f", mult)}.\n(Base: ${String.format("%.1f", usuario.constitucion)} + Equipo: ${String.format("%.1f", bonusCon/10.0)})"
                }
                R.id.tv_percepcion_valor, R.id.tv_percepcion_label -> {
                    val mult = usuario.percepcion + (bonusPer / 10.0)
                    "Percepción" to "Multiplica las monedas obtenidas por ${String.format("%.1f", mult)}.\n(Base: ${String.format("%.1f", usuario.percepcion)} + Equipo: ${String.format("%.1f", bonusPer/10.0)})"
                }
                else -> "" to ""
            }
            if (titulo.isNotEmpty()) {
                AlertDialog.Builder(requireContext())
                    .setTitle(titulo)
                    .setMessage(desc)
                    .setPositiveButton("Entendido", null)
                    .show()
            }
        }

        tvFuerza.setOnClickListener(clickListener)
        tvInteligencia.setOnClickListener(clickListener)
        tvConstitucion.setOnClickListener(clickListener)
        tvPercepcion.setOnClickListener(clickListener)
        
        // También habilitar clic en las etiquetas si existen en el layout
        view.findViewById<TextView>(R.id.tv_fuerza_label)?.setOnClickListener(clickListener)
        view.findViewById<TextView>(R.id.tv_inteligencia_label)?.setOnClickListener(clickListener)
        view.findViewById<TextView>(R.id.tv_constitucion_label)?.setOnClickListener(clickListener)
        view.findViewById<TextView>(R.id.tv_percepcion_label)?.setOnClickListener(clickListener)

        if (usuario.puntosDisponibles > 0) {
            tvPuntos.visibility = View.VISIBLE
            tvPuntos.text = "${usuario.puntosDisponibles} Puntos Libres"
            btnFza.visibility = View.VISIBLE
            btnInt.visibility = View.VISIBLE
            btnCon.visibility = View.VISIBLE
            btnPer.visibility = View.VISIBLE
            
            btnFza.setOnClickListener { subirAtributo("fza") }
            btnInt.setOnClickListener { subirAtributo("int") }
            btnCon.setOnClickListener { subirAtributo("con") }
            btnPer.setOnClickListener { subirAtributo("per") }
        } else {
            tvPuntos.visibility = View.GONE
            btnFza.visibility = View.GONE
            btnInt.visibility = View.GONE
            btnCon.visibility = View.GONE
            btnPer.visibility = View.GONE
        }
    }

    private fun mostrarMochila() {
        val vista = layoutInflater.inflate(R.layout.dialogo_mochila, null)
        val rv = vista.findViewById<RecyclerView>(R.id.rv_inventario)
        val btnCerrar = vista.findViewById<Button>(R.id.btn_cerrar_mochila)
        val tabLayout = vista.findViewById<com.google.android.material.tabs.TabLayout>(R.id.tab_layout_inventario)
        
        var inventarioCompleto = db.obtenerInventario(correoUsuario)
        
        // Función para filtrar por pestaña
        fun obtenerListaFiltrada(tabPosition: Int): List<com.example.xpeando.model.Articulo> {
            return if (tabPosition == 0) {
                inventarioCompleto.filter { it.tipo == "EQUIPO" }
            } else {
                inventarioCompleto.filter { it.tipo == "CONSUMIBLE" }
            }
        }

        val adapter = InventarioAdapter(obtenerListaFiltrada(0)) { articulo ->
            if (articulo.tipo == "CONSUMIBLE" && articulo.subtipo == "POCION") {
                usarPocion(articulo.id, articulo.bonusHp)
            } else {
                db.equiparDesequipar(correoUsuario, articulo.id)
            }
            // Recargar datos y refrescar la pestaña actual
            inventarioCompleto = db.obtenerInventario(correoUsuario)
            (rv.adapter as? InventarioAdapter)?.actualizarLista(obtenerListaFiltrada(tabLayout.selectedTabPosition))
            actualizarUI()
        }
        rv.adapter = adapter

        tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab) {
                adapter.actualizarLista(obtenerListaFiltrada(tab.position))
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
        })
        
        val dialog = AlertDialog.Builder(requireContext())
            .setView(vista)
            .create()
            
        btnCerrar.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun usarPocion(id: Int, curacion: Int) {
        val usuario = db.obtenerUsuarioLogueado(correoUsuario) ?: return
        
        if (usuario.hp >= 50) {
            Toast.makeText(requireContext(), "Tu salud ya está al máximo", Toast.LENGTH_SHORT).show()
            return
        }

        db.actualizarProgresoUsuario(correoUsuario, 0, 0, curacion)
        db.eliminarDelInventario(id)
        Toast.makeText(requireContext(), "¡Poción usada! +$curacion HP", Toast.LENGTH_SHORT).show()
        (activity as? MainActivity)?.actualizarHeader()
    }

    private fun subirAtributo(tipo: String) {
        when (tipo) {
            "fza" -> db.actualizarAtributos(correoUsuario, fza = 1.0, puntosUsados = 1)
            "int" -> db.actualizarAtributos(correoUsuario, int = 1.0, puntosUsados = 1)
            "con" -> db.actualizarAtributos(correoUsuario, con = 1.0, puntosUsados = 1)
            "per" -> db.actualizarAtributos(correoUsuario, per = 1.0, puntosUsados = 1)
        }
        actualizarUI()
    }
}
