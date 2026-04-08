package com.example.xpeando.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.xpeando.R
import com.example.xpeando.activities.MainActivity
import com.example.xpeando.adapters.TareasAdapter
import com.example.xpeando.database.DBHelper
import com.example.xpeando.model.Tarea
import com.example.xpeando.utils.LogroManager
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView

class FragmentTareas : Fragment() {

    private lateinit var db: DBHelper
    private lateinit var adaptador: TareasAdapter
    private lateinit var rvTareas: RecyclerView
    private lateinit var chipGroupFiltros: ChipGroup

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tareas, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = DBHelper(requireContext())
        
        rvTareas = view.findViewById(R.id.rv_tareas)
        chipGroupFiltros = view.findViewById(R.id.chip_group_filtros)
        val fabAnadirTarea = view.findViewById<FloatingActionButton>(R.id.fab_anadir_tarea)

        configurarRecyclerView()
        configurarFiltros()

        fabAnadirTarea.setOnClickListener {
            mostrarDialogoAnadirTarea()
        }
    }

    private fun configurarRecyclerView() {
        val prefs = requireActivity().getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE)
        val correo = prefs.getString("correo_usuario", "") ?: ""

        adaptador = TareasAdapter(
            obtenerListaFiltrada(),
            onTareaCompletada = { tarea, completada ->
                // Solo permitimos marcar como completada si no lo estaba ya.
                // Una vez completada, no se puede desmarcar.
                if (!tarea.completada && completada) {
                    val tareasAntes = db.obtenerTotalTareasCompletadas(correo)
                    val usuarioAntes = db.obtenerUsuarioLogueado(correo)
                    val nivelAntes = usuarioAntes?.nivel ?: 1

                    val tareaActualizada = tarea.copy(completada = true)
                    db.actualizarTarea(tareaActualizada)
                    
                    db.actualizarProgresoUsuario(
                        correo,
                        tarea.experiencia,
                        tarea.monedas
                    )
                    
                    // Daño basado en dificultad (Trivial=1 -> 15, Difícil=4 -> 60)
                    val danioMision = tarea.dificultad * 15
                    db.atacarJefe(danioMision, correo)

                    val tareasDespues = db.obtenerTotalTareasCompletadas(correo)
                    val usuarioDespues = db.obtenerUsuarioLogueado(correo)
                    val nivelDespues = usuarioDespues?.nivel ?: 1

                    // Verificar logros
                    usuarioDespues?.let {
                        LogroManager.verificarNuevosLogros(requireContext(), db, it, tareasAntes, tareasDespues, "TAREA")
                        LogroManager.verificarNuevosLogros(requireContext(), db, it, usuarioAntes?.monedas ?: 0, it.monedas, "MONEDAS")
                        
                        if (nivelDespues > nivelAntes) {
                            LogroManager.verificarNuevosLogros(requireContext(), db, it, nivelAntes, nivelDespues, "NIVEL")
                            mostrarDialogoSubidaNivel(nivelDespues)
                        }
                    }

                    (activity as? MainActivity)?.actualizarHeader()
                    actualizarLista()
                    mostrarToastPersonalizado("¡Misión Completada!")
                } else {
                    // Si se intenta desmarcar una completada, refrescamos para que el checkbox siga marcado visualmente
                    actualizarLista()
                }
            },
            onTareaLongClick = { tarea ->
                mostrarDialogoEliminar(tarea)
            }
        )
        rvTareas.adapter = adaptador
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

    private fun configurarFiltros() {
        chipGroupFiltros.setOnCheckedStateChangeListener { _, _ ->
            actualizarLista()
        }
    }

    private fun obtenerListaFiltrada(): List<Tarea> {
        val prefs = requireActivity().getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE)
        val correo = prefs.getString("correo_usuario", "") ?: ""
        val todas = db.obtenerTodasLasTareas(correo)
        return when (chipGroupFiltros.checkedChipId) {
            R.id.chip_completadas -> {
                // Solo mostramos las últimas 10 completadas para no saturar la vista
                todas.filter { it.completada }.takeLast(10).reversed()
            }
            else -> todas.filter { !it.completada }
        }
    }

    private fun mostrarDialogoAnadirTarea() {
        val prefs = requireActivity().getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE)
        val correo = prefs.getString("correo_usuario", "") ?: ""

        val vista = layoutInflater.inflate(R.layout.dialogo_nueva_tarea, null)
        val etNombre = vista.findViewById<EditText>(R.id.et_nombre_tarea_dialogo)
        val spinnerDificultad = vista.findViewById<Spinner>(R.id.spinner_dificultad)
        val btnAceptar = vista.findViewById<Button>(R.id.btn_aceptar_mision)
        val btnCancelar = vista.findViewById<Button>(R.id.btn_cancelar_mision)

        val opciones = arrayOf("Trivial", "Fácil", "Normal", "Difícil")
        spinnerDificultad.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, opciones)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(vista)
            .create()

        btnAceptar.setOnClickListener {
            val nombre = etNombre.text.toString()
            if (nombre.isNotEmpty()) {
                val dificultad = spinnerDificultad.selectedItemPosition + 1
                val xp = dificultad * 20
                val monedas = dificultad * 10
                
                val nuevaTarea = Tarea(
                    correo_usuario = correo,
                    nombre = nombre,
                    dificultad = dificultad,
                    experiencia = xp,
                    monedas = monedas
                )
                db.insertarTarea(nuevaTarea)
                actualizarLista()
                dialog.dismiss()
            } else {
                etNombre.error = "Nombra tu misión, héroe"
            }
        }

        btnCancelar.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun mostrarDialogoEliminar(tarea: Tarea) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Misión")
            .setMessage("¿Deseas abandonar la misión '${tarea.nombre}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                db.eliminarTarea(tarea.id)
                actualizarLista()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun actualizarLista() {
        adaptador.actualizarLista(obtenerListaFiltrada())
    }

    private fun mostrarToastPersonalizado(mensaje: String) {
        val snackbar = Snackbar.make(requireView(), "", Snackbar.LENGTH_SHORT)
        val snackbarLayout = snackbar.view as ViewGroup
        
        snackbarLayout.removeAllViews()
        snackbarLayout.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        snackbarLayout.setPadding(0, 0, 0, 150)

        val layoutInflater = LayoutInflater.from(requireContext())
        val customView = layoutInflater.inflate(R.layout.layout_toast_mision_completada, snackbarLayout, false)
        
        customView.findViewById<TextView>(R.id.tv_mensaje_toast_mision).text = mensaje

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        params.gravity = Gravity.CENTER_HORIZONTAL
        customView.layoutParams = params
        
        snackbarLayout.addView(customView)
        snackbar.show()
    }
}
