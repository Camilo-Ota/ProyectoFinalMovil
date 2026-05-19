package com.camilootal.copia1app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Adapter para mostrar las publicaciones del canal de conductores en el RecyclerView
class PublicacionAdapter(
    private val lista: List<Publicacion>
) : RecyclerView.Adapter<PublicacionAdapter.PublicacionViewHolder>() {

    class PublicacionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvAvatar: TextView = itemView.findViewById(R.id.tvAvatarPublicacion)
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombreAutor)
        val tvTiempo: TextView = itemView.findViewById(R.id.tvTiempoPublicacion)
        val tvCategoria: TextView = itemView.findViewById(R.id.tvCategoriaPublicacion)
        val tvTexto: TextView = itemView.findViewById(R.id.tvTextoPublicacion)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PublicacionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_publicacion, parent, false)
        return PublicacionViewHolder(view)
    }

    override fun onBindViewHolder(holder: PublicacionViewHolder, position: Int) {
        val pub = lista[position]

        // Avatar: primeras 2 letras del nombre en mayúsculas
        holder.tvAvatar.text = pub.autorNombre
            .trim()
            .split(" ")
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifEmpty { "?" }

        holder.tvNombre.text = pub.autorNombre
        holder.tvTexto.text = pub.texto
        holder.tvCategoria.text = pub.categoria
        holder.tvTiempo.text = formatearTiempoRelativo(pub.timestamp)
    }

    override fun getItemCount(): Int = lista.size

    // Convierte el timestamp a texto relativo ("hace 5 min", "hace 2h", etc.)
    private fun formatearTiempoRelativo(timestamp: Long): String {
        val ahora = System.currentTimeMillis()
        val diff = ahora - timestamp
        return when {
            diff < 60_000 -> "hace un momento"
            diff < 3_600_000 -> "hace ${diff / 60_000} min"
            diff < 86_400_000 -> "hace ${diff / 3_600_000}h"
            else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }
}