package com.example.xpeando.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.xpeando.R
import com.example.xpeando.model.Tarea

class TareasAdapter(
    private var listaTareas: List<Tarea>,
    private val onTareaCompletada: (Tarea, Boolean) -> Unit,
    private val onTareaLongClick: (Tarea) -> Unit
) : RecyclerView.Adapter<TareasAdapter.TareaViewHolder>() {

    class TareaViewHolder(
        itemView: View,
        private val onTareaCompletada: (Tarea, Boolean) -> Unit,
        private val onTareaLongClick: (Tarea) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tv_nombre_tarea)
        val tvDetalles: TextView = itemView.findViewById(R.id.tv_detalles_tarea)
        val cbCompletada: CheckBox = itemView.findViewById(R.id.cb_completada)

        fun bind(tarea: Tarea) {
            tvNombre.text = tarea.nombre
            val dificultadTexto = when(tarea.dificultad) {
                1 -> "Trivial"
                2 -> "Fácil"
                3 -> "Normal"
                4 -> "Difícil"
                else -> "Normal"
            }
            tvDetalles.text = "Dificultad: $dificultadTexto | XP: ${tarea.experiencia} | +${tarea.monedas}"
            
            cbCompletada.setOnCheckedChangeListener(null)
            cbCompletada.isChecked = tarea.completada
            
            // Si la tarea ya está completada, deshabilitamos el CheckBox para evitar errores
            if (tarea.completada) {
                cbCompletada.isEnabled = false
                itemView.alpha = 0.6f // Tarea completada se ve más tenue
            } else {
                cbCompletada.isEnabled = true
                itemView.alpha = 1.0f
                cbCompletada.setOnCheckedChangeListener { _, isChecked ->
                    onTareaCompletada(tarea, isChecked)
                }
            }

            itemView.setOnLongClickListener {
                onTareaLongClick(tarea)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TareaViewHolder {
        val vista = LayoutInflater.from(parent.context).inflate(R.layout.item_tarea, parent, false)
        return TareaViewHolder(vista, onTareaCompletada, onTareaLongClick)
    }

    override fun onBindViewHolder(holder: TareaViewHolder, position: Int) {
        holder.bind(listaTareas[position])
    }

    override fun getItemCount(): Int = listaTareas.size

    fun actualizarLista(nuevaLista: List<Tarea>) {
        listaTareas = nuevaLista
        notifyDataSetChanged()
    }
}
