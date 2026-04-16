package com.example.xpeando.fragments

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.example.xpeando.R
import com.example.xpeando.activities.MainActivity
import com.example.xpeando.adapters.HabitosAdapter
import com.example.xpeando.model.Habito
import com.example.xpeando.repository.DataRepository
import com.example.xpeando.utils.LogroManager
import com.example.xpeando.viewmodel.HabitosViewModel
import com.example.xpeando.viewmodel.UsuarioViewModel
import com.example.xpeando.viewmodel.ViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class FragmentHabitos : Fragment() {

    private lateinit var repository: DataRepository
    private val viewModel: HabitosViewModel by viewModels { ViewModelFactory(DataRepository()) }
    private val usuarioViewModel: UsuarioViewModel by activityViewModels { ViewModelFactory(DataRepository()) }
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

        repository = DataRepository()
        
        rvHabitos = view.findViewById(R.id.rv_habitos)
        val fabAnadirHabito = view.findViewById<FloatingActionButton>(R.id.fab_anadir_habito)

        configurarRecyclerView()
        observarViewModel()

        fabAnadirHabito.setOnClickListener {
            mostrarDialogoAnadirHabito()
        }
    }

    private fun configurarRecyclerView() {
        val prefs = requireActivity().getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE)
        val correo = prefs.getString("correo_usuario", "") ?: ""

        adaptador = HabitosAdapter(
            listaHabitos = emptyList(),
            onAccionHabito = { habito, delta ->
                viewModel.procesarAccion(requireContext(), habito, delta, correo) { nuevoNivel ->
                    mostrarDialogoSubidaNivel(nuevoNivel)
                }
            },
            onLongClick = { habito ->
                mostrarDialogoEliminar(habito)
            }
        )
        rvHabitos.adapter = adaptador
        viewModel.cargarHabitos(correo)
    }

    private fun observarViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.habitos.collect { lista ->
                        adaptador.actualizarLista(lista)
                    }
                }
                launch {
                    viewModel.usuario.collect { usuario ->
                        // UI se actualiza automáticamente mediante Flow
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

        val dialog = AlertDialog.Builder(requireContext()).setView(vista).create()

        btnAceptar.setOnClickListener {
            val nombre = etNombre.text.toString()
            if (nombre.isNotEmpty()) {
                val atributoSeleccionado = spinnerAtributo.selectedItem.toString()
                viewModel.insertarHabito(Habito(correo_usuario = correo, nombre = nombre, atributo = atributoSeleccionado))
                dialog.dismiss()
            } else {
                etNombre.error = "Dale un nombre a tu hábito"
            }
        }
        btnCancelar.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun mostrarDialogoEliminar(habito: Habito) {
        val prefs = requireActivity().getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE)
        val correo = prefs.getString("correo_usuario", "") ?: ""
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Hábito")
            .setMessage("¿Estás seguro de que quieres eliminar '${habito.nombre}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.eliminarHabito(habito.id, correo)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    fun actualizarLista() {
        val prefs = requireActivity().getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE)
        val correo = prefs.getString("correo_usuario", "") ?: ""
        viewModel.cargarHabitos(correo)
    }
}
