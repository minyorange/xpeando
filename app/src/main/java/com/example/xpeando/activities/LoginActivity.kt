package com.example.xpeando.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.xpeando.R
import androidx.activity.viewModels
import com.example.xpeando.viewmodel.UsuarioViewModel
import com.example.xpeando.viewmodel.ViewModelFactory
import com.example.xpeando.repository.DataRepository
import com.example.xpeando.utils.XpeandoToast
import com.google.android.material.card.MaterialCardView

class LoginActivity : AppCompatActivity() {

    private val viewModel: UsuarioViewModel by viewModels { ViewModelFactory() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // db = DBHelper(this)

        // --- ANIMACIÓN LOGO ---
        val cardLogo = findViewById<MaterialCardView>(R.id.card_logo)
        val animation = AnimationUtils.loadAnimation(this, R.anim.float_animation)
        cardLogo.startAnimation(animation)

        val etCorreo = findViewById<EditText>(R.id.et_correo)
        val etContrasena = findViewById<EditText>(R.id.et_contrasena)
        val cbRecordar = findViewById<android.widget.CheckBox>(R.id.cb_recordar_sesion)
        val btnLogin = findViewById<Button>(R.id.btn_login)
        val tvIrARegistro = findViewById<TextView>(R.id.tv_ir_a_registro)

        // --- COMPROBAR SESIÓN AUTOMÁTICA Y AUTOCOMPLETAR ---
        val prefs = getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE)
        val sesionActiva = prefs.getBoolean("sesion_activa", false)
        val correoGuardado = prefs.getString("correo_usuario", "")

        // Autocompletar el correo si existe
        if (!correoGuardado.isNullOrEmpty()) {
            etCorreo.setText(correoGuardado)
            cbRecordar.isChecked = sesionActiva
        }

        if (sesionActiva && !correoGuardado.isNullOrEmpty()) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnLogin.setOnClickListener {
            val correo = etCorreo.text.toString()
            val contrasena = etContrasena.text.toString()

            if (correo.isNotEmpty() && contrasena.isNotEmpty()) {
                viewModel.loginUsuarioFirebase(correo, contrasena) { exito, error ->
                    if (exito) {
                        // --- GUARDAR SESIÓN DEL USUARIO ---
                        val editor = prefs.edit()
                        editor.putString("correo_usuario", correo)
                        editor.putBoolean("sesion_activa", cbRecordar.isChecked)
                        editor.apply()

                        XpeandoToast.success(this@LoginActivity, "¡Bienvenido de nuevo!")
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        XpeandoToast.error(this@LoginActivity, "Error: ${error ?: "Credenciales incorrectas"}")
                    }
                }
            } else {
                XpeandoToast.info(this@LoginActivity, "Por favor, rellena todos los campos")
            }
        }

        tvIrARegistro.setOnClickListener {
            val intent = Intent(this, RegistroActivity::class.java)
            startActivity(intent)
        }
    }
}
