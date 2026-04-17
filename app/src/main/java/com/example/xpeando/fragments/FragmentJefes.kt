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
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
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

    private val rpgViewModel: RpgViewModel by activityViewModels {
        ViewModelFactory(DataRepository())
    }
    private var jefeActual: Jefe? = null

    private lateinit var cvJefe: androidx.cardview.widget.CardView
    private lateinit var tvNombreJefe: TextView
    private lateinit var tvDescripcionJefe: TextView
    private lateinit var tvHpJefe: TextView
    private lateinit var pbHpJefe: ProgressBar
    private lateinit var ivJefe: ImageView
    private lateinit var tvRecompensas: TextView
    private lateinit var tvRecompensasXP: TextView
    private lateinit var containerJefePrincipal: View
    private lateinit var tvContadorReaparicion: TextView
    private lateinit var tvDanioFlotante: TextView
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
        cvJefe = view.findViewById(R.id.cvJefe)
        tvRecompensas = view.findViewById(R.id.tvRecompensasJefe)
        tvRecompensasXP = view.findViewById(R.id.tvRecompensasXPJefe)
        containerJefePrincipal = view.findViewById(R.id.containerJefePrincipal)
        tvContadorReaparicion = view.findViewById(R.id.tvContadorReaparicion)
        tvDanioFlotante = view.findViewById(R.id.tvDanioFlotante)
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

    private fun ejecutarAnimacionDano(danio: Int = 0) {
        // 1. Efecto de "Flash Rojo" Intenso
        ivJefe.setColorFilter(android.graphics.Color.parseColor("#FFFF0000")) 
        
        // 2. Mostrar Daño Flotante más grande
        if (danio > 0) {
            tvDanioFlotante.text = "-$danio HP"
            tvDanioFlotante.visibility = View.VISIBLE
            tvDanioFlotante.translationY = 0f
            tvDanioFlotante.alpha = 1f
            tvDanioFlotante.scaleX = 1.4f
            tvDanioFlotante.scaleY = 1.4f
            
            tvDanioFlotante.animate()
                .translationY(-250f)
                .alpha(0f)
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(1200)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .withEndAction {
                    tvDanioFlotante.visibility = View.INVISIBLE
                }
        }

        // 3. Vibración "Explosiva" (Sacudida triple)
        val shakeDist = 45f
        cvJefe.animate()
            .translationX(shakeDist)
            .scaleX(0.8f)
            .scaleY(0.8f)
            .setDuration(60)
            .withEndAction {
                cvJefe.animate()
                    .translationX(-shakeDist)
                    .setDuration(60)
                    .withEndAction {
                        cvJefe.animate()
                            .translationX(shakeDist / 2)
                            .setDuration(60)
                            .withEndAction {
                                cvJefe.animate()
                                    .translationX(0f)
                                    .scaleX(1.0f)
                                    .scaleY(1.0f)
                                    .setDuration(60)
                                    .withEndAction {
                                        ivJefe.clearColorFilter()
                                    }
                            }
                    }
            }
    }

    private fun atacarAlJefe() {
        val prefs = requireActivity().getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE)
        val correo = prefs.getString("correo_usuario", "") ?: ""
        if (correo.isNotEmpty()) {
            // El daño base al hacer click es pequeño, el fuerte viene de las tareas
            rpgViewModel.atacarJefe(requireContext(), 5, correo) { derrotado ->
                if (derrotado) {
                    NotificationHelper.enviarNotificacionLogro(requireContext(), "¡Victoria!", "Has derrotado al jefe y ganado sus recompensas.")
                }
            }
        }
    }

    private fun observarViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                rpgViewModel.jefeActivo.collect { jefe ->
                    if (jefe == null) return@collect

                    // Detectar daño (tanto en tiempo real como al volver de otra pestaña)
                    if (rpgViewModel.hpAntesDeCambio != -1 && !jefe.derrotado) {
                        val diferencia = rpgViewModel.hpAntesDeCambio - jefe.hpActual
                        if (diferencia > 0) {
                            // Si acabamos de entrar (jefeActual es null), esperamos a que se vea la UI
                            if (jefeActual == null) {
                                delay(600) 
                            }
                            ejecutarAnimacionDano(diferencia)
                        }
                    }

                    // Actualizar el HP recordado en el ViewModel para el siguiente cambio
                    if (!jefe.derrotado) {
                        rpgViewModel.hpAntesDeCambio = jefe.hpActual
                    }

                    // Animación de muerte
                    if (jefeActual != null && !jefeActual!!.derrotado && jefe.derrotado) {
                        ejecutarAnimacionDano(jefeActual!!.hpActual)
                    }
                    
                    actualizarUIJefe(jefe)
                    jefeActual = jefe
                }
            }
        }
        lifecycleScope.launch {
            rpgViewModel.historialJefes.collect { historial ->
                val adapter = HistorialJefesAdapter(historial)
                rvHistorial.adapter = adapter
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
        
        if (jefe != null && !jefe.derrotado) {
            containerJefePrincipal.visibility = View.VISIBLE
            tvNombreJefe.text = jefe.nombre
            tvDescripcionJefe.text = jefe.descripcion
            tvDescripcionJefe.visibility = View.VISIBLE
            tvHpJefe.text = "HP: ${jefe.hpActual} / ${jefe.hpMax}"
            
            pbHpJefe.max = jefe.hpMax
            
            // Si venimos de un HP mayor (daño pendiente de animar), empezamos la barra desde ahí
            val startHp = if (rpgViewModel.hpAntesDeCambio != -1 && rpgViewModel.hpAntesDeCambio > jefe.hpActual) {
                rpgViewModel.hpAntesDeCambio
            } else {
                jefe.hpActual
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                pbHpJefe.setProgress(jefe.hpActual, true)
            } else {
                pbHpJefe.progress = jefe.hpActual
            }
            
            // Forzar actualización del HP recordado si es la primera vez
            if (rpgViewModel.hpAntesDeCambio == -1) rpgViewModel.hpAntesDeCambio = jefe.hpActual
            
            tvRecompensas.text = "Recompensas: ${jefe.recompensaMonedas}"
            tvRecompensasXP.text = "| ${jefe.recompensaXP} XP"
            cvJefe.visibility = View.VISIBLE
            ivJefe.visibility = View.VISIBLE
            tvRecompensas.visibility = View.VISIBLE
            tvRecompensasXP.visibility = View.VISIBLE
            tvContadorReaparicion.visibility = View.GONE

            // Mapeo de iconos locales
            val resId = resources.getIdentifier(jefe.icono, "drawable", requireContext().packageName)
            if (resId != 0) {
                ivJefe.setImageResource(resId)
            } else {
                ivJefe.setImageResource(R.drawable.ic_boss_dragon) // Fallback
            }
            jefeActual = jefe
        } else {
            // JEFE DERROTADO O EN COOLDOWN
            jefeActual = jefe // Guardamos el estado actual (que tiene derrotado = true)
            containerJefePrincipal.visibility = View.GONE
            tvNombreJefe.text = "¡Jefe Derrotado!"
            tvHpJefe.text = "Esperando reaparición..."
            pbHpJefe.progress = 0
            cvJefe.visibility = View.GONE
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
