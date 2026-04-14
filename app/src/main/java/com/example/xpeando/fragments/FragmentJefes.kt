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
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.xpeando.R
import com.example.xpeando.adapters.HistorialJefesAdapter
import com.example.xpeando.database.DBHelper
import com.example.xpeando.model.Jefe
import com.example.xpeando.utils.NotificationHelper
import com.example.xpeando.repository.DataRepository
import com.example.xpeando.viewmodel.RpgViewModel
import com.example.xpeando.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

class FragmentJefes : Fragment() {

    private lateinit var dbHelper: DBHelper
    private val rpgViewModel: RpgViewModel by viewModels {
        ViewModelFactory(DataRepository(DBHelper(requireContext())))
    }
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

        ivJefe.setOnClickListener {
            atacarAlJefe()
        }

        observarViewModel()
        
        val prefs = requireActivity().getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE)
        val correo = prefs.getString("correo_usuario", "") ?: ""
        rpgViewModel.cargarJefeActivo(correo)
        rpgViewModel.cargarHistorial(correo)

        return view
    }

    private fun atacarAlJefe() {
        val prefs = requireActivity().getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE)
        val correo = prefs.getString("correo_usuario", "") ?: ""
        
        jefeActual?.let { jefe ->
            // Efecto visual de golpe
            ivJefe.animate().scaleX(0.9f).scaleY(0.9f).setDuration(50).withEndAction {
                ivJefe.animate().scaleX(1.0f).scaleY(1.0f).setDuration(50)
            }

            rpgViewModel.atacarJefe(10, correo) { derrotado ->
                if (derrotado) {
                    android.widget.Toast.makeText(requireContext(), "¡HAS DERROTADO AL JEFE!", android.widget.Toast.LENGTH_LONG).show()
                    (activity as? com.example.xpeando.activities.MainActivity)?.actualizarHeader()
                }
            }
        }
    }

    private fun observarViewModel() {
        lifecycleScope.launch {
            rpgViewModel.jefeActivo.collect { jefe ->
                actualizarUIJefe(jefe)
            }
        }
        lifecycleScope.launch {
            rpgViewModel.historialJefes.collect { historial ->
                rvHistorial.adapter = HistorialJefesAdapter(historial)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val prefs = requireActivity().getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE)
        val correo = prefs.getString("correo_usuario", "") ?: ""
        rpgViewModel.cargarJefeActivo(correo)
        rpgViewModel.cargarHistorial(correo)
    }

    private fun cargarHistorial() {
        // Obsoleto, ahora se maneja por StateFlow
    }

    override fun onPause() {
        super.onPause()
        countDownTimer?.cancel()
    }

    private fun cargarJefe() {
        // Obsoleto, ahora se maneja por StateFlow
    }

    private fun actualizarUIJefe(jefe: Jefe?) {
        countDownTimer?.cancel()
        jefeActual = jefe
        jefe?.let {
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
        lifecycleScope.launch {
            val prefs = requireActivity().getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE)
            val correo = prefs.getString("correo_usuario", "") ?: ""
            val ultimaMuerte = rpgViewModel.obtenerUltimoJefeDerrotadoTime(correo)
            
            if (ultimaMuerte == 0L) {
                tvContadorReaparicion.visibility = View.GONE
                return@launch
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
                        if (isAdded) {
                            NotificationHelper.enviarNotificacionLogro(requireContext(), "¡El Jefe ha reaparecido!", "Un nuevo desafío te espera en la sección de Jefes.")
                            rpgViewModel.cargarJefeActivo(correo)
                        }
                    }
                }.start()
            } else {
                // Ya debería haber reaparecido
                rpgViewModel.cargarJefeActivo(correo)
            }
        }
    }
}
