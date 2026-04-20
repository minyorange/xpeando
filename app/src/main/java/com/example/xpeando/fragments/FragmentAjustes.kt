package com.example.xpeando.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.xpeando.R
import com.example.xpeando.activities.LoginActivity
import com.example.xpeando.viewmodel.UsuarioViewModel
import com.example.xpeando.viewmodel.ViewModelFactory

class FragmentAjustes : Fragment() {

    private val usuarioViewModel: UsuarioViewModel by activityViewModels { ViewModelFactory() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_ajustes, container, false)

        val btnBorrarCuenta = view.findViewById<Button>(R.id.btn_borrar_cuenta)
        btnBorrarCuenta.setOnClickListener {
            mostrarConfirmacionBorrado()
        }

        return view
    }

    private fun mostrarConfirmacionBorrado() {
        AlertDialog.Builder(requireContext())
            .setTitle("¡ATENCIÓN!")
            .setMessage("¿Estás seguro de que deseas borrar tu cuenta permanentemente? Perderás todo tu progreso, nivel, monedas y héroe. Esta acción no se puede deshacer.")
            .setPositiveButton("SÍ, BORRAR TODO") { _, _ ->
                ejecutarBorradoCuenta()
            }
            .setNegativeButton("CANCELAR", null)
            .show()
    }

    private fun ejecutarBorradoCuenta() {
        val prefs = requireActivity().getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE)
        val correo = prefs.getString("correo_usuario", "") ?: ""

        if (correo.isNotEmpty()) {
            usuarioViewModel.borrarCuenta(correo) { exito ->
                if (exito) {
                    // Limpiar preferencias locales
                    prefs.edit().clear().apply()
                    
                    Toast.makeText(requireContext(), "Cuenta eliminada correctamente", Toast.LENGTH_LONG).show()
                    
                    // Ir a Login y cerrar todo lo demás
                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                } else {
                    Toast.makeText(requireContext(), "Error al eliminar la cuenta. Inténtalo de nuevo.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}