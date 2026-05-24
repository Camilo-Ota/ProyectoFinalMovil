package com.camilootal.copia1app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ConductorBusAdapter(
    private val listaConductores: List<User>,
    private val listaBuses: List<Bus>,
    private val onAsignar: (User) -> Unit,
    private val onDesasignar: (User) -> Unit
) : RecyclerView.Adapter<ConductorBusAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre:     TextView = view.findViewById(R.id.tvNombreConductorBus)
        val tvBusActual:  TextView = view.findViewById(R.id.tvBusActualConductor)
        val tvPlaca:      TextView = view.findViewById(R.id.tvPlacaConductorBus)
        val btnAsignar:   Button   = view.findViewById(R.id.btnAsignarBus)
        val btnDesasignar:Button   = view.findViewById(R.id.btnDesasignarBus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conductor_bus, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val conductor = listaConductores[position]
        holder.tvNombre.text = conductor.name ?: "Sin nombre"

        // Buscar el bus asignado a este conductor en la lista de buses
        val busAsignado = listaBuses.find { it.conductorId == conductor.uid }

        if (busAsignado != null) {
            holder.tvBusActual.text  = "${busAsignado.placa} — ${busAsignado.modelo}"
            holder.tvPlaca.text      = "Cap: ${busAsignado.capacidad} · Estado: ${busAsignado.estado}"
            holder.tvBusActual.setTextColor(android.graphics.Color.parseColor("#1B5E20"))
            holder.btnAsignar.text   = "Cambiar bus"
            holder.btnDesasignar.visibility = View.VISIBLE
        } else {
            holder.tvBusActual.text  = "Sin bus asignado"
            holder.tvPlaca.text      = ""
            holder.tvBusActual.setTextColor(android.graphics.Color.parseColor("#B71C1C"))
            holder.btnAsignar.text   = "Asignar bus"
            holder.btnDesasignar.visibility = View.GONE
        }

        holder.btnAsignar.setOnClickListener    { onAsignar(conductor) }
        holder.btnDesasignar.setOnClickListener { onDesasignar(conductor) }
    }

    override fun getItemCount() = listaConductores.size
}
