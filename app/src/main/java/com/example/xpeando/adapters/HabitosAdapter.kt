package com.example.xpeando.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.xpeando.R
import com.example.xpeando.model.Habito

class HabitosAdapter(
    private var listaHabitos: List<Habito>,
    private val onAccionHabito: (Habito, Int) -> Unit, // Int será 1 para sumar, -1 para restar
    private val onLongClick: (Habito) -> Unit
) : RecyclerView.Adapter<HabitosAdapter.HabitoViewHolder>() {

    class HabitoViewHolder(
        itemView: View,
        private val onAccionHabito: (Habito, Int) -> Unit,
        private val onLongClick: (Habito) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tv_nombre_habito)
        val tvXP: TextView = itemView.findViewById(R.id.tv_experiencia_habito)
        val btnSumar: View = itemView.findViewById(R.id.btn_sumar_habito)
        val btnRestar: View = itemView.findViewById(R.id.btn_restar_habito)

        fun bind(habito: Habito) {
            tvNombre.text = habito.nombre
            tvXP.text = "XP: ${habito.experiencia} | +${habito.monedas}"

            btnSumar.setOnClickListener {
                onAccionHabito(habito, 1)
            }

            btnRestar.setOnClickListener {
                onAccionHabito(habito, -1)
            }

            itemView.setOnLongClickListener {
                onLongClick(habito)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitoViewHolder {
        val vista = LayoutInflater.from(parent.context).inflate(R.layout.item_habito, parent, false)
        return HabitoViewHolder(vista, onAccionHabito, onLongClick)
    }

    override fun onBindViewHolder(holder: HabitoViewHolder, position: Int) {
        holder.bind(listaHabitos[position])
    }

    override fun getItemCount(): Int = listaHabitos.size

    fun actualizarLista(nuevaLista: List<Habito>) {
        listaHabitos = nuevaLista
        notifyDataSetChanged()
    }
}
