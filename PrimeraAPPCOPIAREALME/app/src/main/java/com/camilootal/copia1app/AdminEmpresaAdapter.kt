package com.camilootal.copia1app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AdminEmpresaAdapter(
    private val lista: MutableList<User>,
    private val onEditar: (User) -> Unit,
    private val onEliminar: (User) -> Unit
) : RecyclerView.Adapter<AdminEmpresaAdapter.AdminVH>() {

    inner class AdminVH(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre    : TextView = view.findViewById(R.id.tvNombreConductorItem)
        val tvEmail     : TextView = view.findViewById(R.id.tvEmailConductorItem)
        val tvRol       : TextView = view.findViewById(R.id.tvPlacaConductorItem)
        val tvPhone     : TextView = view.findViewById(R.id.tvPhoneConductorItem)
        val btnEditar   : Button   = view.findViewById(R.id.btnEditarConductor)
        val btnEliminar : Button   = view.findViewById(R.id.btnEliminarConductor)
        val btnHistorial: Button   = view.findViewById(R.id.btnVerHistorialConductor)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminVH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conductor_admin, parent, false)
        return AdminVH(v)
    }

    override fun onBindViewHolder(holder: AdminVH, position: Int) {
        val admin = lista[position]
        holder.tvNombre.text = admin.name  ?: "Sin nombre"
        holder.tvEmail.text  = admin.email ?: "Sin email"
        holder.tvRol.text    = "Rol: Administrador"
        holder.tvPhone.text  = "Tel: ${admin.phone ?: "N/A"}"

        holder.btnHistorial.visibility = View.GONE  // no aplica para admins

        holder.btnEditar.setOnClickListener   { onEditar(admin) }
        holder.btnEliminar.setOnClickListener { onEliminar(admin) }
    }

    override fun getItemCount() = lista.size
}