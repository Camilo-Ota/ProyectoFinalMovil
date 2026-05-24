package com.camilootal.copia1app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BusAdapter(
    private val lista: MutableList<Bus>,
    private val onEditar: (Bus) -> Unit,
    private val onEliminar: (Bus) -> Unit
) : RecyclerView.Adapter<BusAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvPlaca:      TextView = view.findViewById(R.id.tvPlacaBusItem)
        val tvModelo:     TextView = view.findViewById(R.id.tvModeloBusItem)
        val tvCapacidad:  TextView = view.findViewById(R.id.tvCapacidadBusItem)
        val tvEstado:     TextView = view.findViewById(R.id.tvEstadoBusItem)
        val tvConductor:  TextView = view.findViewById(R.id.tvConductorBusItem)
        val btnEstado:    Button   = view.findViewById(R.id.btnEditarEstadoBus)
        val btnEliminar:  Button   = view.findViewById(R.id.btnEliminarBus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bus, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bus = lista[position]

        holder.tvPlaca.text     = bus.placa
        holder.tvModelo.text    = bus.modelo
        holder.tvCapacidad.text = "Capacidad: ${bus.capacidad} pasajeros"

        // Estado con color
        val (estadoTexto, estadoColor) = when (bus.estado) {
            Bus.ESTADO_EN_RUTA      -> Pair("🟡 En ruta",       "#F57F17")
            Bus.ESTADO_MANTENIMIENTO-> Pair("🔴 Mantenimiento", "#C62828")
            else                    -> Pair("🟢 Disponible",    "#2E7D32")
        }
        holder.tvEstado.text     = estadoTexto
        holder.tvEstado.setTextColor(android.graphics.Color.parseColor(estadoColor))

        holder.tvConductor.text = if (!bus.conductorNombre.isNullOrEmpty())
            "Conductor: ${bus.conductorNombre}"
        else
            "Sin conductor asignado"

        holder.btnEstado.setOnClickListener  { onEditar(bus) }
        holder.btnEliminar.setOnClickListener { onEliminar(bus) }
    }

    override fun getItemCount() = lista.size
}
