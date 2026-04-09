package com.example.xpeando.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.xpeando.R
import com.example.xpeando.activities.MainActivity
import com.example.xpeando.adapters.InventarioAdapter
import com.example.xpeando.adapters.RecompensasAdapter
import com.example.xpeando.database.DBHelper
import com.example.xpeando.model.Recompensa
import com.example.xpeando.model.Articulo
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout

class FragmentRecompensas : Fragment() {

    private lateinit var db: DBHelper
    private lateinit var rvRecompensas: RecyclerView
    private lateinit var tvSaldo: TextView
    private lateinit var tabLayout: TabLayout
    private lateinit var fabAnadir: FloatingActionButton
    private var correoUsuario: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_recompensas, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = DBHelper(requireContext())
        val prefs = requireActivity().getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE)
        correoUsuario = prefs.getString("correo_usuario", "") ?: ""
        
        rvRecompensas = view.findViewById(R.id.rv_recompensas)
        rvRecompensas.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        tvSaldo = view.findViewById(R.id.tv_saldo_monedas)
        tabLayout = view.findViewById(R.id.tab_layout_tienda)
        fabAnadir = view.findViewById(R.id.fab_anadir_recompensa)

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                actualizarListaSegunTab()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        fabAnadir.setOnClickListener {
            mostrarDialogoAnadirRecompensa()
        }

        actualizarSaldo()
        actualizarListaSegunTab()
    }

    private fun actualizarListaSegunTab() {
        if (tabLayout.selectedTabPosition == 0) {
            // "Mis Premios" - Mantener lista vertical
            fabAnadir.visibility = View.VISIBLE
            rvRecompensas.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
            configurarListaRecompensasPersonales()
        } else {
            // "Armería" - Cambiar a Cuadrícula de 3 columnas
            fabAnadir.visibility = View.GONE
            rvRecompensas.layoutManager = androidx.recyclerview.widget.GridLayoutManager(requireContext(), 3)
            configurarListaArmeria()
        }
    }

    private fun configurarListaRecompensasPersonales() {
        val adaptador = RecompensasAdapter(
            listaRecompensas = db.obtenerTodasRecompensas(correoUsuario),
            onComprarClick = { recompensa ->
                val usuario = db.obtenerUsuarioLogueado(correoUsuario)
                if (usuario != null && usuario.monedas >= recompensa.precio) {
                    db.actualizarProgresoUsuario(correoUsuario, 0, -recompensa.precio)
                    Toast.makeText(requireContext(), "¡Has canjeado ${recompensa.nombre}!", Toast.LENGTH_SHORT).show()
                    (activity as? MainActivity)?.actualizarHeader()
                    actualizarSaldo()
                } else {
                    Toast.makeText(requireContext(), "No tienes suficientes monedas", Toast.LENGTH_SHORT).show()
                }
            },
            onLongClick = { recompensa ->
                mostrarDialogoEliminar(recompensa)
            }
        )
        rvRecompensas.adapter = adaptador
    }

    private fun configurarListaArmeria() {
        val armeria = db.obtenerTiendaRPG()
        val adaptador = InventarioAdapter(armeria) { articulo ->
            val usuario = db.obtenerUsuarioLogueado(correoUsuario)
            if (usuario != null && usuario.monedas >= articulo.precio) {
                if (db.comprarArticulo(correoUsuario, articulo)) {
                    Toast.makeText(requireContext(), "¡Has comprado ${articulo.nombre}!", Toast.LENGTH_SHORT).show()
                    (activity as? MainActivity)?.actualizarHeader()
                    actualizarSaldo()
                }
            } else {
                Toast.makeText(requireContext(), "No tienes suficientes monedas", Toast.LENGTH_SHORT).show()
            }
        }
        rvRecompensas.adapter = adaptador
    }

    private fun mostrarDialogoAnadirRecompensa() {
        val vista = layoutInflater.inflate(R.layout.dialogo_nueva_recompensa, null)
        val etNombre = vista.findViewById<EditText>(R.id.et_nombre_recompensa_dialogo)
        val etPrecio = vista.findViewById<EditText>(R.id.et_precio_recompensa_dialogo)
        val btnAceptar = vista.findViewById<Button>(R.id.btn_aceptar_recompensa)
        val btnCancelar = vista.findViewById<Button>(R.id.btn_cancelar_recompensa)
        
        val dialog = AlertDialog.Builder(requireContext()).setView(vista).create()

        btnAceptar.setOnClickListener {
            val nombre = etNombre.text.toString()
            val precioStr = etPrecio.text.toString()
            if (nombre.isNotEmpty() && precioStr.isNotEmpty()) {
                db.insertarRecompensa(Recompensa(
                    correo_usuario = correoUsuario,
                    nombre = nombre,
                    precio = precioStr.toIntOrNull() ?: 0
                ))
                actualizarListaSegunTab()
                Toast.makeText(requireContext(), "¡Tesoro '$nombre' añadido!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }
        btnCancelar.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun mostrarDialogoEliminar(recompensa: Recompensa) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Recompensa")
            .setMessage("¿Eliminar '${recompensa.nombre}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                db.eliminarRecompensa(recompensa.id)
                actualizarListaSegunTab()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun actualizarSaldo() {
        val usuario = db.obtenerUsuarioLogueado(correoUsuario)
        tvSaldo.text = "${usuario?.monedas ?: 0}"
    }
}
