package com.example.xpeando.fragments

import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.xpeando.R
import com.example.xpeando.adapters.HistorialJefesAdapter
import com.example.xpeando.model.Jefe
import com.example.xpeando.utils.NotificationHelper
import com.example.xpeando.repository.DataRepository
import com.example.xpeando.viewmodel.RpgViewModel
import com.example.xpeando.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

class FragmentJefes : Fragment() {

    private val rpgViewModel: RpgViewModel by viewModels {
        ViewModelFactory(DataRepository())
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
    private lateinit var ivInfo: ImageView

    private var countDownTimer: CountDownTimer? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_jefes, container, false)

        tvNombreJefe = view.findViewById(R.id.tvNombreJefe)
        tvDescripcionJefe = view.findViewById(R.id.tvDescripcionJefe)
        tvHpJefe = view.findViewById(R.id.tvHpJefe)
        pbHpJefe = view.findViewById(R.id.pbHpJefe)
        ivJefe = view.findViewById(R.id.ivJefe)
        tvRecompensas = view.findViewById(R.id.tvRecompensasJefe)
        tvRecompensasXP = view.findViewById(R.id.tvRecompensasXPJefe)
        tvContadorReaparicion = view.findViewById(R.id.tvContadorReaparicion)
        rvHistorial = view.findViewById(R.id.rvHistorialJefes)
        ivInfo = view.findViewById(R.id.iv_info_jefes)

        ivInfo.setOnClickListener {
            mostrarTutorialJefes()
        }

        /*
        ivJefe.setOnClickListener {
            atacarAlJefe()
        }
        */

        observarViewModel()
        
        val prefs = requireActivity().getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE)
        val correo = prefs.getString("correo_usuario", "") ?: ""
        rpgViewModel.cargarJefeActivo(correo)
        rpgViewModel.cargarHistorial(correo)

        // Mostrar tutorial si es la primera vez
        val prefsTutorial = requireActivity().getSharedPreferences("TutorialPrefs", Context.MODE_PRIVATE)
        val visto = prefsTutorial.getBoolean("tutorial_jefes_visto_$correo", false)
        if (correo.isNotEmpty() && !visto) {
            mostrarTutorialJefes()
            prefsTutorial.edit().putBoolean("tutorial_jefes_visto_$correo", true).apply()
        }

        return view
    }

    private fun mostrarTutorialJefes() {
        val vista = layoutInflater.inflate(R.layout.dialogo_tutorial_jefes, null)
        val flipper = vista.findViewById<android.widget.ViewFlipper>(R.id.view_flipper_jefes)
        val btnAtras = vista.findViewById<Button>(R.id.btn_tutorial_jefes_anterior)
        val btnSig = vista.findViewById<Button>(R.id.btn_tutorial_jefes_siguiente)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(vista)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnSig.setOnClickListener {
            if (flipper.displayedChild < flipper.childCount - 1) {
                flipper.setInAnimation(requireContext(), R.anim.slide_in_right)
                flipper.setOutAnimation(requireContext(), R.anim.slide_out_left)
                flipper.showNext()
                btnAtras.visibility = View.VISIBLE
                if (flipper.displayedChild == flipper.childCount - 1) {
                    btnSig.text = "¡A la Batalla!"
                }
            } else {
                dialog.dismiss()
            }
        }

        btnAtras.setOnClickListener {
            if (flipper.displayedChild > 0) {
                flipper.setInAnimation(requireContext(), R.anim.slide_in_left)
                flipper.setOutAnimation(requireContext(), R.anim.slide_out_right)
                flipper.showPrevious()
                btnSig.text = "Siguiente"
                if (flipper.displayedChild == 0) {
                    btnAtras.visibility = View.INVISIBLE
                }
            }
        }

        dialog.show()
    }

    private fun ejecutarAnimacionDano() {
        // 1. Efecto de "Flash Rojo"
        ivJefe.setColorFilter(android.graphics.Color.parseColor("#80FF0000")) 
        
        // 2. Efecto de Vibración y Escala
        ivJefe.animate()
            .translationXBy(15f)
            .scaleX(0.85f)
            .scaleY(0.85f)
            .setDuration(50)
            .withEndAction {
                ivJefe.animate()
                    .translationXBy(-30f)
                    .setDuration(50)
                    .withEndAction {
                        ivJefe.animate()
                            .translationX(0f)
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(50)
                            .withEndAction {
                                ivJefe.clearColorFilter()
                            }
                    }
            }
    }

    private fun atacarAlJefe() {
        val prefs = requireActivity().getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE)
        val correo = prefs.getString("correo_usuario", "") ?: ""
        if (correo.isNotEmpty()) {
            // El daño base al hacer click es pequeño, el fuerte viene de las tareas
            rpgViewModel.atacarJefe(5, correo) { derrotado ->
                if (derrotado) {
                    NotificationHelper.enviarNotificacionLogro(requireContext(), "¡Victoria!", "Has derrotado al jefe y ganado sus recompensas.")
                }
            }
        }
    }

    private fun observarViewModel() {
        lifecycleScope.launch {
            rpgViewModel.jefeActivo.collect { jefe ->
                if (jefeActual != null && jefe != null && jefe.hpActual < jefeActual!!.hpActual) {
                    // Si el HP ha bajado desde la última vez, animamos
                    ejecutarAnimacionDano()
                }
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
            
            // Llamada segura a la función suspend del ViewModel
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
                rpgViewModel.cargarJefeActivo(correo)
            }
        }
    }
}
