package com.example.xpeando.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.graphics.Color
import com.example.xpeando.R
import com.example.xpeando.adapters.LogrosAdapter
import com.example.xpeando.database.DBHelper
import com.example.xpeando.utils.LogroManager
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

class FragmentEstadisticas : Fragment() {

    private lateinit var db: DBHelper

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

        db = DBHelper(requireContext())

        // Vistas de Progresión Global
        val tvTareas = view.findViewById<TextView>(R.id.tv_total_tareas)
        val tvDailies = view.findViewById<TextView>(R.id.tv_total_dailies)
        val tvHabitos = view.findViewById<TextView>(R.id.tv_total_habitos)

        // Vistas de Nivel y XP
        val tvNivel = view.findViewById<TextView>(R.id.tv_stat_nivel_valor)
        val tvXpTexto = view.findViewById<TextView>(R.id.tv_stat_xp_porcentaje)
        val pbXp = view.findViewById<ProgressBar>(R.id.pb_stat_xp)

        // Vistas de Atributos
        val tvFza = view.findViewById<TextView>(R.id.tv_stat_fza)
        val tvInt = view.findViewById<TextView>(R.id.tv_stat_int)
        val tvCon = view.findViewById<TextView>(R.id.tv_stat_con)
        val tvPer = view.findViewById<TextView>(R.id.tv_stat_per)

        // RecyclerView de Logros
        val rvLogros = view.findViewById<RecyclerView>(R.id.rv_logros)

        // Cargar datos del usuario
        val prefs = requireContext().getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE)
        val correo = prefs.getString("correo_usuario", "") ?: ""
        
        val usuario = db.obtenerUsuarioLogueado(correo)
        usuario?.let { user ->
            // Actualizar Nivel y XP
            tvNivel.text = "Nivel ${user.nivel}"
            tvXpTexto.text = "${user.experiencia}/100 XP"
            pbXp.progress = user.experiencia

            // Actualizar Atributos
            tvFza.text = String.format(Locale.getDefault(), "%.1f", user.fuerza)
            tvInt.text = String.format(Locale.getDefault(), "%.1f", user.inteligencia)
            tvCon.text = String.format(Locale.getDefault(), "%.1f", user.constitucion)
            tvPer.text = String.format(Locale.getDefault(), "%.1f", user.percepcion)

            // Configurar Logros usando el Manager centralizado
            val listaLogros = LogroManager.obtenerLogrosDefinidos(db, user)
            rvLogros.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            rvLogros.adapter = LogrosAdapter(listaLogros)
        }

        // Cargar totales
        tvTareas.text = db.obtenerTotalTareasCompletadas(correo).toString()
        tvDailies.text = db.obtenerTotalDailiesCompletadas(correo).toString()
        tvHabitos.text = (usuario?.totalHabitos ?: 0).toString()

        // Configurar Gráficos
        configurarGraficoXP(view.findViewById(R.id.chart_xp_semanal), correo)
        configurarGraficoDistribucion(view.findViewById(R.id.chart_distribucion_actividad), correo)
    }

    private fun configurarGraficoXP(chart: LineChart, correo: String) {
        val historial = db.obtenerHistorial(correo)
        val entries = mutableListOf<Entry>()
        val labels = mutableListOf<String>()

        // Agrupar XP por fecha (últimos 7 días)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val hoy = Date()

        for (i in 6 downTo 0) {
            calendar.time = hoy
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val fechaStr = sdf.format(calendar.time)
            
            val xpDia = historial.filter { it.fecha == fechaStr }.sumOf { it.xp }
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

    private fun configurarGraficoDistribucion(chart: PieChart, correo: String) {
        val tareas = db.obtenerTotalTareasCompletadas(correo)
        val dailies = db.obtenerTotalDailiesCompletadas(correo)
        val habitos = db.obtenerTotalHabitosCompletados(correo)

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
