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
import com.example.xpeando.R
import com.example.xpeando.adapters.LogrosAdapter
import com.example.xpeando.database.DBHelper
import com.example.xpeando.utils.LogroManager
import java.util.Locale

class FragmentEstadisticas : Fragment() {

    private lateinit var db: DBHelper

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
        tvTareas.text = db.obtenerTotalTareasCompletadas().toString()
        tvDailies.text = db.obtenerTotalDailiesCompletadas().toString()
        tvHabitos.text = (usuario?.totalHabitos ?: 0).toString()
    }
}
