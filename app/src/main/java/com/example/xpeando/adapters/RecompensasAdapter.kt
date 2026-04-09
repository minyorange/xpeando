package com.example.xpeando.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.xpeando.R
import com.example.xpeando.model.Recompensa

class RecompensasAdapter(
    private var listaRecompensas: List<Recompensa>,
    private val onComprarClick: (Recompensa) -> Unit,
    private val onLongClick: (Recompensa) -> Unit
) : RecyclerView.Adapter<RecompensasAdapter.RecompensaViewHolder>() {

    class RecompensaViewHolder(
        itemView: View,
        private val onComprarClick: (Recompensa) -> Unit,
        private val onLongClick: (Recompensa) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tv_nombre_recompensa)
        val btnComprar: Button = itemView.findViewById(R.id.btn_comprar_recompensa)
        val ivIcono: ImageView = itemView.findViewById(R.id.iv_recompensa)

        fun bind(recompensa: Recompensa) {
            tvNombre.text = recompensa.nombre
            btnComprar.text = recompensa.precio.toString()

            // Carga dinámica de iconos
            val context = itemView.context
            val resId = context.resources.getIdentifier(recompensa.icono, "drawable", context.packageName)
            if (resId != 0) {
                ivIcono.setImageResource(resId)
            } else {
                ivIcono.setImageResource(R.drawable.ic_recompensas)
            }

            btnComprar.setOnClickListener {
                onComprarClick(recompensa)
            }

            itemView.setOnLongClickListener {
                onLongClick(recompensa)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecompensaViewHolder {
        val vista = LayoutInflater.from(parent.context).inflate(R.layout.item_recompensa, parent, false)
        return RecompensaViewHolder(vista, onComprarClick, onLongClick)
    }

    override fun onBindViewHolder(holder: RecompensaViewHolder, position: Int) {
        holder.bind(listaRecompensas[position])
    }

    override fun getItemCount(): Int = listaRecompensas.size

    fun actualizarLista(nuevaLista: List<Recompensa>) {
        listaRecompensas = nuevaLista
        notifyDataSetChanged()
    }
}
