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
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.xpeando.R
import com.example.xpeando.activities.MainActivity
import com.example.xpeando.adapters.InventarioAdapter
import com.example.xpeando.repository.DataRepository
import com.example.xpeando.model.Articulo
import com.example.xpeando.model.Usuario
import com.example.xpeando.viewmodel.UsuarioViewModel
import com.example.xpeando.viewmodel.ViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.example.xpeando.utils.XpeandoToast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Locale

class FragmentPersonaje : Fragment() {

    private val usuarioViewModel: UsuarioViewModel by activityViewModels {
        ViewModelFactory(DataRepository())
    }
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

        val fabMochila = view.findViewById<FloatingActionButton>(R.id.fab_mochila)
        fabMochila.setOnClickListener {
            mostrarMochila()
        }

        val ivInfo = view.findViewById<View>(R.id.iv_info_atributos)
        ivInfo.setOnClickListener {
            mostrarTutorialAtributos()
        }

        // Mostrar tutorial automáticamente la primera vez por usuario
        val prefsTutorial = requireActivity().getSharedPreferences("TutorialPrefs", Context.MODE_PRIVATE)
        val tutorialVisto = prefsTutorial.getBoolean("tutorial_atributos_visto_$correoUsuario", false)
        if (correoUsuario.isNotEmpty() && !tutorialVisto) {
            mostrarTutorialAtributos()
            prefsTutorial.edit().putBoolean("tutorial_atributos_visto_$correoUsuario", true).apply()
        }

        usuarioViewModel.cargarUsuario(correoUsuario)
    }

    override fun onResume() {
        super.onResume()
        if (correoUsuario.isNotEmpty()) {
            usuarioViewModel.cargarUsuario(correoUsuario)
        }
    }

    private fun observarViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                usuarioViewModel.usuario.combine(usuarioViewModel.inventario) { u, inv ->
                    u to inv
                }.collect { (usuario, _) ->
                    usuario?.let { 
                        actualizarUI(it) 
                    }
                }
            }
        }
    }

    private fun mostrarTutorialAtributos() {
        val vista = layoutInflater.inflate(R.layout.dialogo_tutorial_atributos, null)
        val flipper = vista.findViewById<android.widget.ViewFlipper>(R.id.view_flipper_tutorial)
        val btnAtras = vista.findViewById<Button>(R.id.btn_tutorial_anterior)
        val btnSig = vista.findViewById<Button>(R.id.btn_tutorial_siguiente)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(vista)
            .create()
        
        // Hacer el fondo del diálogo transparente para que se vea el redondeado de la card
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnSig.setOnClickListener {
            if (flipper.displayedChild < flipper.childCount - 1) {
                // Siguiente: Entra desde la derecha, sale por la izquierda
                flipper.setInAnimation(requireContext(), R.anim.slide_in_right)
                flipper.setOutAnimation(requireContext(), R.anim.slide_out_left)
                flipper.showNext()
                btnAtras.visibility = View.VISIBLE
                if (flipper.displayedChild == flipper.childCount - 1) {
                    btnSig.text = "¡Entendido!"
                }
            } else {
                dialog.dismiss()
            }
        }

        btnAtras.setOnClickListener {
            if (flipper.displayedChild > 0) {
                // Atrás: Entra desde la izquierda, sale por la derecha
                flipper.setInAnimation(requireContext(), R.anim.slide_in_left)
                flipper.setOutAnimation(requireContext(), R.anim.slide_out_right)
                flipper.showPrevious()
                btnSig.text = "Siguiente"
                if (flipper.displayedChild == 0) {
                    btnAtras.visibility = View.INVISIBLE
                }
            }
        }

        dialog.show()
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

        val inventario = usuarioViewModel.inventario.value
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
        
        // Función para filtrar por pestaña
        fun obtenerListaFiltrada(tabPosition: Int): List<Articulo> {
            val inventarioCompleto = usuarioViewModel.inventario.value
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
                usuarioViewModel.equiparDesequipar(correoUsuario, articulo.id)
            }
            // El StateFlow se encargará de actualizar el adaptador mediante el recolector en mostrarMochila si lo hiciéramos reactivo.
            // Para simplificar ahora que el diálogo es síncrono, forzamos refresco del adaptador:
            lifecycleScope.launch {
                usuarioViewModel.inventario.collect {
                    (rv.adapter as? InventarioAdapter)?.actualizarLista(obtenerListaFiltrada(tabLayout.selectedTabPosition))
                }
            }
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
        val usuario = usuarioViewModel.usuario.value ?: return
        
        if (usuario.hp >= 50) {
            XpeandoToast.info(requireContext(), "Tu salud ya está al máximo")
            return
        }

        usuarioViewModel.usarPocion(correoUsuario, id, curacion)
        XpeandoToast.success(requireContext(), "¡Poción usada! +$curacion HP")
    }

    private fun subirAtributo(tipo: String) {
        usuarioViewModel.subirAtributo(correoUsuario, tipo)
    }
}
