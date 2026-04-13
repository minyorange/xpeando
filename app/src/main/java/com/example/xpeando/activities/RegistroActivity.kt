package com.example.xpeando.activities

import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.xpeando.R
import com.example.xpeando.database.DBHelper
import com.example.xpeando.model.Usuario

class RegistroActivity : AppCompatActivity() {

    private lateinit var db: DBHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        db = DBHelper(this)

        val imgDragon = findViewById<ImageView>(R.id.img_dragon_anim)
        val animation = AnimationUtils.loadAnimation(this, R.anim.float_animation)
        imgDragon.startAnimation(animation)

        val etNombre = findViewById<EditText>(R.id.et_nombre_registro)
        val etCorreo = findViewById<EditText>(R.id.et_correo_registro)
        val etContrasena = findViewById<EditText>(R.id.et_contrasena_registro)
        val etConfirmarContrasena = findViewById<EditText>(R.id.et_confirmar_contrasena_registro)
        val btnRegistrar = findViewById<Button>(R.id.btn_registrar)
        val tvVolverAlLogin = findViewById<TextView>(R.id.tv_volver_al_login)

        btnRegistrar.setOnClickListener {
            val nombre = etNombre.text.toString()
            val correo = etCorreo.text.toString()
            val contrasena = etContrasena.text.toString()
            val confirmarContrasena = etConfirmarContrasena.text.toString()

            if (nombre.isNotEmpty() && correo.isNotEmpty() && contrasena.isNotEmpty() && confirmarContrasena.isNotEmpty()) {
                if (contrasena == confirmarContrasena) {
                    val nuevoUsuario = Usuario(nombre = nombre, correo = correo, contrasena = contrasena)
                    val id = db.registrarUsuario(nuevoUsuario)

                    if (id != -1L) {
                        Toast.makeText(this, "Registro con éxito", Toast.LENGTH_SHORT).show()
                        finish() // Vuelve al Login
                    } else {
                        Toast.makeText(this, "Error: El correo ya existe", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Por favor, rellena todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        tvVolverAlLogin.setOnClickListener {
            finish()
        }
    }
}
