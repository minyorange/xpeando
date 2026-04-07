package com.example.xpeando.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.xpeando.R
import com.example.xpeando.activities.MainActivity
import com.example.xpeando.adapters.InventarioAdapter
import com.example.xpeando.database.DBHelper
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.Locale

class FragmentPersonaje : Fragment() {

    private lateinit var db: DBHelper
    private var correoUsuario: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_personaje, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = DBHelper(requireContext())
        
        val prefs = requireActivity().getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE)
        correoUsuario = prefs.getString("correo_usuario", "") ?: ""

        val fabMochila = view.findViewById<FloatingActionButton>(R.id.fab_mochila)
        fabMochila.setOnClickListener {
            mostrarMochila()
        }

        actualizarUI()
    }

    private fun actualizarUI() {
        val view = view ?: return
        val usuario = db.obtenerUsuarioLogueado(correoUsuario) ?: return

        val tvNombre = view.findViewById<TextView>(R.id.tv_nombre_personaje)
        val tvNivel = view.findViewById<TextView>(R.id.tv_nivel)
        val tvHPValor = view.findViewById<TextView>(R.id.tv_hp_valor)
        val pbHP = view.findViewById<ProgressBar>(R.id.pb_hp)
        val tvXPValor = view.findViewById<TextView>(R.id.tv_xp_valor)
        val pbExperiencia = view.findViewById<ProgressBar>(R.id.pb_experiencia)
        val tvMonedas = view.findViewById<TextView>(R.id.tv_monedas_personaje)
        
        val tvFuerza = view.findViewById<TextView>(R.id.tv_fuerza_valor)
        val tvInteligencia = view.findViewById<TextView>(R.id.tv_inteligencia_valor)
        val tvConstitucion = view.findViewById<TextView>(R.id.tv_constitucion_valor)
        val tvPercepcion = view.findViewById<TextView>(R.id.tv_percepcion_valor)
        
        val tvPuntos = view.findViewById<TextView>(R.id.tv_puntos_disponibles)
        val btnFza = view.findViewById<Button>(R.id.btn_subir_fza)
        val btnInt = view.findViewById<Button>(R.id.btn_subir_int)
        val btnCon = view.findViewById<Button>(R.id.btn_subir_con)
        val btnPer = view.findViewById<Button>(R.id.btn_subir_per)

        val inventario = db.obtenerInventario(correoUsuario)
        val equipado = inventario.filter { it.equipado }
        
        val bonusFza = equipado.sumOf { it.bonusFza }
        val bonusInt = equipado.sumOf { it.bonusInt }
        val bonusCon = equipado.sumOf { it.bonusCon }
        val bonusPer = equipado.sumOf { it.bonusPer }

        tvNombre.text = usuario.nombre
        tvNivel.text = "Nivel ${usuario.nivel}"
        tvHPValor.text = "${usuario.hp} / 50"
        pbHP.progress = usuario.hp
        tvXPValor.text = "${usuario.experiencia} / 100"
        pbExperiencia.progress = usuario.experiencia
        tvMonedas.text = "${usuario.monedas} Monedas"
        
        // Mostrar solo el número entero. El progreso decimal está guardado en la DB.
        tvFuerza.text = "${usuario.fuerza.toInt()} ${if (bonusFza > 0) "(+$bonusFza)" else ""}"
        tvInteligencia.text = "${usuario.inteligencia.toInt()} ${if (bonusInt > 0) "(+$bonusInt)" else ""}"
        tvConstitucion.text = "${usuario.constitucion.toInt()} ${if (bonusCon > 0) "(+$bonusCon)" else ""}"
        tvPercepcion.text = "${usuario.percepcion.toInt()} ${if (bonusPer > 0) "(+$bonusPer)" else ""}"

        if (usuario.puntosDisponibles > 0) {
            tvPuntos.visibility = View.VISIBLE
            tvPuntos.text = "${usuario.puntosDisponibles} Puntos Libres"
            btnFza.visibility = View.VISIBLE
            btnInt.visibility = View.VISIBLE
            btnCon.visibility = View.VISIBLE
            btnPer.visibility = View.VISIBLE
            
            btnFza.setOnClickListener { subirAtributo("fza") }
            btnInt.setOnClickListener { subirAtributo("int") }
            btnCon.setOnClickListener { subirAtributo("con") }
            btnPer.setOnClickListener { subirAtributo("per") }
        } else {
            tvPuntos.visibility = View.GONE
            btnFza.visibility = View.GONE
            btnInt.visibility = View.GONE
            btnCon.visibility = View.GONE
            btnPer.visibility = View.GONE
        }
    }

    private fun mostrarMochila() {
        val vista = layoutInflater.inflate(R.layout.dialogo_mochila, null)
        val rv = vista.findViewById<RecyclerView>(R.id.rv_inventario)
        val btnCerrar = vista.findViewById<Button>(R.id.btn_cerrar_mochila)
        
        val inventario = db.obtenerInventario(correoUsuario)
        val adapter = InventarioAdapter(inventario) { articulo ->
            if (articulo.tipo == "CONSUMIBLE" && articulo.subtipo == "POCION") {
                usarPocion(articulo.id, articulo.bonusHp)
            } else {
                db.equiparDesequipar(correoUsuario, articulo.id)
            }
            actualizarUI()
            (rv.adapter as? InventarioAdapter)?.actualizarLista(db.obtenerInventario(correoUsuario))
        }
        rv.adapter = adapter
        
        val dialog = AlertDialog.Builder(requireContext())
            .setView(vista)
            .create()
            
        btnCerrar.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun usarPocion(id: Int, curacion: Int) {
        val usuario = db.obtenerUsuarioLogueado(correoUsuario) ?: return
        var nuevaHp = usuario.hp + curacion
        if (nuevaHp > 50) nuevaHp = 50
        
        db.actualizarProgresoUsuario(correoUsuario, 0, 0, curacion)
        db.eliminarDelInventario(id)
        Toast.makeText(requireContext(), "¡Poción usada! +$curacion HP", Toast.LENGTH_SHORT).show()
        (activity as? MainActivity)?.actualizarHeader()
    }

    private fun subirAtributo(tipo: String) {
        when (tipo) {
            "fza" -> db.actualizarAtributos(correoUsuario, fza = 1.0, puntosUsados = 1)
            "int" -> db.actualizarAtributos(correoUsuario, int = 1.0, puntosUsados = 1)
            "con" -> db.actualizarAtributos(correoUsuario, con = 1.0, puntosUsados = 1)
            "per" -> db.actualizarAtributos(correoUsuario, per = 1.0, puntosUsados = 1)
        }
        actualizarUI()
    }
}
