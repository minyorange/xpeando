package com.example.xpeando.fragments

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.xpeando.R
import com.example.xpeando.adapters.LogrosAdapter
import com.example.xpeando.repository.DataRepository
import com.example.xpeando.viewmodel.EstadisticasViewModel
import com.example.xpeando.viewmodel.ViewModelFactory
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class FragmentEstadisticas : Fragment() {

    private val viewModel: EstadisticasViewModel by viewModels { ViewModelFactory(DataRepository()) }
    private lateinit var rvLogros: RecyclerView
    private lateinit var tvTareas: TextView
    private lateinit var tvDailies: TextView
    private lateinit var tvHabitos: TextView
    private lateinit var tvNivel: TextView
    private lateinit var tvXpTexto: TextView
    private lateinit var pbXp: ProgressBar
    private lateinit var tvFza: TextView
    private lateinit var tvInt: TextView
    private lateinit var tvCon: TextView
    private lateinit var tvPer: TextView
    private lateinit var chartXP: LineChart
    private lateinit var chartDistribucion: PieChart

    class LabelFormatter(private val labels: List<String>) : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            val index = value.toInt()
            return if (index >= 0 && index < labels.size) labels[index] else ""
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_estadisticas, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Vistas de Progresión Global
        tvTareas = view.findViewById(R.id.tv_total_tareas)
        tvDailies = view.findViewById(R.id.tv_total_dailies)
        tvHabitos = view.findViewById(R.id.tv_total_habitos)

        // Vistas de Nivel y XP
        tvNivel = view.findViewById(R.id.tv_stat_nivel_valor)
        tvXpTexto = view.findViewById(R.id.tv_stat_xp_porcentaje)
        pbXp = view.findViewById(R.id.pb_stat_xp)

        // Vistas de Atributos
        tvFza = view.findViewById(R.id.tv_stat_fza)
        tvInt = view.findViewById(R.id.tv_stat_int)
        tvCon = view.findViewById(R.id.tv_stat_con)
        tvPer = view.findViewById(R.id.tv_stat_per)

        // RecyclerView de Logros
        rvLogros = view.findViewById(R.id.rv_logros)
        rvLogros.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        // Gráficos
        chartXP = view.findViewById(R.id.chart_xp_semanal)
        chartDistribucion = view.findViewById(R.id.chart_distribucion_actividad)

        val prefs = requireContext().getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE)
        val correo = prefs.getString("correo_usuario", "") ?: ""
        
        observarViewModel()
        
        if (correo.isNotEmpty()) {
            viewModel.cargarEstadisticas(correo)
        }
    }

    private fun observarViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    state.usuario?.let { user ->
                        tvNivel.text = "Nivel ${user.nivel}"
                        tvXpTexto.text = "${user.experiencia}/100 XP"
                        pbXp.progress = user.experiencia

                        tvFza.text = String.format(Locale.getDefault(), "%.1f", user.fuerza)
                        tvInt.text = String.format(Locale.getDefault(), "%.1f", user.inteligencia)
                        tvCon.text = String.format(Locale.getDefault(), "%.1f", user.constitucion)
                        tvPer.text = String.format(Locale.getDefault(), "%.1f", user.percepcion)
                    }

                    tvTareas.text = state.totalTareas.toString()
                    tvDailies.text = state.totalDailies.toString()
                    tvHabitos.text = state.totalHabitos.toString()

                    rvLogros.adapter = LogrosAdapter(state.logros)

                    configurarGraficoXP(chartXP, state.xpSemanal)
                    configurarGraficoDistribucion(chartDistribucion, state.totalTareas, state.totalDailies, state.totalHabitos)
                }
            }
        }
    }

    private fun configurarGraficoXP(chart: LineChart, xpSemanal: Map<String, Int>) {
        val entries = mutableListOf<Entry>()
        val labels = mutableListOf<String>()

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val hoy = Date()

        for (i in 6 downTo 0) {
            calendar.time = hoy
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val fechaStr = sdf.format(calendar.time)
            
            val xpDia = xpSemanal[fechaStr] ?: 0
            entries.add(Entry((6 - i).toFloat(), xpDia.toFloat()))
            labels.add(fechaStr.substring(5)) // Solo MM-dd
        }

        val dataSet = LineDataSet(entries, "XP Ganada")
        dataSet.color = Color.parseColor("#6200EE")
        dataSet.setCircleColor(Color.parseColor("#6200EE"))
        dataSet.lineWidth = 2f
        dataSet.circleRadius = 4f
        dataSet.setDrawValues(false)
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER

        chart.data = LineData(dataSet)
        chart.xAxis.valueFormatter = LabelFormatter(labels)
        chart.xAxis.granularity = 1f
        chart.description.isEnabled = false
        chart.animateX(1000)
        chart.invalidate()
    }

    private fun configurarGraficoDistribucion(chart: PieChart, tareas: Int, dailies: Int, habitos: Int) {
        val entries = mutableListOf<PieEntry>()
        if (tareas > 0) entries.add(PieEntry(tareas.toFloat(), "Tareas"))
        if (dailies > 0) entries.add(PieEntry(dailies.toFloat(), "Dailies"))
        if (habitos > 0) entries.add(PieEntry(habitos.toFloat(), "Hábitos"))

        if (entries.isEmpty()) {
            chart.setNoDataText("No hay actividad registrada aún")
            return
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = listOf(
            Color.parseColor("#FF6200EE"),
            Color.parseColor("#FF03DAC5"),
            Color.parseColor("#FFFFB300")
        )
        dataSet.valueTextSize = 14f
        dataSet.valueTextColor = Color.WHITE

        chart.data = PieData(dataSet)
        chart.description.isEnabled = false
        chart.centerText = "Actividad"
        chart.animateY(1000)
        chart.invalidate()
    }
}
