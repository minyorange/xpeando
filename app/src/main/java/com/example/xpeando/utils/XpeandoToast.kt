package com.example.xpeando.utils

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.xpeando.R
import com.google.android.material.snackbar.Snackbar

object XpeandoToast {

    private var currentSnackbar: Snackbar? = null
    private var currentToast: Toast? = null

    // --- MENSAJES GENÉRICOS (Login, Bienvenido, etc.) ---
    fun show(context: Context, message: String, iconResId: Int = R.mipmap.ic_lan) {
        currentToast?.cancel()
        currentSnackbar?.dismiss()

        val layout = LayoutInflater.from(context).inflate(R.layout.layout_custom_toast, null)
        layout.findViewById<TextView>(R.id.tv_toast_text)?.text = message
        layout.findViewById<ImageView>(R.id.iv_toast_icon)?.setImageResource(iconResId)

        val toast = Toast(context)
        toast.duration = Toast.LENGTH_SHORT
        toast.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 200)
        toast.view = layout
        currentToast = toast
        toast.show()
    }

    // --- PROGRESO (Habitos, Tareas y Dailies) ---
    fun mostrarProgreso(context: Context, xp: Int, monedas: Int, esDaily: Boolean = false) {
        val activity = context as? android.app.Activity
        val rootView = activity?.findViewById<View>(android.R.id.content)

        if (rootView != null) {
            currentToast?.cancel()
            currentSnackbar?.dismiss()
            
            val snackbar = Snackbar.make(rootView, "", Snackbar.LENGTH_SHORT)
            val snackbarLayout = snackbar.view as ViewGroup
            snackbarLayout.removeAllViews()
            snackbarLayout.setBackgroundColor(android.graphics.Color.TRANSPARENT)

            val layoutRes = if (esDaily) R.layout.layout_toast_daily_completada else R.layout.layout_toast_progreso
            val customView = LayoutInflater.from(context).inflate(layoutRes, null)

            if (esDaily) {
                customView.findViewById<TextView>(R.id.tv_xp_daily)?.text = "+$xp XP"
                customView.findViewById<TextView>(R.id.tv_monedas_daily)?.text = "+$monedas"
            } else {
                customView.findViewById<TextView>(R.id.tv_xp_feedback)?.text = "+$xp XP"
                customView.findViewById<TextView>(R.id.tv_monedas_feedback)?.text = "+$monedas"
            }

            val params = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)
            params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            params.bottomMargin = 200
            customView.layoutParams = params

            snackbarLayout.addView(customView)
            currentSnackbar = snackbar
            snackbar.show()
        } else {
            show(context, if(esDaily) "¡Daily! +$xp XP" else "+$xp XP")
        }
    }

    // --- PENALIZACIÓN (Habitos negativos) ---
    fun mostrarPenalizacion(context: Context, hp: Int, xp: Int) {
        val activity = context as? android.app.Activity
        val rootView = activity?.findViewById<View>(android.R.id.content)

        if (rootView != null) {
            currentToast?.cancel()
            currentSnackbar?.dismiss()
            
            val snackbar = Snackbar.make(rootView, "", Snackbar.LENGTH_SHORT)
            val snackbarLayout = snackbar.view as ViewGroup
            snackbarLayout.removeAllViews()
            snackbarLayout.setBackgroundColor(android.graphics.Color.TRANSPARENT)

            val customView = LayoutInflater.from(context).inflate(R.layout.layout_toast_progreso, null)
            val container = customView as? com.google.android.material.card.MaterialCardView
            container?.setCardBackgroundColor(android.graphics.Color.parseColor("#450000"))

            customView.findViewById<TextView>(R.id.tv_xp_feedback)?.apply {
                text = "$xp XP"
                setTextColor(android.graphics.Color.parseColor("#FF6B6B"))
            }
            customView.findViewById<TextView>(R.id.tv_monedas_feedback)?.apply {
                text = "$hp HP"
                setTextColor(android.graphics.Color.parseColor("#FF6B6B"))
            }
            customView.findViewById<ImageView>(R.id.iv_icon_feedback)?.setImageResource(R.drawable.death)
            customView.findViewById<ImageView>(R.id.iv_coins_icon_toast)?.setImageResource(R.drawable.vida)

            val params = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)
            params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            params.bottomMargin = 200
            customView.layoutParams = params

            snackbarLayout.addView(customView)
            currentSnackbar = snackbar
            snackbar.show()
        }
    }

    // --- LOGROS ---
    fun mostrarLogro(context: Context, nombre: String) {
        val activity = context as? android.app.Activity
        val rootView = activity?.findViewById<View>(android.R.id.content) ?: return

        currentSnackbar?.dismiss()
        val snackbar = Snackbar.make(rootView, "", Snackbar.LENGTH_LONG)
        val snackbarLayout = snackbar.view as ViewGroup
        snackbarLayout.removeAllViews()
        snackbarLayout.setBackgroundColor(android.graphics.Color.TRANSPARENT)

        val customView = LayoutInflater.from(context).inflate(R.layout.layout_toast_logro, null)
        customView.findViewById<TextView>(R.id.tv_logro_toast_nombre)?.text = nombre

        val params = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        params.topMargin = 100
        customView.layoutParams = params

        snackbarLayout.addView(customView)
        currentSnackbar = snackbar
        snackbar.show()
    }

    fun success(context: Context, message: String) = show(context, message)
    fun error(context: Context, message: String) = show(context, message)
    fun info(context: Context, message: String) = show(context, message)
}
