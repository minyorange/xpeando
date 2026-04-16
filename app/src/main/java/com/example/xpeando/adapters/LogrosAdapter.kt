package com.example.xpeando.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.xpeando.R
import com.example.xpeando.model.Logro

class LogrosAdapter(private var logros: List<Logro>) :
    RecyclerView.Adapter<LogrosAdapter.LogroViewHolder>() {

    class LogroViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcono: ImageView = view.findViewById(R.id.iv_logro_icono)
        val tvNombre: TextView = view.findViewById(R.id.tv_logro_nombre)
        val tvProgreso: TextView = view.findViewById(R.id.tv_logro_progreso)
        val pbLogro: ProgressBar = view.findViewById(R.id.pb_logro)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogroViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_logro, parent, false)
        return LogroViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogroViewHolder, position: Int) {
        val logro = logros[position]
        holder.tvNombre.text = logro.nombre
        
        val progresoVisual = if (logro.progresoActual > logro.requisito) logro.requisito else logro.progresoActual
        holder.tvProgreso.text = "$progresoVisual/${logro.requisito}"
        
        holder.pbLogro.max = logro.requisito
        holder.pbLogro.progress = progresoVisual
        
        // Carga segura de iconos (mejor si migramos a String pronto)
        holder.ivIcono.setImageResource(logro.iconoResId)

        if (logro.completado) {
            holder.ivIcono.alpha = 1.0f
            holder.tvNombre.setTextColor(holder.itemView.context.getColor(R.color.xpeando_purple_primary))
        } else {
            holder.ivIcono.alpha = 0.3f
        }
    }

    override fun getItemCount() = logros.size

    fun actualizarLista(nuevaLista: List<Logro>) {
        logros = nuevaLista
        notifyDataSetChanged()
    }
}
