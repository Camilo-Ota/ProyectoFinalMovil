package com.camilootal.copia1app

import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class AsignarBusConductorActivity : AppCompatActivity() {

    private lateinit var db: DatabaseReference
    private lateinit var rvConductores: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvSinConductores: TextView

    private val listaConductores = mutableListOf<User>()
    private val listaBuses       = mutableListOf<Bus>()
    private lateinit var adapter: ConductorBusAdapter

    private var empresaId = ""

    // ── Guardar referencias de listeners ──────────────────────────────────────
    private var busesListener: ValueEventListener? = null
    private var busesQuery: Query? = null
    private var conductoresListener: ValueEventListener? = null
    private var conductoresQuery: Query? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asignar_bus)

        db               = FirebaseDatabase.getInstance().reference
        rvConductores    = findViewById(R.id.rvConductoresBus)
        progressBar      = findViewById(R.id.progressBarAsignar)
        tvSinConductores = findViewById(R.id.tvSinConductoresBus)

        rvConductores.layoutManager = LinearLayoutManager(this)
        adapter = ConductorBusAdapter(
            listaConductores = listaConductores,
            listaBuses       = listaBuses,
            onAsignar        = { mostrarDialogoAsignar(it) },
            onDesasignar     = { desasignarBus(it) }
        )
        rvConductores.adapter = adapter

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.child("users").child(uid).child("empresaId").get()
            .addOnSuccessListener { snap ->
                empresaId = snap.getValue(String::class.java) ?: ""
                cargarDatos()
            }
    }

    private fun cargarDatos() {
        progressBar.visibility = View.VISIBLE

        // Buses en tiempo real
        val qBuses = db.child("buses").orderByChild("empresaId").equalTo(empresaId)
        val lBuses = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                listaBuses.clear()
                for (s in snap.children) {
                    val bus = s.getValue(Bus::class.java) ?: continue
                    bus.id = s.key ?: continue
                    listaBuses.add(bus)
                }
                adapter.notifyDataSetChanged()
            }
            override fun onCancelled(e: DatabaseError) {
                if (FirebaseAuth.getInstance().currentUser != null)
                    Toast.makeText(this@AsignarBusConductorActivity, "Error buses: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        busesQuery    = qBuses
        busesListener = lBuses
        qBuses.addValueEventListener(lBuses)

        // Conductores en tiempo real
        val qConductores = db.child("users").orderByChild("empresaId").equalTo(empresaId)
        val lConductores = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                listaConductores.clear()
                for (s in snap.children) {
                    val user = s.getValue(User::class.java) ?: continue
                    if (user.role == User.ROL_CONDUCTOR) listaConductores.add(user)
                }
                listaConductores.sortBy { it.name }
                adapter.notifyDataSetChanged()
                progressBar.visibility   = View.GONE
                tvSinConductores.visibility = if (listaConductores.isEmpty()) View.VISIBLE else View.GONE
            }
            override fun onCancelled(e: DatabaseError) {
                progressBar.visibility = View.GONE
                if (FirebaseAuth.getInstance().currentUser != null)
                    Toast.makeText(this@AsignarBusConductorActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        conductoresQuery    = qConductores
        conductoresListener = lConductores
        qConductores.addValueEventListener(lConductores)
    }

    private fun desconectarListeners() {
        busesListener?.let       { busesQuery?.removeEventListener(it) }
        conductoresListener?.let { conductoresQuery?.removeEventListener(it) }
        busesListener       = null; busesQuery       = null
        conductoresListener = null; conductoresQuery = null
    }

    override fun onDestroy() {
        super.onDestroy()
        desconectarListeners()
    }

    private fun mostrarDialogoAsignar(conductor: User) {
        val busesDisponibles = listaBuses.filter {
            it.estado != Bus.ESTADO_MANTENIMIENTO &&
            (it.conductorId.isNullOrEmpty() || it.conductorId == conductor.uid)
        }

        if (busesDisponibles.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("⚠️ Sin buses disponibles")
                .setMessage("Todos los buses están asignados o en mantenimiento.\nRevisa el estado de la flota en Gestionar Buses.")
                .setPositiveButton("Entendido", null).show()
            return
        }

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_seleccionar_bus, null)
        val tvTitulo   = dialogView.findViewById<TextView>(R.id.tvTituloDialogBus)
        val rvBuses    = dialogView.findViewById<RecyclerView>(R.id.rvBusesDialogo)

        tvTitulo.text = "Asignar bus a ${conductor.name}"
        rvBuses.layoutManager = LinearLayoutManager(this)

        val dialog = AlertDialog.Builder(this).setView(dialogView).setNegativeButton("Cancelar", null).create()

        rvBuses.adapter = BusSeleccionAdapter(busesDisponibles, conductor.uid) { busElegido ->
            dialog.dismiss()
            realizarAsignacion(conductor, busElegido)
        }
        dialog.show()
    }

    private fun realizarAsignacion(conductor: User, bus: Bus) {
        val conductorUid = conductor.uid ?: return
        progressBar.visibility = View.VISIBLE

        val busAnterior = listaBuses.find { it.conductorId == conductorUid }
        val updates = mutableMapOf<String, Any?>()

        if (busAnterior != null && busAnterior.id != bus.id) {
            updates["buses/${busAnterior.id}/conductorId"]     = null
            updates["buses/${busAnterior.id}/conductorNombre"] = null
        }
        updates["buses/${bus.id}/conductorId"]          = conductorUid
        updates["buses/${bus.id}/conductorNombre"]       = conductor.name ?: ""
        updates["users/${conductorUid}/busAsignado"]     = bus.id
        updates["users/${conductorUid}/busPlaca"]        = bus.placa
        updates["users/${conductorUid}/busModelo"]       = bus.modelo

        db.updateChildren(updates)
            .addOnSuccessListener {
                progressBar.visibility = View.GONE
                AlertDialog.Builder(this)
                    .setTitle("✅ Bus asignado")
                    .setMessage("El bus ${bus.placa} (${bus.modelo}) fue asignado a ${conductor.name} correctamente.")
                    .setPositiveButton("Aceptar", null).show()
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                AlertDialog.Builder(this).setTitle("⚠️ Error").setMessage(e.message ?: "Error desconocido").setPositiveButton("Entendido", null).show()
            }
    }

    private fun desasignarBus(conductor: User) {
        val conductorUid = conductor.uid ?: return
        val busAsignado  = listaBuses.find { it.conductorId == conductorUid }

        if (busAsignado == null) {
            Toast.makeText(this, "Este conductor no tiene bus asignado", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Desasignar bus")
            .setMessage("¿Quitar el bus ${busAsignado.placa} (${busAsignado.modelo}) a ${conductor.name}?")
            .setPositiveButton("Desasignar") { _, _ ->
                progressBar.visibility = View.VISIBLE
                val updates = mapOf<String, Any?>(
                    "buses/${busAsignado.id}/conductorId"     to null,
                    "buses/${busAsignado.id}/conductorNombre" to null,
                    "users/${conductorUid}/busAsignado"       to null,
                    "users/${conductorUid}/busPlaca"          to null,
                    "users/${conductorUid}/busModelo"         to null
                )
                db.updateChildren(updates)
                    .addOnSuccessListener {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this, "✅ Bus desasignado correctamente", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        progressBar.visibility = View.GONE
                        AlertDialog.Builder(this).setTitle("⚠️ Error").setMessage(e.message).setPositiveButton("Entendido", null).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}

class BusSeleccionAdapter(
    private val buses: List<Bus>,
    private val conductorUidActual: String?,
    private val onSeleccionar: (Bus) -> Unit
) : RecyclerView.Adapter<BusSeleccionAdapter.VH>() {

    inner class VH(val view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_bus_seleccion, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val bus     = buses[position]
        val esActual = bus.conductorId == conductorUidActual

        holder.view.findViewById<TextView>(R.id.tvPlacaSeleccion).text  = bus.placa
        holder.view.findViewById<TextView>(R.id.tvModeloSeleccion).text = bus.modelo
        holder.view.findViewById<TextView>(R.id.tvCapSeleccion).text    = "Cap: ${bus.capacidad} pasajeros"
        holder.view.findViewById<TextView>(R.id.tvEstadoSeleccion).text = if (bus.estado == Bus.ESTADO_EN_RUTA) "🟡 En ruta" else "🟢 Disponible"

        val badge = holder.view.findViewById<TextView>(R.id.tvBadgeActual)
        val card  = holder.view.findViewById<CardView>(R.id.cardBusSeleccion)
        badge.visibility = if (esActual) View.VISIBLE else View.GONE
        card.setCardBackgroundColor(if (esActual) Color.parseColor("#E3F2FD") else Color.WHITE)

        holder.view.setOnClickListener { onSeleccionar(bus) }
    }

    override fun getItemCount() = buses.size
}
