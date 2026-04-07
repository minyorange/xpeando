package com.example.xpeando.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.xpeando.R
import com.example.xpeando.adapters.LogrosAdapter
import com.example.xpeando.database.DBHelper
import com.example.xpeando.utils.LogroManager

class FragmentLogros : Fragment() {

    private lateinit var db: DBHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_logros, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = DBHelper(requireContext())
        val rvLogros = view.findViewById<RecyclerView>(R.id.rv_lista_logros)

        val prefs = requireContext().getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE)
        val correo = prefs.getString("correo_usuario", "") ?: ""
        
        val usuario = db.obtenerUsuarioLogueado(correo)
        usuario?.let { user ->
            val listaLogros = LogroManager.obtenerLogrosDefinidos(db, user)
            rvLogros.layoutManager = GridLayoutManager(requireContext(), 2)
            rvLogros.adapter = LogrosAdapter(listaLogros)
        }
    }
}
