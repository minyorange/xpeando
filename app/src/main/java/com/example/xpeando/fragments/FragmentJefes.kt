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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import androidx.recyclerview.widget.RecyclerView
import com.example.xpeando.R
import com.example.xpeando.adapters.HistorialJefesAdapter
import com.example.xpeando.model.Jefe
import com.example.xpeando.utils.NotificationHelper
import com.example.xpeando.utils.XpeandoToast
import com.example.xpeando.viewmodel.RpgViewModel
import com.example.xpeando.viewmodel.UsuarioViewModel
import com.example.xpeando.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

class FragmentJefes : Fragment() {

    private val rpgViewModel: RpgViewModel by activityViewModels { ViewModelFactory() }
    private val userViewModel: UsuarioViewModel by activityViewModels { ViewModelFactory() }
    
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

        ivInfo.setOnClickListener { mostrarTutorialJefes() }
        // ivJefe.setOnClickListener { atacarAlJefe() } // Desactivado ataque por clic

        observarViewModel()
        
        val prefs = requireActivity().getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE)
        val correo = prefs.getString("correo_usuario", "") ?: ""
        rpgViewModel.cargarJefeActivo(correo)
        rpgViewModel.cargarHistorial(correo)

        return view
    }

    private fun atacarAlJefe() {
        val correo = requireActivity().getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE).getString("correo_usuario", "") ?: ""
        if (correo.isNotEmpty()) {
            rpgViewModel.atacarJefe(requireContext(), 5, correo) { derrotado ->
                if (derrotado) {
                    jefeActual?.let {
                        userViewModel.actualizarProgreso(correo, it.recompensaXP, it.recompensaMonedas)
                    }
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

                    if (rpgViewModel.hpAntesDeCambio != -1 && !jefe.derrotado) {
                        val diferencia = rpgViewModel.hpAntesDeCambio - jefe.hpActual
                        if (diferencia > 0) {
                            if (jefeActual == null) delay(600) 
                            ejecutarAnimacionDano(diferencia)
                        }
                    }

                    if (!jefe.derrotado) rpgViewModel.hpAntesDeCambio = jefe.hpActual
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
                rvHistorial.adapter = HistorialJefesAdapter(historial)
            }
        }
    }

    private fun actualizarUIJefe(jefe: Jefe?) {
        countDownTimer?.cancel()
        if (jefe != null && !jefe.derrotado) {
            containerJefePrincipal.visibility = View.VISIBLE
            tvNombreJefe.text = jefe.nombre
            tvDescripcionJefe.text = jefe.descripcion
            tvHpJefe.text = "HP: ${jefe.hpActual} / ${jefe.hpMax}"
            pbHpJefe.max = jefe.hpMax
            pbHpJefe.progress = jefe.hpActual
            
            tvRecompensas.text = "Recompensas: ${jefe.recompensaMonedas}"
            tvRecompensasXP.text = "| ${jefe.recompensaXP} XP"
            cvJefe.visibility = View.VISIBLE
            ivJefe.visibility = View.VISIBLE
            tvContadorReaparicion.visibility = View.GONE

            val resId = resources.getIdentifier(jefe.icono, "drawable", requireContext().packageName)
            ivJefe.setImageResource(if (resId != 0) resId else R.drawable.ic_boss_dragon)
        } else {
            containerJefePrincipal.visibility = View.GONE
            tvNombreJefe.text = "¡Jefe Derrotado!"
            tvHpJefe.text = "Esperando reaparición..."
            pbHpJefe.progress = 0
            cvJefe.visibility = View.GONE
            ivJefe.visibility = View.GONE
            iniciarContadorReaparicion()
        }
    }

    private fun iniciarContadorReaparicion() {
        lifecycleScope.launch {
            val correo = requireActivity().getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE).getString("correo_usuario", "") ?: ""
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
                        if (isAdded) rpgViewModel.cargarJefeActivo(correo)
                    }
                }.start()
            } else {
                rpgViewModel.cargarJefeActivo(correo)
            }
        }
    }

    private fun ejecutarAnimacionDano(danio: Int) {
        ivJefe.setColorFilter(android.graphics.Color.RED)
        tvDanioFlotante.text = "-$danio HP"
        tvDanioFlotante.visibility = View.VISIBLE
        tvDanioFlotante.animate().translationY(-250f).alpha(0f).setDuration(1000).withEndAction {
            tvDanioFlotante.visibility = View.INVISIBLE
            tvDanioFlotante.translationY = 0f
            tvDanioFlotante.alpha = 1f
            ivJefe.clearColorFilter()
        }.start()
    }

    private fun mostrarTutorialJefes() {
        val vista = layoutInflater.inflate(R.layout.dialogo_tutorial_jefes, null)
        val dialog = AlertDialog.Builder(requireContext()).setView(vista).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        vista.findViewById<Button>(R.id.btn_tutorial_jefes_siguiente).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        val correo = requireActivity().getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE).getString("correo_usuario", "") ?: ""
        if (correo.isNotEmpty()) {
            rpgViewModel.cargarJefeActivo(correo)
            rpgViewModel.cargarHistorial(correo)
        }
    }

    override fun onPause() {
        super.onPause()
        countDownTimer?.cancel()
    }
}
