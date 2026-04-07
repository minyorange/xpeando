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
import com.google.android.material.floatingactionbutton.FloatingActionButton
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
                        val daño = db.procesarDailiesFallidas(correoUsuario, diasDiferencia)
                        if (daño > 0) {
                            Toast.makeText(requireContext(), "¡Has vuelto! Recibes $daño de daño por $diasDiferencia días de ausencia.", Toast.LENGTH_LONG).show()
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
            lista = db.obtenerTodasDailies(),
            onCheckedChange = { daily, completada ->
                val dailiesAntes = db.obtenerTotalDailiesCompletadas()
                val usuarioAntes = db.obtenerUsuarioLogueado(correoUsuario)
                val nivelAntes = usuarioAntes?.nivel ?: 1

                db.actualizarEstadoDaily(daily, completada)
                
                val multiplicador = if (completada) 1 else -1
                db.actualizarProgresoUsuario(
                    correoUsuario,
                    daily.experiencia * multiplicador,
                    daily.monedas * multiplicador
                )
                
                if (completada) {
                    db.dañarJefe(25, correoUsuario)
                }

                val dailiesDespues = db.obtenerTotalDailiesCompletadas()
                val usuarioDespues = db.obtenerUsuarioLogueado(correoUsuario)
                val nivelDespues = usuarioDespues?.nivel ?: 1

                // Verificar logros
                usuarioDespues?.let {
                    if (completada) {
                        LogroManager.verificarNuevosLogros(requireContext(), db, it, dailiesAntes, dailiesDespues, "DAILY")
                        LogroManager.verificarNuevosLogros(requireContext(), db, it, usuarioAntes?.monedas ?: 0, it.monedas, "MONEDAS")
                    }

                    if (nivelDespues > nivelAntes) {
                        LogroManager.verificarNuevosLogros(requireContext(), db, it, nivelAntes, nivelDespues, "NIVEL")
                        // Aquí podrías mostrar un diálogo de subida de nivel similar al de Tareas si lo deseas
                    }
                }

                (activity as? MainActivity)?.actualizarHeader()
                actualizarLista()
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
        adaptador.actualizarLista(db.obtenerTodasDailies())
    }
}
