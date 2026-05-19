package com.camilootal.copia1app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.camilootal.copia1app.PuntoRuta


class PuntoRutaAdapter(
    private val listaPuntos: List<PuntoRuta>,
    private val onEditarClick: (PuntoRuta) -> Unit,
    private val onEliminarClick: (PuntoRuta) -> Unit
) : RecyclerView.Adapter<PuntoRutaAdapter.PuntoViewHolder>() {

    class PuntoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombrePuntoItem: TextView = itemView.findViewById(R.id.tvNombrePuntoItem)
        val tvTipoPuntoItem: TextView = itemView.findViewById(R.id.tvTipoPuntoItem)
        val tvCoordenadasPuntoItem: TextView = itemView.findViewById(R.id.tvCoordenadasPuntoItem)
        val btnEditarPunto: Button = itemView.findViewById(R.id.btnEditarPunto)
        val btnEliminarPunto: Button = itemView.findViewById(R.id.btnEliminarPunto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PuntoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_punto_ruta, parent, false)
        return PuntoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PuntoViewHolder, position: Int) {
        val punto = listaPuntos[position]

        holder.tvNombrePuntoItem.text = "${punto.orden}. ${punto.nombre}"
        holder.tvTipoPuntoItem.text = "Tipo: ${punto.tipo}"
        holder.tvCoordenadasPuntoItem.text = "Lat: ${punto.latitud} | Lng: ${punto.longitud}"

        holder.btnEditarPunto.setOnClickListener {
            onEditarClick(punto)
        }

        holder.btnEliminarPunto.setOnClickListener {
            onEliminarClick(punto)
        }
    }

    override fun getItemCount(): Int = listaPuntos.size
}