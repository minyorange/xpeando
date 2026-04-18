package com.example.xpeando.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.xpeando.R
import com.example.xpeando.adapters.InventarioAdapter
import com.example.xpeando.model.Articulo
import com.example.xpeando.model.Usuario
import com.example.xpeando.viewmodel.UsuarioViewModel
import com.example.xpeando.viewmodel.RpgViewModel
import com.example.xpeando.viewmodel.ViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.example.xpeando.utils.XpeandoToast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class FragmentPersonaje : Fragment() {

    private val usuarioViewModel: UsuarioViewModel by activityViewModels { ViewModelFactory() }
    private val rpgViewModel: RpgViewModel by activityViewModels { ViewModelFactory() }
    
    private var correoUsuario: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_personaje, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val prefs = requireActivity().getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE)
        correoUsuario = prefs.getString("correo_usuario", "") ?: ""

        observarViewModel()

        view.findViewById<FloatingActionButton>(R.id.fab_mochila).setOnClickListener {
            mostrarMochila()
        }

        view.findViewById<View>(R.id.iv_info_atributos).setOnClickListener {
            mostrarTutorialAtributos()
        }

        val prefsTutorial = requireActivity().getSharedPreferences("TutorialPrefs", Context.MODE_PRIVATE)
        if (correoUsuario.isNotEmpty() && !prefsTutorial.getBoolean("tutorial_atributos_visto_$correoUsuario", false)) {
            mostrarTutorialAtributos()
            prefsTutorial.edit().putBoolean("tutorial_atributos_visto_$correoUsuario", true).apply()
        }

        usuarioViewModel.cargarUsuario(correoUsuario)
        rpgViewModel.cargarInventario(correoUsuario)
    }

    private fun observarViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(usuarioViewModel.usuario, rpgViewModel.inventario) { u, inv ->
                    u to inv
                }.collect { (usuario, _) ->
                    usuario?.let { actualizarUI(it) }
                }
            }
        }
    }

    private fun actualizarUI(usuario: Usuario) {
        val view = view ?: return

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

        val inventario = rpgViewModel.inventario.value
        val equipado = inventario.filter { it.equipado }
        
        val bonusFza = equipado.sumOf { it.bonusFza }
        val bonusInt = equipado.sumOf { it.bonusInt }
        val bonusCon = equipado.sumOf { it.bonusCon }
        val bonusPer = equipado.sumOf { it.bonusPer }

        tvNombre.text = usuario.nombre
        tvNivel.text = "Nivel ${usuario.nivel}"
        tvHPValor.text = "${usuario.hp} / 50"
        pbHP.progress = usuario.hp
        val xpParaSiguiente = usuario.nivel * 100
        tvXPValor.text = "${usuario.experiencia} / $xpParaSiguiente"
        pbExperiencia.max = xpParaSiguiente
        pbExperiencia.progress = usuario.experiencia
        tvMonedas.text = "${usuario.monedas}"
        
        tvFuerza.text = "${usuario.fuerza.toInt()} ${if (bonusFza > 0) "(+$bonusFza)" else ""}"
        tvInteligencia.text = "${usuario.inteligencia.toInt()} ${if (bonusInt > 0) "(+$bonusInt)" else ""}"
        tvConstitucion.text = "${usuario.constitucion.toInt()} ${if (bonusCon > 0) "(+$bonusCon)" else ""}"
        tvPercepcion.text = "${usuario.percepcion.toInt()} ${if (bonusPer > 0) "(+$bonusPer)" else ""}"

        val clickListener = View.OnClickListener { v ->
            val (titulo, desc) = when (v.id) {
                R.id.tv_fuerza_valor, R.id.tv_fuerza_label -> {
                    val mult = usuario.fuerza + (bonusFza / 10.0)
                    "Fuerza" to "Daño x${String.format("%.1f", mult)}\n(Base: ${String.format("%.1f", usuario.fuerza)} + Equipo: ${String.format("%.1f", bonusFza/10.0)})"
                }
                R.id.tv_inteligencia_valor, R.id.tv_inteligencia_label -> {
                    val mult = usuario.inteligencia + (bonusInt / 10.0)
                    "Inteligencia" to "XP x${String.format("%.1f", mult)}\n(Base: ${String.format("%.1f", usuario.inteligencia)} + Equipo: ${String.format("%.1f", bonusInt/10.0)})"
                }
                R.id.tv_constitucion_valor, R.id.tv_constitucion_label -> {
                    val mult = usuario.constitucion + (bonusCon / 10.0)
                    "Constitución" to "Defensa x${String.format("%.1f", mult)}\n(Base: ${String.format("%.1f", usuario.constitucion)} + Equipo: ${String.format("%.1f", bonusCon/10.0)})"
                }
                R.id.tv_percepcion_valor, R.id.tv_percepcion_label -> {
                    val mult = usuario.percepcion + (bonusPer / 10.0)
                    "Percepción" to "Oro x${String.format("%.1f", mult)}\n(Base: ${String.format("%.1f", usuario.percepcion)} + Equipo: ${String.format("%.1f", bonusPer/10.0)})"
                }
                else -> "" to ""
            }
            if (titulo.isNotEmpty()) {
                AlertDialog.Builder(requireContext()).setTitle(titulo).setMessage(desc).setPositiveButton("OK", null).show()
            }
        }

        tvFuerza.setOnClickListener(clickListener)
        tvInteligencia.setOnClickListener(clickListener)
        tvConstitucion.setOnClickListener(clickListener)
        tvPercepcion.setOnClickListener(clickListener)
        view.findViewById<View>(R.id.tv_fuerza_label)?.setOnClickListener(clickListener)
        view.findViewById<View>(R.id.tv_inteligencia_label)?.setOnClickListener(clickListener)
        view.findViewById<View>(R.id.tv_constitucion_label)?.setOnClickListener(clickListener)
        view.findViewById<View>(R.id.tv_percepcion_label)?.setOnClickListener(clickListener)

        if (usuario.puntosDisponibles > 0) {
            tvPuntos.visibility = View.VISIBLE
            tvPuntos.text = "${usuario.puntosDisponibles} Puntos Libres"
            btnFza.visibility = View.VISIBLE
            btnInt.visibility = View.VISIBLE
            btnCon.visibility = View.VISIBLE
            btnPer.visibility = View.VISIBLE
            btnFza.setOnClickListener { usuarioViewModel.subirAtributo(correoUsuario, "fza") }
            btnInt.setOnClickListener { usuarioViewModel.subirAtributo(correoUsuario, "int") }
            btnCon.setOnClickListener { usuarioViewModel.subirAtributo(correoUsuario, "con") }
            btnPer.setOnClickListener { usuarioViewModel.subirAtributo(correoUsuario, "per") }
        } else {
            tvPuntos.visibility = View.GONE
            listOf(btnFza, btnInt, btnCon, btnPer).forEach { it.visibility = View.GONE }
        }
    }

    private fun mostrarMochila() {
        val vista = layoutInflater.inflate(R.layout.dialogo_mochila, null)
        val rv = vista.findViewById<RecyclerView>(R.id.rv_inventario)
        val tabLayout = vista.findViewById<com.google.android.material.tabs.TabLayout>(R.id.tab_layout_inventario)
        
        fun obtenerListaFiltrada(tabPosition: Int): List<Articulo> {
            val inv = rpgViewModel.inventario.value
            return if (tabPosition == 0) inv.filter { it.tipo == "EQUIPO" } else inv.filter { it.tipo == "CONSUMIBLE" }
        }

        val adapter = InventarioAdapter(obtenerListaFiltrada(0)) { articulo ->
            if (articulo.tipo == "CONSUMIBLE" && articulo.subtipo == "POCION") {
                usarPocion(articulo.id, articulo.bonusHp)
            } else {
                rpgViewModel.equiparDesequipar(correoUsuario, articulo.id)
            }
        }
        rv.adapter = adapter

        val job = lifecycleScope.launch {
            rpgViewModel.inventario.collect {
                (rv.adapter as? InventarioAdapter)?.actualizarLista(obtenerListaFiltrada(tabLayout.selectedTabPosition))
            }
        }

        tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab) {
                adapter.actualizarLista(obtenerListaFiltrada(tab.position))
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
        })
        
        val dialog = AlertDialog.Builder(requireContext()).setView(vista).create()
        vista.findViewById<Button>(R.id.btn_cerrar_mochila).setOnClickListener { dialog.dismiss() }
        dialog.setOnDismissListener { job.cancel() }
        dialog.show()
    }

    private fun usarPocion(id: Int, curacion: Int) {
        val hpActual = usuarioViewModel.usuario.value?.hp ?: 0
        
        // Solo bloqueamos si ya tiene la vida máxima (50)
        if (hpActual >= 50) {
            XpeandoToast.info(requireContext(), "Salud al máximo")
            return
        }
        
        // Llamamos al RpgViewModel que cura Y elimina la poción de la mochila
        rpgViewModel.usarPocion(correoUsuario, id, curacion)
        XpeandoToast.success(requireContext(), "¡Poción usada! +$curacion HP")
    }

    private fun mostrarTutorialAtributos() {
        val vista = layoutInflater.inflate(R.layout.dialogo_tutorial_atributos, null)
        val dialog = AlertDialog.Builder(requireContext()).setView(vista).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        vista.findViewById<Button>(R.id.btn_tutorial_siguiente).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        if (correoUsuario.isNotEmpty()) {
            usuarioViewModel.cargarUsuario(correoUsuario)
            rpgViewModel.cargarInventario(correoUsuario)
        }
    }
}
