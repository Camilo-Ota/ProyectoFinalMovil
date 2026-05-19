package com.camilootal.copia1app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.camilootal.copia1app.Ruta

class RutaAdapter(
    private val listaRutas: List<Ruta>,
    private val onConfigurarClick: (Ruta) -> Unit
) : RecyclerView.Adapter<RutaAdapter.RutaViewHolder>() {

    class RutaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombreRutaItem: TextView = itemView.findViewById(R.id.tvNombreRutaItem)
        val tvDescripcionRutaItem: TextView = itemView.findViewById(R.id.tvDescripcionRutaItem)
        val tvRadioRutaItem: TextView = itemView.findViewById(R.id.tvRadioRutaItem)
        val btnConfigurarPuntos: Button = itemView.findViewById(R.id.btnConfigurarPuntos)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RutaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ruta, parent, false)
        return RutaViewHolder(view)
    }

    override fun onBindViewHolder(holder: RutaViewHolder, position: Int) {
        val ruta = listaRutas[position]

        holder.tvNombreRutaItem.text = ruta.nombre
        holder.tvDescripcionRutaItem.text = ruta.descripcion
        holder.tvRadioRutaItem.text = "Radio de detección: ${ruta.radioDeteccion} m"

        holder.btnConfigurarPuntos.setOnClickListener {
            onConfigurarClick(ruta)
        }
    }

    override fun getItemCount(): Int = listaRutas.size
}