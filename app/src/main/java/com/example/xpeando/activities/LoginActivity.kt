package com.example.xpeando.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.xpeando.R
import com.example.xpeando.database.DBHelper

class LoginActivity : AppCompatActivity() {

    private lateinit var db: DBHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        db = DBHelper(this)

        val etCorreo = findViewById<EditText>(R.id.et_correo)
        val etContrasena = findViewById<EditText>(R.id.et_contrasena)
        val cbRecordar = findViewById<android.widget.CheckBox>(R.id.cb_recordar_sesion)
        val btnLogin = findViewById<Button>(R.id.btn_login)
        val tvIrARegistro = findViewById<TextView>(R.id.tv_ir_a_registro)

        // --- COMPROBAR SESIÓN AUTOMÁTICA ---
        val prefs = getSharedPreferences("XpeandoPrefs", Context.MODE_PRIVATE)
        val sesionActiva = prefs.getBoolean("sesion_activa", false)
        val correoGuardado = prefs.getString("correo_usuario", "")

        if (sesionActiva && !correoGuardado.isNullOrEmpty()) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnLogin.setOnClickListener {
            val correo = etCorreo.text.toString()
            val contrasena = etContrasena.text.toString()

            if (correo.isNotEmpty() && contrasena.isNotEmpty()) {
                if (db.validarUsuario(correo, contrasena)) {
                    // --- GUARDAR SESIÓN DEL USUARIO ---
                    val editor = prefs.edit()
                    editor.putString("correo_usuario", correo)
                    editor.putBoolean("sesion_activa", cbRecordar.isChecked)
                    editor.apply()

                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Correo o contraseña incorrectos", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Por favor, rellena todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        tvIrARegistro.setOnClickListener {
            val intent = Intent(this, RegistroActivity::class.java)
            startActivity(intent)
        }
    }
}
