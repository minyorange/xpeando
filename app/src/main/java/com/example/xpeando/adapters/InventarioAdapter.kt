package com.example.xpeando.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.xpeando.R
import com.example.xpeando.model.Articulo

class InventarioAdapter(
    private var lista: List<Articulo>,
    private val onEquiparClick: (Articulo) -> Unit
) : RecyclerView.Adapter<InventarioAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcono: ImageView = view.findViewById(R.id.iv_articulo_icono)
        val tvNombre: TextView = view.findViewById(R.id.tv_articulo_nombre)
        val tvBonus: TextView = view.findViewById(R.id.tv_articulo_bonus)
        val btnEquipar: Button = view.findViewById(R.id.btn_equipar)
        val indicator: View = view.findViewById(R.id.view_equipado_indicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_inventario, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val art = lista[position]
        holder.tvNombre.text = art.nombre
        
        val bonusText = when {
            art.bonusFza > 0 -> "+${art.bonusFza} FZA"
            art.bonusInt > 0 -> "+${art.bonusInt} INT"
            art.bonusCon > 0 -> "+${art.bonusCon} CON"
            art.bonusPer > 0 -> "+${art.bonusPer} PER"
            art.bonusHp > 0 -> "+${art.bonusHp} HP"
            else -> ""
        }
        holder.tvBonus.text = bonusText

        // Volvemos a usar el icono por defecto local
        holder.ivIcono.setImageResource(R.drawable.ic_recompensas)
        
        holder.indicator.visibility = if (art.equipado) View.VISIBLE else View.GONE
        holder.btnEquipar.text = if (art.equipado) "Quitar" else "Equipar"
        
        holder.btnEquipar.setOnClickListener { onEquiparClick(art) }
    }

    override fun getItemCount() = lista.size

    fun actualizarLista(nuevaLista: List<Articulo>) {
        lista = nuevaLista
        notifyDataSetChanged()
    }
}
