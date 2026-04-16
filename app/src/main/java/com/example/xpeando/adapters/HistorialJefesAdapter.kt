package com.example.xpeando.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.xpeando.R
import com.example.xpeando.model.Jefe

class HistorialJefesAdapter(private var jefes: List<Jefe>) :
    RecyclerView.Adapter<HistorialJefesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcono: ImageView = view.findViewById(R.id.ivHistorialIcono)
        val tvNombre: TextView = view.findViewById(R.id.tvHistorialNombre)
        val tvNivel: TextView = view.findViewById(R.id.tvHistorialNivel)
        val tvRecompensas: TextView = view.findViewById(R.id.tvHistorialRecompensas)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_historial_jefe, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val jefe = jefes[position]
        holder.tvNombre.text = jefe.nombre
        holder.tvNivel.text = "Nivel ${jefe.nivel}"
        holder.tvRecompensas.text = "+${jefe.recompensaMonedas} | +${jefe.recompensaXP} XP"
        
        val context = holder.itemView.context
        val resId = context.resources.getIdentifier(jefe.icono, "drawable", context.packageName)
        if (resId != 0) {
            holder.ivIcono.setImageResource(resId)
        } else {
            holder.ivIcono.setImageResource(R.drawable.ic_boss_dragon)
        }
    }

    override fun getItemCount() = jefes.size

    fun actualizarLista(nuevaLista: List<Jefe>) {
        jefes = nuevaLista
        notifyDataSetChanged()
    }
}