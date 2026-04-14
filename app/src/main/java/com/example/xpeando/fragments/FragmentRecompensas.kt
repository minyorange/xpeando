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
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.xpeando.R
import com.example.xpeando.activities.MainActivity
import com.example.xpeando.adapters.InventarioAdapter
import com.example.xpeando.adapters.RecompensasAdapter
import com.example.xpeando.database.DBHelper
import com.example.xpeando.model.Articulo
import com.example.xpeando.model.Recompensa
import com.example.xpeando.repository.DataRepository
import com.example.xpeando.utils.XpeandoToast
import com.example.xpeando.viewmodel.RecompensasViewModel
import com.example.xpeando.viewmodel.ViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

class FragmentRecompensas : Fragment() {

    private val viewModel: RecompensasViewModel by viewModels { ViewModelFactory(DataRepository(DBHelper(requireContext()))) }
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

        val prefs = requireActivity().getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE)
        correoUsuario = prefs.getString("correo_usuario", "") ?: ""
        
        rvRecompensas = view.findViewById(R.id.rv_recompensas)
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

        observarViewModel()
        
        if (correoUsuario.isNotEmpty()) {
            viewModel.cargarDatos(correoUsuario)
        }
    }

    private fun observarViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    tvSaldo.text = "${state.usuario?.monedas ?: 0}"
                    actualizarListaSegunTab(state)
                }
            }
        }
    }

    private fun actualizarListaSegunTab(state: com.example.xpeando.viewmodel.RecompensasState? = null) {
        val currentState = state ?: viewModel.state.value
        if (tabLayout.selectedTabPosition == 0) {
            fabAnadir.visibility = View.VISIBLE
            rvRecompensas.layoutManager = LinearLayoutManager(requireContext())
            configurarListaRecompensasPersonales(currentState.recompensasPersonales)
        } else {
            fabAnadir.visibility = View.GONE
            rvRecompensas.layoutManager = GridLayoutManager(requireContext(), 3)
            configurarListaArmeria(currentState.armeria)
        }
    }

    private fun configurarListaRecompensasPersonales(lista: List<Recompensa>) {
        val adaptador = RecompensasAdapter(
            listaRecompensas = lista,
            onComprarClick = { recompensa ->
                viewModel.canjearRecompensaPersonal(requireContext(), correoUsuario, recompensa) { exito, mensaje ->
                    if (exito) {
                        XpeandoToast.success(requireContext(), mensaje)
                        (activity as? MainActivity)?.actualizarHeader()
                    } else {
                        XpeandoToast.error(requireContext(), mensaje)
                    }
                }
            },
            onLongClick = { recompensa ->
                mostrarDialogoEliminar(recompensa)
            }
        )
        rvRecompensas.adapter = adaptador
    }

    private fun configurarListaArmeria(lista: List<Articulo>) {
        val adaptador = InventarioAdapter(lista) { articulo ->
            viewModel.comprarArticuloArmeria(requireContext(), correoUsuario, articulo) { exito, mensaje ->
                if (exito) {
                    XpeandoToast.success(requireContext(), mensaje)
                    (activity as? MainActivity)?.actualizarHeader()
                } else {
                    XpeandoToast.error(requireContext(), mensaje)
                }
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
                viewModel.insertarRecompensa(Recompensa(
                    correo_usuario = correoUsuario,
                    nombre = nombre,
                    precio = precioStr.toIntOrNull() ?: 0
                ))
                XpeandoToast.success(requireContext(), "¡Tesoro '$nombre' añadido!")
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
                viewModel.eliminarRecompensa(recompensa.id, correoUsuario)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
