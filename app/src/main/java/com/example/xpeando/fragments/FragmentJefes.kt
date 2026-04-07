package com.example.xpeando.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.xpeando.R
import com.example.xpeando.database.DBHelper
import com.example.xpeando.model.Jefe

class FragmentJefes : Fragment() {

    private lateinit var dbHelper: DBHelper
    private var jefeActual: Jefe? = null

    private lateinit var tvNombreJefe: TextView
    private lateinit var tvHpJefe: TextView
    private lateinit var pbHpJefe: ProgressBar
    private lateinit var ivJefe: ImageView
    private lateinit var tvRecompensas: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_jefes, container, false)
        dbHelper = DBHelper(requireContext())

        tvNombreJefe = view.findViewById(R.id.tvNombreJefe)
        tvHpJefe = view.findViewById(R.id.tvHpJefe)
        pbHpJefe = view.findViewById(R.id.pbHpJefe)
        ivJefe = view.findViewById(R.id.ivJefe)
        tvRecompensas = view.findViewById(R.id.tvRecompensasJefe)

        cargarJefe()

        return view
    }

    override fun onResume() {
        super.onResume()
        cargarJefe()
    }

    private fun cargarJefe() {
        jefeActual = dbHelper.obtenerJefeActivo()
        jefeActual?.let {
            tvNombreJefe.text = it.nombre
            tvHpJefe.text = "HP: ${it.hpActual} / ${it.hpMax}"
            pbHpJefe.max = it.hpMax
            pbHpJefe.progress = it.hpActual
            
            tvRecompensas.text = "Recompensas: ${it.recompensaMonedas} Oro | ${it.recompensaXP} XP"

            // Mapeo de iconos locales
            val resId = resources.getIdentifier(it.icono, "drawable", requireContext().packageName)
            if (resId != 0) {
                ivJefe.setImageResource(resId)
            } else {
                ivJefe.setImageResource(R.drawable.ic_boss_dragon) // Fallback
            }
        } ?: run {
            tvNombreJefe.text = "¡Todos los jefes derrotados!"
            tvHpJefe.text = "Paz en el reino"
            pbHpJefe.progress = 0
            ivJefe.visibility = View.GONE
            tvRecompensas.visibility = View.GONE
        }
    }
}
