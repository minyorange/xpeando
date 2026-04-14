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
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.xpeando.R
import com.example.xpeando.activities.MainActivity
import com.example.xpeando.adapters.DailiesAdapter
import com.example.xpeando.database.DBHelper
import com.example.xpeando.model.Daily
import com.example.xpeando.utils.LogroManager
import com.example.xpeando.utils.NotificationHelper
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FragmentDailies : Fragment() {

    private lateinit var db: DBHelper
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

        db = DBHelper(requireContext())
        val prefs = requireActivity().getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE)
        correoUsuario = prefs.getString("correo_usuario", "") ?: ""

        rvDailies = view.findViewById(R.id.rv_dailies)
        val fab = view.findViewById<FloatingActionButton>(R.id.fab_anadir_daily)

        verificarPenalizacionDiaria()
        configurarRecyclerView()

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
                        val danio = db.procesarDailiesFallidas(correoUsuario, diasDiferencia)
                        if (danio > 0) {
                            Toast.makeText(requireContext(), "¡Has vuelto! Recibes $danio de daño por $diasDiferencia días de ausencia.", Toast.LENGTH_LONG).show()
                            NotificationHelper.enviarNotificacionLogro(requireContext(), "¡Penalización por Ausencia!", "Has recibido $danio de daño por no completar tus dailies.")
                            (activity as? MainActivity)?.actualizarHeader()
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
            lista = db.obtenerTodasDailies(correoUsuario).filter { !it.completadaHoy },
            onCheckedChange = { daily, completada ->
                if (completada) { // Solo actuamos si se marca, no se puede desmarcar
                    val dailiesAntes = db.obtenerTotalDailiesCompletadas(correoUsuario)
                    val usuarioAntes = db.obtenerUsuarioLogueado(correoUsuario)
                    val nivelAntes = usuarioAntes?.nivel ?: 1

                    db.actualizarEstadoDaily(daily, true)
                    
                    // --- ACTUALIZAR RACHA ---
                    db.actualizarRacha(correoUsuario)
                    
                    // La lógica de Multiplicadores (INT, PER) ya está dentro de este método en DBHelper
                    db.actualizarProgresoUsuario(
                        correoUsuario,
                        daily.experiencia,
                        daily.monedas
                    )
                    
                    // La lógica de Multiplicador de Fuerza (FZA) ya está dentro de este método en DBHelper
                    db.atacarJefe(25, correoUsuario)

                    val dailiesDespues = db.obtenerTotalDailiesCompletadas(correoUsuario)
                    val usuarioDespues = db.obtenerUsuarioLogueado(correoUsuario)
                    val nivelDespues = usuarioDespues?.nivel ?: 1

                    // Verificar logros
                    usuarioDespues?.let {
                        LogroManager.verificarNuevosLogros(requireContext(), db, it, dailiesAntes, dailiesDespues, "DAILY")
                        LogroManager.verificarNuevosLogros(requireContext(), db, it, usuarioAntes?.monedas ?: 0, it.monedas, "MONEDAS")

                        if (nivelDespues > nivelAntes) {
                            LogroManager.verificarNuevosLogros(requireContext(), db, it, nivelAntes, nivelDespues, "NIVEL")
                        }
                    }

                    (activity as? MainActivity)?.actualizarHeader()
                    actualizarLista()
                    mostrarToastPersonalizado("¡Tarea Diaria Realizada!")
                    
                    if (db.obtenerTodasDailies(correoUsuario).none { !it.completadaHoy }) {
                        mostrarToastPersonalizado("¡No quedan tareas para hoy!")
                    }
                }
            },
            onLongClick = { daily ->
                mostrarDialogoEliminar(daily)
            }
        )
        rvDailies.adapter = adaptador
    }

    private fun mostrarDialogoNuevaDaily() {
        val vista = layoutInflater.inflate(R.layout.dialogo_nueva_daily, null)
        val etNombre = vista.findViewById<EditText>(R.id.et_nombre_daily_dialogo)
        val spinnerDificultad = vista.findViewById<Spinner>(R.id.spinner_dificultad_daily)
        val btnAceptar = vista.findViewById<Button>(R.id.btn_aceptar_daily)
        val btnCancelar = vista.findViewById<Button>(R.id.btn_cancelar_daily)

        val opciones = arrayOf("Trivial", "Fácil", "Normal", "Difícil")
        spinnerDificultad.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, opciones)
        spinnerDificultad.setSelection(1) // Por defecto "Fácil"

        val dialog = AlertDialog.Builder(requireContext()).setView(vista).create()

        btnAceptar.setOnClickListener {
            val nombre = etNombre.text.toString()
            if (nombre.isNotEmpty()) {
                val dificultad = spinnerDificultad.selectedItemPosition + 1
                // Calculamos XP y Monedas según dificultad
                val xpBase = 10 * dificultad
                val monedasBase = 5 * dificultad
                
                db.insertarDaily(Daily(
                    correo_usuario = correoUsuario,
                    nombre = nombre,
                    experiencia = xpBase,
                    monedas = monedasBase
                ))
                actualizarLista()
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
                db.eliminarDaily(daily.id)
                actualizarLista()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun actualizarLista() {
        val listaFiltrada = db.obtenerTodasDailies(correoUsuario).filter { !it.completadaHoy }
        adaptador.actualizarLista(listaFiltrada)
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
