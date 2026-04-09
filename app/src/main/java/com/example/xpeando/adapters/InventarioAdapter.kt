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
        val tvCantidad: TextView = view.findViewById(R.id.tv_articulo_cantidad)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_inventario, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val art = lista[position]
        holder.tvNombre.text = art.nombre
        
        // Lógica para mostrar múltiples bonus
        val bonuses = mutableListOf<String>()
        if (art.bonusFza > 0) bonuses.add("+${art.bonusFza} FZA")
        if (art.bonusInt > 0) bonuses.add("+${art.bonusInt} INT")
        if (art.bonusCon > 0) bonuses.add("+${art.bonusCon} CON")
        if (art.bonusPer > 0) bonuses.add("+${art.bonusPer} PER")
        if (art.bonusHp > 0) bonuses.add("+${art.bonusHp} HP")
        
        holder.tvBonus.text = if (bonuses.isEmpty()) "Sin atributos" else bonuses.joinToString(" ")
        
        // MOSTRAR PRECIO
        val tvPrecio = holder.itemView.findViewById<TextView>(R.id.tv_articulo_precio)
        tvPrecio.text = "${art.precio}"

        // Carga dinámica de iconos
        val context = holder.itemView.context
        val resId = context.resources.getIdentifier(art.icono, "drawable", context.packageName)
        if (resId != 0) {
            holder.ivIcono.setImageResource(resId)
        } else {
            holder.ivIcono.setImageResource(R.drawable.ic_recompensas) // Fallback
        }
        
        // Lógica de compra vs equipar/usar
        if (art.esPropio) {
            tvPrecio.visibility = View.GONE
            if (art.tipo == "CONSUMIBLE") {
                holder.btnEquipar.text = "Usar"
                holder.indicator.visibility = View.GONE
            } else {
                holder.btnEquipar.text = if (art.equipado) "Quitar" else "Equipar"
                holder.indicator.visibility = if (art.equipado) View.VISIBLE else View.GONE
            }
        } else {
            tvPrecio.visibility = View.VISIBLE
            holder.btnEquipar.text = "Comprar"
            holder.indicator.visibility = View.GONE
        }
        
        holder.btnEquipar.setOnClickListener { onEquiparClick(art) }

        // Mostrar cantidad si es mayor a 1
        if (art.cantidad > 1) {
            holder.tvCantidad.visibility = View.VISIBLE
            holder.tvCantidad.text = "x${art.cantidad}"
        } else {
            holder.tvCantidad.visibility = View.GONE
        }
    }

    override fun getItemCount() = lista.size

    fun actualizarLista(nuevaLista: List<Articulo>) {
        lista = nuevaLista
        notifyDataSetChanged()
    }
}
