package com.example.xpeando.fragments

import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.xpeando.R
import com.example.xpeando.adapters.HistorialJefesAdapter
import com.example.xpeando.database.DBHelper
import com.example.xpeando.model.Jefe
import com.example.xpeando.utils.NotificationHelper

class FragmentJefes : Fragment() {

    private lateinit var dbHelper: DBHelper
    private var jefeActual: Jefe? = null

    private lateinit var tvNombreJefe: TextView
    private lateinit var tvDescripcionJefe: TextView
    private lateinit var tvHpJefe: TextView
    private lateinit var pbHpJefe: ProgressBar
    private lateinit var ivJefe: ImageView
    private lateinit var tvRecompensas: TextView
    private lateinit var tvRecompensasXP: TextView
    private lateinit var tvContadorReaparicion: TextView
    private lateinit var rvHistorial: RecyclerView

    private var countDownTimer: CountDownTimer? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_jefes, container, false)
        dbHelper = DBHelper(requireContext())

        tvNombreJefe = view.findViewById(R.id.tvNombreJefe)
        tvDescripcionJefe = view.findViewById(R.id.tvDescripcionJefe)
        tvHpJefe = view.findViewById(R.id.tvHpJefe)
        pbHpJefe = view.findViewById(R.id.pbHpJefe)
        ivJefe = view.findViewById(R.id.ivJefe)
        tvRecompensas = view.findViewById(R.id.tvRecompensasJefe)
        tvRecompensasXP = view.findViewById(R.id.tvRecompensasXPJefe)
        tvContadorReaparicion = view.findViewById(R.id.tvContadorReaparicion)
        rvHistorial = view.findViewById(R.id.rvHistorialJefes)

        cargarJefe()
        cargarHistorial()

        return view
    }

    override fun onResume() {
        super.onResume()
        cargarJefe()
        cargarHistorial()
    }

    private fun cargarHistorial() {
        val prefs = requireActivity().getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE)
        val correo = prefs.getString("correo_usuario", "") ?: ""
        val jefesDerrotados = dbHelper.obtenerJefesDerrotados(correo)
        rvHistorial.adapter = HistorialJefesAdapter(jefesDerrotados)
    }

    override fun onPause() {
        super.onPause()
        countDownTimer?.cancel()
    }

    private fun cargarJefe() {
        countDownTimer?.cancel()
        val prefs = requireActivity().getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE)
        val correo = prefs.getString("correo_usuario", "") ?: ""
        jefeActual = dbHelper.obtenerJefeActivo(correo)
        jefeActual?.let {
            tvNombreJefe.text = it.nombre
            tvDescripcionJefe.text = it.descripcion
            tvDescripcionJefe.visibility = View.VISIBLE
            tvHpJefe.text = "HP: ${it.hpActual} / ${it.hpMax}"
            pbHpJefe.max = it.hpMax
            pbHpJefe.progress = it.hpActual
            
            tvRecompensas.text = "Recompensas: ${it.recompensaMonedas}"
            tvRecompensasXP.text = "| ${it.recompensaXP} XP"
            ivJefe.visibility = View.VISIBLE
            tvRecompensas.visibility = View.VISIBLE
            tvRecompensasXP.visibility = View.VISIBLE
            tvContadorReaparicion.visibility = View.GONE

            // Mapeo de iconos locales
            val resId = resources.getIdentifier(it.icono, "drawable", requireContext().packageName)
            if (resId != 0) {
                ivJefe.setImageResource(resId)
            } else {
                ivJefe.setImageResource(R.drawable.ic_boss_dragon) // Fallback
            }
        } ?: run {
            tvNombreJefe.text = "¡Jefe Derrotado!"
            tvHpJefe.text = "Esperando reaparición..."
            pbHpJefe.progress = 0
            ivJefe.visibility = View.GONE
            tvRecompensas.visibility = View.GONE
            tvRecompensasXP.visibility = View.GONE
            
            iniciarContadorReaparicion()
        }
    }

    private fun iniciarContadorReaparicion() {
        val prefs = requireActivity().getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE)
        val correo = prefs.getString("correo_usuario", "") ?: ""
        val ultimaMuerte = dbHelper.obtenerUltimoJefeDerrotadoTime(correo)
        if (ultimaMuerte == 0L) {
            tvContadorReaparicion.visibility = View.GONE
            return
        }

        val tiempoRespawn = 21 * 60 * 60 * 1000L
        val tiempoRestante = (ultimaMuerte + tiempoRespawn) - System.currentTimeMillis()

        if (tiempoRestante > 0) {
            tvContadorReaparicion.visibility = View.VISIBLE
            countDownTimer = object : CountDownTimer(tiempoRestante, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    val horas = (millisUntilFinished / (1000 * 60 * 60))
                    val minutos = (millisUntilFinished / (1000 * 60)) % 60
                    val segundos = (millisUntilFinished / 1000) % 60
                    tvContadorReaparicion.text = String.format(java.util.Locale.getDefault(), "Reaparece en: %02d:%02d:%02d", horas, minutos, segundos)
                }

                override fun onFinish() {
                    NotificationHelper.enviarNotificacionLogro(requireContext(), "¡El Jefe ha reaparecido!", "Un nuevo desafío te espera en la sección de Jefes.")
                    cargarJefe()
                }
            }.start()
        } else {
            // Ya debería haber reaparecido, intentamos recargar (DBHelper lo resucitará)
            cargarJefe()
        }
    }
}
