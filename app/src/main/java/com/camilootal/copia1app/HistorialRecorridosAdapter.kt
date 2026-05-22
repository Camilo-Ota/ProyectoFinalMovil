package com.camilootal.copia1app

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistorialRecorridosAdapter(
    private val listaRecorridos: List<Pair<Recorrido, Int>>
) : RecyclerView.Adapter<HistorialRecorridosAdapter.HistorialViewHolder>() {

    class HistorialViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val tvRutaHistorialItem: TextView =
            itemView.findViewById(R.id.tvRutaHistorialItem)

        val tvEstadoHistorialItem: TextView =
            itemView.findViewById(R.id.tvEstadoHistorialItem)

        val tvInicioHistorialItem: TextView =
            itemView.findViewById(R.id.tvInicioHistorialItem)

        val tvFinHistorialItem: TextView =
            itemView.findViewById(R.id.tvFinHistorialItem)

        val tvTiempoTotalHistorialItem: TextView =
            itemView.findViewById(R.id.tvTiempoTotalHistorialItem)

        val tvCantidadPuntosHistorialItem: TextView =
            itemView.findViewById(R.id.tvCantidadPuntosHistorialItem)

        val btnGestionarRecorrido: Button =
            itemView.findViewById(R.id.btnGestionarRecorrido)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistorialViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_historial_recorrido, parent, false)
        return HistorialViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistorialViewHolder, position: Int) {

        val (recorrido, cantidadPuntos) = listaRecorridos[position]

        holder.tvRutaHistorialItem.text = "Ruta: ${recorrido.rutaNombre}"
        holder.tvEstadoHistorialItem.text = "Estado: ${recorrido.estado}"
        holder.tvInicioHistorialItem.text = "Inicio: ${formatearFechaHora(recorrido.inicioTiempo)}"

        holder.tvFinHistorialItem.text =
            if (recorrido.finTiempo > 0L) "Fin: ${formatearFechaHora(recorrido.finTiempo)}"
            else "Fin: --"

        holder.tvTiempoTotalHistorialItem.text =
            if (recorrido.tiempoTotalMs > 0L) "Tiempo total: ${formatearDuracion(recorrido.tiempoTotalMs)}"
            else "Tiempo total: --"

        holder.tvCantidadPuntosHistorialItem.text = "Puntos registrados: $cantidadPuntos"

        // Color según estado
        when (recorrido.estado) {
            "finalizado_automatico" -> holder.tvEstadoHistorialItem.setTextColor(Color.parseColor("#1B5E20"))
            "finalizado_manual"     -> holder.tvEstadoHistorialItem.setTextColor(Color.parseColor("#E65100"))
            "en_proceso"            -> holder.tvEstadoHistorialItem.setTextColor(Color.parseColor("#0D47A1"))
            else                    -> holder.tvEstadoHistorialItem.setTextColor(Color.parseColor("#424242"))
        }

        // Mostrar botón solo si el recorrido sigue activo
        if (recorrido.estado == "en_proceso") {

            holder.btnGestionarRecorrido.visibility = View.VISIBLE

            holder.btnGestionarRecorrido.setOnClickListener {
                val context = holder.itemView.context

                // ✅ CORRECCIÓN: se pasan todos los datos que RecorridoRutaActivity necesita
                val intent = Intent(context, RecorridoRutaActivity::class.java).apply {
                    putExtra("rutaId",      recorrido.rutaId)       // ID de la ruta en Firebase
                    putExtra("rutaNombre",  recorrido.rutaNombre)   // Nombre visible
                    putExtra("rutaRadio",   30f)                    // valor por defecto, no está en Recorrido
                    putExtra("recorridoId", recorrido.id)           // ← clave para recuperación
                }

                context.startActivity(intent)
            }

        } else {
            holder.btnGestionarRecorrido.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = listaRecorridos.size

    private fun formatearFechaHora(tiempo: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(tiempo))
    }

    private fun formatearDuracion(ms: Long): String {
        val segundos = ms / 1000
        val horas    = segundos / 3600
        val minutos  = (segundos % 3600) / 60
        val seg      = segundos % 60
        return String.format("%02d:%02d:%02d", horas, minutos, seg)
    }

    // ✅ Se eliminó la función falsa "private fun Context.startActivity(p0: Any) {}"
    //    que estaba bloqueando la navegación real.
}