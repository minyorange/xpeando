package com.example.xpeando.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.xpeando.R
import com.example.xpeando.adapters.LogrosAdapter
import com.example.xpeando.database.DBHelper
import com.example.xpeando.repository.DataRepository
import com.example.xpeando.viewmodel.LogrosViewModel
import com.example.xpeando.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

class FragmentLogros : Fragment() {

    private val logrosViewModel: LogrosViewModel by viewModels {
        ViewModelFactory(DataRepository(DBHelper(requireContext())))
    }
    private lateinit var rvLogros: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_logros, container, false)
        rvLogros = view.findViewById(R.id.rv_lista_logros)
        rvLogros.layoutManager = GridLayoutManager(requireContext(), 2)
        
        observarViewModel()
        
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cargarLogros()
    }

    private fun cargarLogros() {
        val prefs = requireContext().getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE)
        val correo = prefs.getString("correo_usuario", "") ?: ""
        logrosViewModel.cargarLogros(correo)
    }

    private fun observarViewModel() {
        lifecycleScope.launch {
            logrosViewModel.logros.collect { lista ->
                rvLogros.adapter = LogrosAdapter(lista)
            }
        }
    }
}
