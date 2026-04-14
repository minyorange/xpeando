package com.example.xpeando.utils

import android.content.Context
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.xpeando.R

object XpeandoToast {

    fun show(context: Context, message: String, iconResId: Int = R.mipmap.ic_lan, duration: Int = Toast.LENGTH_SHORT) {
        val layoutInflater = LayoutInflater.from(context)
        val layout = layoutInflater.inflate(R.layout.layout_custom_toast, null)

        val textView: TextView = layout.findViewById(R.id.tv_toast_text)
        val imageView: ImageView = layout.findViewById(R.id.iv_toast_icon)

        textView.text = message
        imageView.setImageResource(iconResId)

        val toast = Toast(context)
        toast.duration = duration
        toast.view = layout
        toast.show()
    }

    // Funciones de utilidad para tipos específicos de Toast
    fun success(context: Context, message: String) {
        show(context, message, R.mipmap.ic_lan) // O un icono de éxito si tienes
    }

    fun error(context: Context, message: String) {
        show(context, message, R.mipmap.ic_lan) // O un icono de error si tienes
    }
    
    fun info(context: Context, message: String) {
        show(context, message, R.mipmap.ic_lan)
    }
}