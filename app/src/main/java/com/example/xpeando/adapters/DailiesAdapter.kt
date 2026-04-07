package com.example.xpeando.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.xpeando.R
import com.example.xpeando.model.Daily

class DailiesAdapter(
    private var lista: List<Daily>,
    private val onCheckedChange: (Daily, Boolean) -> Unit,
    private val onLongClick: (Daily) -> Unit
) : RecyclerView.Adapter<DailiesAdapter.DailyViewHolder>() {

    class DailyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cbDaily: CheckBox = view.findViewById(R.id.cb_daily)
        val tvNombre: TextView = view.findViewById(R.id.tv_nombre_daily)
        val tvRecompensa: TextView = view.findViewById(R.id.tv_recompensa_daily)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DailyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_daily, parent, false)
        return DailyViewHolder(view)
    }

    override fun onBindViewHolder(holder: DailyViewHolder, position: Int) {
        val daily = lista[position]
        holder.tvNombre.text = daily.nombre
        holder.tvRecompensa.text = "+${daily.experiencia} XP | +${daily.monedas} Monedas"
        
        // Evitar disparar el listener al configurar el estado inicial
        holder.cbDaily.setOnCheckedChangeListener(null)
        holder.cbDaily.isChecked = daily.completadaHoy
        
        holder.cbDaily.setOnCheckedChangeListener { _, isChecked ->
            onCheckedChange(daily, isChecked)
        }

        holder.itemView.setOnLongClickListener {
            onLongClick(daily)
            true
        }
    }

    override fun getItemCount() = lista.size

    fun actualizarLista(nuevaLista: List<Daily>) {
        lista = nuevaLista
        notifyDataSetChanged()
    }
}
