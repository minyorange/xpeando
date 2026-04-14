package com.example.xpeando.fragments

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.xpeando.R
import com.example.xpeando.adapters.NotasAdapter
import com.example.xpeando.database.DBHelper
import com.example.xpeando.model.Nota
import com.example.xpeando.repository.DataRepository
import com.example.xpeando.viewmodel.NotasViewModel
import com.example.xpeando.viewmodel.ViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class FragmentNotas : Fragment() {

    private val viewModel: NotasViewModel by viewModels { ViewModelFactory(DataRepository(DBHelper(requireContext()))) }
    private lateinit var rvNotas: RecyclerView
    private lateinit var fabAnadir: FloatingActionButton
    private lateinit var adapter: NotasAdapter
    private var correoUsuario: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notas, container, false)

        rvNotas = view.findViewById(R.id.rv_notas)
        fabAnadir = view.findViewById(R.id.fab_anadir_nota)

        val prefs = requireActivity().getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE)
        correoUsuario = prefs.getString("correo_usuario", "") ?: ""

        configurarRecyclerView()
        observarViewModel()

        fabAnadir.setOnClickListener {
            mostrarDialogoNota()
        }

        if (correoUsuario.isNotEmpty()) {
            viewModel.cargarNotas(correoUsuario)
        }

        return view
    }

    private fun observarViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.notas.collect { lista ->
                    adapter.actualizarLista(lista)
                }
            }
        }
    }

    private fun configurarRecyclerView() {
        adapter = NotasAdapter(
            emptyList(),
            onNotaClick = { nota -> mostrarDialogoNota(nota) },
            onNotaLongClick = { nota -> confirmarEliminacion(nota) }
        )
        rvNotas.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        rvNotas.adapter = adapter
    }

    private fun mostrarDialogoNota(notaExistente: Nota? = null) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialogo_nota, null)
        val etTitulo = dialogView.findViewById<EditText>(R.id.et_titulo_nota_dialogo)
        val etContenido = dialogView.findViewById<EditText>(R.id.et_contenido_nota_dialogo)

        if (notaExistente != null) {
            etTitulo.setText(notaExistente.titulo)
            etContenido.setText(notaExistente.contenido)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(if (notaExistente == null) "Nueva Nota" else "Editar Nota")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val titulo = etTitulo.text.toString().trim()
                val contenido = etContenido.text.toString().trim()

                if (titulo.isNotEmpty() || contenido.isNotEmpty()) {
                    if (notaExistente == null) {
                        val nuevaNota = Nota(correo_usuario = correoUsuario, titulo = titulo, contenido = contenido)
                        viewModel.insertarNota(nuevaNota)
                    } else {
                        notaExistente.titulo = titulo
                        notaExistente.contenido = contenido
                        viewModel.actualizarNota(notaExistente)
                    }
                } else {
                    Toast.makeText(context, "La nota no puede estar vacía", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarEliminacion(nota: Nota) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Nota")
            .setMessage("¿Estás seguro de que quieres eliminar esta nota?")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.eliminarNota(nota.id, correoUsuario)
                Toast.makeText(context, "Nota eliminada", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}