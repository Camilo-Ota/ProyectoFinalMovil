package com.camilootal.copia1app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.camilootal.copia1app.Ruta

class RutaRecorridoAdapter(
    private val listaRutas: List<Ruta>,
    private val onIniciarClick: (Ruta) -> Unit
) : RecyclerView.Adapter<RutaRecorridoAdapter.RutaRecorridoViewHolder>() {

    class RutaRecorridoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombreRutaRecorridoItem: TextView = itemView.findViewById(R.id.tvNombreRutaRecorridoItem)
        val tvDescripcionRutaRecorridoItem: TextView = itemView.findViewById(R.id.tvDescripcionRutaRecorridoItem)
        val tvRadioRutaRecorridoItem: TextView = itemView.findViewById(R.id.tvRadioRutaRecorridoItem)
        val btnIniciarRecorridoRuta: Button = itemView.findViewById(R.id.btnIniciarRecorridoRuta)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RutaRecorridoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ruta_recorrido, parent, false)
        return RutaRecorridoViewHolder(view)
    }

    override fun onBindViewHolder(holder: RutaRecorridoViewHolder, position: Int) {
        val ruta = listaRutas[position]

        holder.tvNombreRutaRecorridoItem.text = ruta.nombre
        holder.tvDescripcionRutaRecorridoItem.text = ruta.descripcion
        holder.tvRadioRutaRecorridoItem.text = "Radio de detección: ${ruta.radioDeteccion} m"

        holder.btnIniciarRecorridoRuta.setOnClickListener {
            onIniciarClick(ruta)
        }
    }

    override fun getItemCount(): Int = listaRutas.size
}