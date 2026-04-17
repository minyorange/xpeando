package com.example.xpeando.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.example.xpeando.R
import com.example.xpeando.activities.MainActivity
import com.example.xpeando.adapters.TareasAdapter
import com.example.xpeando.model.Tarea
import com.example.xpeando.repository.DataRepository
import com.example.xpeando.utils.LogroManager
import com.example.xpeando.viewmodel.TareasViewModel
import com.example.xpeando.viewmodel.UsuarioViewModel
import com.example.xpeando.viewmodel.ViewModelFactory
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import android.view.Gravity
import kotlinx.coroutines.launch

class FragmentTareas : Fragment() {

    private lateinit var repository: DataRepository
    private val viewModel: TareasViewModel by viewModels { ViewModelFactory(DataRepository()) }
    private val usuarioViewModel: UsuarioViewModel by activityViewModels { ViewModelFactory(DataRepository()) }
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

        repository = DataRepository()
        
        rvTareas = view.findViewById(R.id.rv_tareas)
        chipGroupFiltros = view.findViewById(R.id.chip_group_filtros)
        val fabAnadirTarea = view.findViewById<FloatingActionButton>(R.id.fab_anadir_tarea)

        configurarRecyclerView()
        configurarFiltros()
        observarViewModel()

        fabAnadirTarea.setOnClickListener {
            mostrarDialogoAnadirTarea()
        }
    }

    private fun configurarRecyclerView() {
        val prefs = requireActivity().getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE)
        val correo = prefs.getString("correo_usuario", "") ?: ""

        adaptador = TareasAdapter(
            emptyList(),
            onTareaCompletada = { tarea, completada ->
                if (!tarea.completada && completada) {
                    viewModel.completarTarea(requireContext(), tarea, correo) { nuevoNivel ->
                        mostrarDialogoSubidaNivel(nuevoNivel)
                    }
                    mostrarToastPersonalizado("¡Misión Completada!")
                } else {
                    actualizarLista()
                }
            },
            onTareaLongClick = { tarea ->
                mostrarDialogoEliminar(tarea)
            }
        )
        rvTareas.adapter = adaptador
        actualizarLista()
    }

    private var nivelActual: Int = -1

    private fun observarViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.tareas.collect { lista ->
                        adaptador.actualizarLista(lista)
                    }
                }
                launch {
                    viewModel.usuario.collect { usuario ->
                        usuario?.let {
                            if (nivelActual != -1 && it.nivel > nivelActual) {
                                mostrarDialogoSubidaNivel(it.nivel)
                            }
                            nivelActual = it.nivel
                        }
                    }
                }
            }
        }
    }

    private fun mostrarDialogoSubidaNivel(nuevoNivel: Int) {
        val vista = layoutInflater.inflate(R.layout.dialogo_subida_nivel, null)
        val tvNivel = vista.findViewById<TextView>(R.id.tv_nivel_nuevo)
        val btnContinuar = vista.findViewById<Button>(R.id.btn_continuar_aventura)
        tvNivel.text = "Nivel $nuevoNivel"
        val dialog = AlertDialog.Builder(requireContext()).setView(vista).setCancelable(false).create()
        btnContinuar.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun configurarFiltros() {
        chipGroupFiltros.setOnCheckedStateChangeListener { _, _ ->
            actualizarLista()
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

        val dialog = AlertDialog.Builder(requireContext()).setView(vista).create()

        btnAceptar.setOnClickListener {
            val nombre = etNombre.text.toString()
            if (nombre.isNotEmpty()) {
                val dificultad = spinnerDificultad.selectedItemPosition + 1
                val xp = dificultad * 20
                val monedas = dificultad * 10
                viewModel.insertarTarea(Tarea(correo_usuario = correo, nombre = nombre, dificultad = dificultad, experiencia = xp, monedas = monedas))
                dialog.dismiss()
            } else {
                etNombre.error = "Nombra tu misión, héroe"
            }
        }
        btnCancelar.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun mostrarDialogoEliminar(tarea: Tarea) {
        val prefs = requireActivity().getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE)
        val correo = prefs.getString("correo_usuario", "") ?: ""
        val mostrarCompletadas = chipGroupFiltros.checkedChipId == R.id.chip_completadas

        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Misión")
            .setMessage("¿Deseas abandonar la misión '${tarea.nombre}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.eliminarTarea(tarea.id, correo, mostrarCompletadas)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun actualizarLista() {
        val prefs = requireActivity().getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE)
        val correo = prefs.getString("correo_usuario", "") ?: ""
        val mostrarCompletadas = chipGroupFiltros.checkedChipId == R.id.chip_completadas
        viewModel.cargarTareas(correo, mostrarCompletadas)
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
        val params = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        params.gravity = Gravity.CENTER_HORIZONTAL
        customView.layoutParams = params
        snackbarLayout.addView(customView)
        snackbar.show()
    }
}
