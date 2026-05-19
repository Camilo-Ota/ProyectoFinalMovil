package com.camilootal.copia1app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ConductorAdminAdapter(
    private val lista: List<User>,
    private val onEditar: (User) -> Unit,
    private val onEliminar: (User) -> Unit
) : RecyclerView.Adapter<ConductorAdminAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView  = view.findViewById(R.id.tvNombreConductorItem)
        val tvEmail: TextView   = view.findViewById(R.id.tvEmailConductorItem)
        val tvPlaca: TextView   = view.findViewById(R.id.tvPlacaConductorItem)
        val tvPhone: TextView   = view.findViewById(R.id.tvPhoneConductorItem)
        val btnEditar: Button   = view.findViewById(R.id.btnEditarConductor)
        val btnEliminar: Button = view.findViewById(R.id.btnEliminarConductor)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conductor_admin, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val conductor = lista[position]
        holder.tvNombre.text  = conductor.name  ?: "Sin nombre"
        holder.tvEmail.text   = conductor.email ?: "Sin email"
        holder.tvPlaca.text   = "Placa: ${conductor.placa ?: "N/A"}"
        holder.tvPhone.text   = "Tel: ${conductor.phone ?: "N/A"}"
        holder.btnEditar.setOnClickListener   { onEditar(conductor) }
        holder.btnEliminar.setOnClickListener { onEliminar(conductor) }
    }

    override fun getItemCount() = lista.size
}