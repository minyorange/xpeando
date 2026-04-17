package com.example.xpeando.activities

import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.xpeando.R
import androidx.activity.viewModels
import com.example.xpeando.viewmodel.UsuarioViewModel
import com.example.xpeando.viewmodel.ViewModelFactory
import com.example.xpeando.repository.DataRepository
import com.example.xpeando.utils.XpeandoToast
import com.example.xpeando.model.Usuario

class RegistroActivity : AppCompatActivity() {

    private val viewModel: UsuarioViewModel by viewModels { ViewModelFactory(DataRepository()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

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
                    val nuevoUsuario = Usuario(nombre = nombre, correo = correo)
                    viewModel.registrarUsuarioFirebase(nuevoUsuario, contrasena) { exito, error ->
                        if (exito) {
                            XpeandoToast.success(this@RegistroActivity, "¡Cuenta creada en la nube!")
                            finish()
                        } else {
                            XpeandoToast.error(this@RegistroActivity, "Error: ${error ?: "Error desconocido"}")
                        }
                    }
                } else {
                    XpeandoToast.error(this@RegistroActivity, "Las contraseñas no coinciden")
                }
            } else {
                XpeandoToast.info(this@RegistroActivity, "Por favor, rellena todos los campos")
            }
        }

        tvVolverAlLogin.setOnClickListener {
            finish()
        }
    }
}
