package com.example.xpeando.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.xpeando.R
import com.example.xpeando.model.Nota
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotasAdapter(
    private var notas: List<Nota>,
    private val onNotaClick: (Nota) -> Unit,
    private val onNotaLongClick: (Nota) -> Unit
) : RecyclerView.Adapter<NotasAdapter.NotaViewHolder>() {

    class NotaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitulo: TextView = view.findViewById(R.id.tv_titulo_nota)
        val tvContenido: TextView = view.findViewById(R.id.tv_contenido_nota)
        val tvFecha: TextView = view.findViewById(R.id.tv_fecha_nota)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_nota, parent, false)
        return NotaViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotaViewHolder, position: Int) {
        val nota = notas[position]
        holder.tvTitulo.text = nota.titulo
        holder.tvContenido.text = nota.contenido
        
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        holder.tvFecha.text = sdf.format(Date(nota.fecha))

        holder.itemView.setOnClickListener { onNotaClick(nota) }
        holder.itemView.setOnLongClickListener {
            onNotaLongClick(nota)
            true
        }
    }

    override fun getItemCount(): Int = notas.size

    fun actualizarLista(nuevaLista: List<Nota>) {
        notas = nuevaLista
        notifyDataSetChanged()
    }
}