package com.camilootal.copia1app

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class HistorialRecorridosAdapter(
    private val listaRecorridos: List<Pair<Recorrido, Int>>,
    private val onItemClick: (Recorrido) -> Unit       // ✅ callback al hacer click
) : RecyclerView.Adapter<HistorialRecorridosAdapter.HistorialViewHolder>() {

    class HistorialViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvRutaHistorialItem: TextView           = itemView.findViewById(R.id.tvRutaHistorialItem)
        val tvEstadoHistorialItem: TextView         = itemView.findViewById(R.id.tvEstadoHistorialItem)
        val tvInicioHistorialItem: TextView         = itemView.findViewById(R.id.tvInicioHistorialItem)
        val tvFinHistorialItem: TextView            = itemView.findViewById(R.id.tvFinHistorialItem)
        val tvTiempoTotalHistorialItem: TextView    = itemView.findViewById(R.id.tvTiempoTotalHistorialItem)
        val tvCantidadPuntosHistorialItem: TextView = itemView.findViewById(R.id.tvCantidadPuntosHistorialItem)
        val tvVerDetalle: TextView                  = itemView.findViewById(R.id.tvVerDetalle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistorialViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_historial_recorrido, parent, false)
        return HistorialViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistorialViewHolder, position: Int) {
        val (recorrido, cantidadPuntos) = listaRecorridos[position]

        holder.tvRutaHistorialItem.text           = "Ruta: ${recorrido.rutaNombre}"
        holder.tvInicioHistorialItem.text         = "Inicio: ${formatearFechaHora(recorrido.inicioTiempo)}"
        holder.tvCantidadPuntosHistorialItem.text = "Puntos recorridos: $cantidadPuntos"

        holder.tvFinHistorialItem.text =
            if (recorrido.finTiempo > 0L) "Fin: ${formatearFechaHora(recorrido.finTiempo)}"
            else "Fin: En curso"

        holder.tvTiempoTotalHistorialItem.text =
            if (recorrido.tiempoTotalMs > 0L) "Duración: ${formatearDuracion(recorrido.tiempoTotalMs)}"
            else if (recorrido.estado == "en_proceso") "Duración: En curso"
            else "Duración: --"

        // Estado con color y etiqueta legible
        val (textoEstado, colorEstado) = when (recorrido.estado) {
            "finalizado_automatico" -> "✅ Finalizado automático" to Color.parseColor("#1B5E20")
            "finalizado_manual"     -> "🔶 Finalizado manual"     to Color.parseColor("#E65100")
            "en_proceso"            -> "🔵 En proceso"            to Color.parseColor("#0D47A1")
            else                    -> recorrido.estado            to Color.parseColor("#424242")
        }
        holder.tvEstadoHistorialItem.text = textoEstado
        holder.tvEstadoHistorialItem.setTextColor(colorEstado)

        // ✅ Texto del botón de detalle según estado
        holder.tvVerDetalle.text = if (recorrido.estado == "en_proceso")
            "Ver detalle / Continuar o Finalizar →"
        else
            "Ver detalle en mapa →"

        // Click en toda la card
        holder.itemView.setOnClickListener { onItemClick(recorrido) }
        holder.tvVerDetalle.setOnClickListener { onItemClick(recorrido) }
    }

    override fun getItemCount(): Int = listaRecorridos.size

    private fun formatearFechaHora(tiempo: Long): String =
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(tiempo))

    private fun formatearDuracion(ms: Long): String {
        val s = ms / 1000
        return String.format("%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
    }
}