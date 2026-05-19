package com.camilootal.copia1app

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.camilootal.copia1app.PuntoSeguimientoUI

class PuntoSeguimientoAdapter(
    private val listaPuntos: List<PuntoSeguimientoUI>
) : RecyclerView.Adapter<PuntoSeguimientoAdapter.PuntoSeguimientoViewHolder>() {

    class PuntoSeguimientoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val layoutItemPuntoSeguimiento: LinearLayout =
            itemView.findViewById(R.id.layoutItemPuntoSeguimiento)
        val tvNombrePuntoSeguimiento: TextView =
            itemView.findViewById(R.id.tvNombrePuntoSeguimiento)
        val tvTipoPuntoSeguimiento: TextView =
            itemView.findViewById(R.id.tvTipoPuntoSeguimiento)
        val tvEstadoPuntoSeguimiento: TextView =
            itemView.findViewById(R.id.tvEstadoPuntoSeguimiento)
        val tvTiempoDesdeAnteriorSeguimiento: TextView =
            itemView.findViewById(R.id.tvTiempoDesdeAnteriorSeguimiento)
        val tvTiempoAcumuladoSeguimiento: TextView =
            itemView.findViewById(R.id.tvTiempoAcumuladoSeguimiento)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PuntoSeguimientoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_punto_seguimiento, parent, false)
        return PuntoSeguimientoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PuntoSeguimientoViewHolder, position: Int) {
        val punto = listaPuntos[position]

        holder.tvNombrePuntoSeguimiento.text = "${punto.orden}. ${punto.nombre}"
        holder.tvTipoPuntoSeguimiento.text = "Tipo: ${punto.tipo}"

        when {
            punto.completado -> {
                holder.tvEstadoPuntoSeguimiento.text = "Estado: completado"
                holder.tvEstadoPuntoSeguimiento.setTextColor(Color.parseColor("#1B5E20"))
                holder.layoutItemPuntoSeguimiento.setBackgroundColor(Color.parseColor("#C8E6C9"))
            }

            punto.esSiguiente -> {
                holder.tvEstadoPuntoSeguimiento.text = "Estado: siguiente punto"
                holder.tvEstadoPuntoSeguimiento.setTextColor(Color.parseColor("#0D47A1"))
                holder.layoutItemPuntoSeguimiento.setBackgroundColor(Color.parseColor("#BBDEFB"))
            }

            else -> {
                holder.tvEstadoPuntoSeguimiento.text = "Estado: pendiente"
                holder.tvEstadoPuntoSeguimiento.setTextColor(Color.parseColor("#616161"))
                holder.layoutItemPuntoSeguimiento.setBackgroundColor(Color.parseColor("#F5F5F5"))
            }
        }

        holder.tvTiempoDesdeAnteriorSeguimiento.text =
            if (punto.completado) {
                "Tiempo desde anterior: ${formatearDuracion(punto.tiempoDesdeAnteriorMs)}"
            } else {
                "Tiempo desde anterior: --"
            }

        holder.tvTiempoAcumuladoSeguimiento.text =
            if (punto.completado) {
                "Tiempo acumulado: ${formatearDuracion(punto.tiempoAcumuladoRutaMs)}"
            } else {
                "Tiempo acumulado: --"
            }
    }

    override fun getItemCount(): Int = listaPuntos.size

    private fun formatearDuracion(ms: Long): String {
        val segundos = ms / 1000
        val horas = segundos / 3600
        val minutos = (segundos % 3600) / 60
        val seg = segundos % 60
        return String.format("%02d:%02d:%02d", horas, minutos, seg)
    }
}