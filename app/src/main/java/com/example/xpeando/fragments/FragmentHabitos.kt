package com.example.xpeando.fragments

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.xpeando.R
import com.example.xpeando.activities.MainActivity
import com.example.xpeando.adapters.HabitosAdapter
import com.example.xpeando.database.DBHelper
import com.example.xpeando.model.Habito
import com.example.xpeando.utils.LogroManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar

class FragmentHabitos : Fragment() {

    private lateinit var db: DBHelper
    private lateinit var adaptador: HabitosAdapter
    private lateinit var rvHabitos: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_habitos, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = DBHelper(requireContext())
        
        rvHabitos = view.findViewById(R.id.rv_habitos)
        val fabAnadirHabito = view.findViewById<FloatingActionButton>(R.id.fab_anadir_habito)

        configurarRecyclerView()

        fabAnadirHabito.setOnClickListener {
            mostrarDialogoAnadirHabito()
        }
    }

    private fun configurarRecyclerView() {
        val prefs = requireActivity().getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE)
        val correo = prefs.getString("correo_usuario", "") ?: ""

        adaptador = HabitosAdapter(
            listaHabitos = db.obtenerTodosHabitos(correo),
            onAccionHabito = { habito, delta ->
                val habitosAntes = db.obtenerTotalHabitosCompletados(correo)
                val usuarioAntes = db.obtenerUsuarioLogueado(correo)
                val nivelAntes = usuarioAntes?.nivel ?: 1

                val xpCambio = habito.experiencia * delta
                val monedasCambio = if (delta > 0) habito.monedas else 0
                val hpCambio = if (delta < 0) -5 else 0
                
                // --- ACTIVAR RACHA SI LA ACCIÓN ES POSITIVA ---
                if (delta > 0) {
                    db.actualizarRacha(correo)
                }

                db.actualizarProgresoUsuario(correo, xpCambio, monedasCambio, hpCambio)
                
                // Balanceo: Daño al jefe si es positivo, Curación si es negativo (solo atributo Fuerza)
                if (habito.atributo == "Fuerza") {
                    val danioBase = if (delta > 0) 10 else -15 // Los fallos curan más de lo que dañan
                    db.atacarJefe(danioBase, correo)
                }

                // MEJORA: Subida gradual de atributos (0.01)
                if (delta > 0) {
                    when (habito.atributo) {
                        "Fuerza" -> db.actualizarAtributos(correo, fza = 0.01)
                        "Inteligencia" -> db.actualizarAtributos(correo, int = 0.01)
                        "Constitución" -> db.actualizarAtributos(correo, con = 0.01)
                        "Percepción" -> db.actualizarAtributos(correo, per = 0.01)
                    }
                }
                
                val habitosDespues = db.obtenerTotalHabitosCompletados(correo)
                val usuarioDespues = db.obtenerUsuarioLogueado(correo)
                val nivelDespues = usuarioDespues?.nivel ?: 1

                // Verificar logros
                usuarioDespues?.let {
                    if (delta > 0) {
                        LogroManager.verificarNuevosLogros(requireContext(), db, it, habitosAntes, habitosDespues, "HABITO")
                        LogroManager.verificarNuevosLogros(requireContext(), db, it, usuarioAntes?.monedas ?: 0, it.monedas, "MONEDAS")
                    }

                    if (nivelDespues > nivelAntes) {
                        LogroManager.verificarNuevosLogros(requireContext(), db, it, nivelAntes, nivelDespues, "NIVEL")
                        mostrarDialogoSubidaNivel(nivelDespues)
                    } else {
                        mostrarFeedbackProgreso(requireView(), xpCambio, monedasCambio, hpCambio, if (delta > 0) habito.atributo else null)
                    }
                }

                (activity as? MainActivity)?.actualizarHeader()
            },
            onLongClick = { habito ->
                mostrarDialogoEliminar(habito)
            }
        )
        rvHabitos.adapter = adaptador
    }

    private fun mostrarDialogoSubidaNivel(nuevoNivel: Int) {
        val vista = layoutInflater.inflate(R.layout.dialogo_subida_nivel, null)
        val tvNivel = vista.findViewById<TextView>(R.id.tv_nivel_nuevo)
        val btnContinuar = vista.findViewById<Button>(R.id.btn_continuar_aventura)
        
        tvNivel.text = "Nivel $nuevoNivel"
        
        val dialog = AlertDialog.Builder(requireContext())
            .setView(vista)
            .setCancelable(false)
            .create()
            
        btnContinuar.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }

    private fun mostrarFeedbackProgreso(vista: View, xp: Int, monedas: Int, hp: Int, atributo: String? = null) {
        val snackbar = Snackbar.make(vista, "", Snackbar.LENGTH_SHORT)
        val snackbarLayout = snackbar.view as ViewGroup
        
        snackbarLayout.removeAllViews()
        snackbarLayout.setBackgroundColor(Color.TRANSPARENT)
        snackbarLayout.setPadding(0, 0, 0, 150)

        val layoutInflater = LayoutInflater.from(requireContext())
        val customView = layoutInflater.inflate(R.layout.layout_toast_progreso, snackbarLayout, false)
        
        val tvXP = customView.findViewById<TextView>(R.id.tv_xp_feedback)
        val tvMonedas = customView.findViewById<TextView>(R.id.tv_monedas_feedback)
        val ivIconXP = customView.findViewById<ImageView>(R.id.iv_icon_feedback)
        
        var textoXp = if (xp >= 0) "+$xp XP" else "$xp XP"
        if (atributo != null) {
            textoXp += " | +0.01 $atributo"
        }
        tvXP.text = textoXp
        
        tvMonedas.text = if (monedas >= 0) "+$monedas" else "$monedas"
        
        if (hp < 0) {
            tvXP.text = "${tvXP.text} | $hp HP"
            ivIconXP.setColorFilter(ContextCompat.getColor(requireContext(), R.color.xpeando_red_negative))
        } else if (xp < 0) {
            ivIconXP.setColorFilter(ContextCompat.getColor(requireContext(), R.color.xpeando_red_negative))
        }

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        params.gravity = Gravity.CENTER_HORIZONTAL
        customView.layoutParams = params
        
        snackbarLayout.addView(customView)
        snackbar.show()
    }

    private fun mostrarDialogoAnadirHabito() {
        val prefs = requireActivity().getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE)
        val correo = prefs.getString("correo_usuario", "") ?: ""

        val vista = layoutInflater.inflate(R.layout.dialogo_nuevo_habito, null)
        val etNombre = vista.findViewById<EditText>(R.id.et_nombre_habito_dialogo)
        val spinnerAtributo = vista.findViewById<Spinner>(R.id.spinner_atributo_habito)
        val btnAceptar = vista.findViewById<Button>(R.id.btn_aceptar_habito)
        val btnCancelar = vista.findViewById<Button>(R.id.btn_cancelar_habito)

        val atributos = arrayOf("Fuerza", "Inteligencia", "Constitución", "Percepción")
        spinnerAtributo.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, atributos)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(vista)
            .create()

        btnAceptar.setOnClickListener {
            val nombre = etNombre.text.toString()
            if (nombre.isNotEmpty()) {
                val atributoSeleccionado = spinnerAtributo.selectedItem.toString()
                val nuevoHabito = Habito(correo_usuario = correo, nombre = nombre, atributo = atributoSeleccionado)
                db.insertarHabito(nuevoHabito)
                actualizarLista()
                dialog.dismiss()
            } else {
                etNombre.error = "Dale un nombre a tu hábito"
            }
        }

        btnCancelar.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun mostrarDialogoEliminar(habito: Habito) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Hábito")
            .setMessage("¿Estás seguro de que quieres eliminar '${habito.nombre}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                db.eliminarHabito(habito.id)
                actualizarLista()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    fun actualizarLista() {
        val prefs = requireActivity().getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE)
        val correo = prefs.getString("correo_usuario", "") ?: ""
        adaptador.actualizarLista(db.obtenerTodosHabitos(correo))
    }
}
