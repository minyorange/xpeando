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
import com.example.xpeando.adapters.DailiesAdapter
import com.example.xpeando.database.DBHelper
import com.example.xpeando.model.Daily
import com.example.xpeando.repository.DataRepository
import com.example.xpeando.utils.LogroManager
import com.example.xpeando.utils.NotificationHelper
import com.example.xpeando.viewmodel.DailiesViewModel
import com.example.xpeando.viewmodel.UsuarioViewModel
import com.example.xpeando.viewmodel.ViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import android.view.Gravity
import com.example.xpeando.utils.XpeandoToast
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FragmentDailies : Fragment() {

    private lateinit var repository: DataRepository
    private val viewModel: DailiesViewModel by viewModels { ViewModelFactory(DataRepository(DBHelper(requireContext()))) }
    private val usuarioViewModel: UsuarioViewModel by activityViewModels { ViewModelFactory(DataRepository(DBHelper(requireContext()))) }
    private lateinit var adaptador: DailiesAdapter
    private lateinit var rvDailies: RecyclerView
    private var correoUsuario: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dailies, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = DataRepository(DBHelper(requireContext()))
        val prefs = requireActivity().getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE)
        correoUsuario = prefs.getString("correo_usuario", "") ?: ""

        rvDailies = view.findViewById(R.id.rv_dailies)
        val fab = view.findViewById<FloatingActionButton>(R.id.fab_anadir_daily)

        verificarPenalizacionDiaria()
        configurarRecyclerView()
        observarViewModel()

        fab.setOnClickListener {
            mostrarDialogoNuevaDaily()
        }
    }

    private fun verificarPenalizacionDiaria() {
        val prefs = requireActivity().getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE)
        val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val ultimaVezPenalizado = prefs.getString("ultima_penalizacion_dailies", "")

        if (ultimaVezPenalizado.isNullOrEmpty()) {
            prefs.edit().putString("ultima_penalizacion_dailies", hoy).apply()
            return
        }

        if (ultimaVezPenalizado != hoy) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            try {
                val fechaInicio = sdf.parse(ultimaVezPenalizado)
                val fechaFin = sdf.parse(hoy)
                
                if (fechaInicio != null && fechaFin != null) {
                    val diffInMillis = fechaFin.time - fechaInicio.time
                    val diasDiferencia = (diffInMillis / (1000 * 60 * 60 * 24)).toInt()

                    if (diasDiferencia > 0) {
                        val danio = viewModel.procesarDailiesFallidas(correoUsuario, diasDiferencia)
                        if (danio > 0) {
                            XpeandoToast.error(requireContext(), "¡Has vuelto! Recibes $danio de daño por $diasDiferencia días de ausencia.")
                            NotificationHelper.enviarNotificacionLogro(requireContext(), "¡Penalización por Ausencia!", "Has recibido $danio de daño por no completar tus dailies.")
                        }
                        prefs.edit().putString("ultima_penalizacion_dailies", hoy).apply()
                    }
                }
            } catch (e: Exception) {
                prefs.edit().putString("ultima_penalizacion_dailies", hoy).apply()
            }
        }
    }

    private fun configurarRecyclerView() {
        adaptador = DailiesAdapter(
            lista = emptyList(),
            onCheckedChange = { daily, completada ->
                if (completada) {
                    viewModel.completarDaily(requireContext(), daily, correoUsuario) { nuevoNivel ->
                        mostrarDialogoSubidaNivel(nuevoNivel)
                    }
                    usuarioViewModel.refrescarUsuario(correoUsuario)
                    mostrarToastPersonalizado("¡Tarea Diaria Realizada!")
                }
            },
            onLongClick = { daily ->
                mostrarDialogoEliminar(daily)
            }
        )
        rvDailies.adapter = adaptador
        viewModel.cargarDailies(correoUsuario)
    }

    private fun observarViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.dailies.collect { lista ->
                        adaptador.actualizarLista(lista)
                        if (lista.isEmpty()) {
                            // Feedback de lista vacía se puede añadir aquí
                        }
                    }
                }
                launch {
                    viewModel.usuario.collect { usuario ->
                        usuario?.let {
                            (activity as? MainActivity)?.actualizarHeader()
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

    private fun mostrarDialogoNuevaDaily() {
        val vista = layoutInflater.inflate(R.layout.dialogo_nueva_daily, null)
        val etNombre = vista.findViewById<EditText>(R.id.et_nombre_daily_dialogo)
        val spinnerDificultad = vista.findViewById<Spinner>(R.id.spinner_dificultad_daily)
        val btnAceptar = vista.findViewById<Button>(R.id.btn_aceptar_daily)
        val btnCancelar = vista.findViewById<Button>(R.id.btn_cancelar_daily)

        val opciones = arrayOf("Trivial", "Fácil", "Normal", "Difícil")
        spinnerDificultad.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, opciones)
        spinnerDificultad.setSelection(1)

        val dialog = AlertDialog.Builder(requireContext()).setView(vista).create()

        btnAceptar.setOnClickListener {
            val nombre = etNombre.text.toString()
            if (nombre.isNotEmpty()) {
                val dificultad = spinnerDificultad.selectedItemPosition + 1
                viewModel.insertarDaily(Daily(correo_usuario = correoUsuario, nombre = nombre, experiencia = 10 * dificultad, monedas = 5 * dificultad))
                dialog.dismiss()
            } else {
                etNombre.error = "Escribe un nombre para la rutina"
            }
        }
        btnCancelar.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun mostrarDialogoEliminar(daily: Daily) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Daily")
            .setMessage("¿Deseas eliminar '${daily.nombre}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.eliminarDaily(daily.id, correoUsuario)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarToastPersonalizado(mensaje: String) {
        val snackbar = Snackbar.make(requireView(), "", Snackbar.LENGTH_SHORT)
        val snackbarLayout = snackbar.view as ViewGroup
        snackbarLayout.removeAllViews()
        snackbarLayout.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        snackbarLayout.setPadding(0, 0, 0, 150)
        val layoutInflater = LayoutInflater.from(requireContext())
        val customView = layoutInflater.inflate(R.layout.layout_toast_daily_completada, snackbarLayout, false)
        customView.findViewById<TextView>(R.id.tv_mensaje_toast).text = mensaje
        val params = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        params.gravity = Gravity.CENTER_HORIZONTAL
        customView.layoutParams = params
        snackbarLayout.addView(customView)
        snackbar.show()
    }
}
