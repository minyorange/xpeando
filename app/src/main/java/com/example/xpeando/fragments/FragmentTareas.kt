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
                val tareasAntes = db.obtenerTotalTareasCompletadas()
                val usuarioAntes = db.obtenerUsuarioLogueado(correo)
                val nivelAntes = usuarioAntes?.nivel ?: 1

                val tareaActualizada = tarea.copy(completada = completada)
                db.actualizarTarea(tareaActualizada)
                
                val multiplicador = if (completada) 1 else -1
                db.actualizarProgresoUsuario(
                    correo,
                    tarea.experiencia * multiplicador,
                    tarea.monedas * multiplicador
                )
                
                if (completada) {
                    db.dañarJefe(50, correo)
                }

                val tareasDespues = db.obtenerTotalTareasCompletadas()
                val usuarioDespues = db.obtenerUsuarioLogueado(correo)
                val nivelDespues = usuarioDespues?.nivel ?: 1

                // Verificar logros
                usuarioDespues?.let {
                    if (completada) {
                        LogroManager.verificarNuevosLogros(requireContext(), db, it, tareasAntes, tareasDespues, "TAREA")
                        LogroManager.verificarNuevosLogros(requireContext(), db, it, usuarioAntes?.monedas ?: 0, it.monedas, "MONEDAS")
                    }
                    
                    // Verificar logros de nivel si ha subido
                    if (nivelDespues > nivelAntes) {
                        LogroManager.verificarNuevosLogros(requireContext(), db, it, nivelAntes, nivelDespues, "NIVEL")
                        mostrarDialogoSubidaNivel(nivelDespues)
                    }
                }

                (activity as? MainActivity)?.actualizarHeader()
                actualizarLista()
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
        val todas = db.obtenerTodasLasTareas()
        return when (chipGroupFiltros.checkedChipId) {
            R.id.chip_completadas -> todas.filter { it.completada }
            else -> todas.filter { !it.completada }
        }
    }

    private fun mostrarDialogoAnadirTarea() {
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
}
