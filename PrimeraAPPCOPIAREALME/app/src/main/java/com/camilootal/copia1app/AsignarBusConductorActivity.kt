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

    // ── Carga en tiempo real de buses Y conductores ───────────────────────────

    private fun cargarDatos() {
        progressBar.visibility = View.VISIBLE

        // Buses: escuchar en tiempo real para reflejar cambios de asignación
        db.child("buses")
            .orderByChild("empresaId")
            .equalTo(empresaId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snap: DataSnapshot) {
                    listaBuses.clear()
                    for (s in snap.children) {
                        val bus = s.getValue(Bus::class.java) ?: continue
                        bus.id = s.key ?: continue
                        listaBuses.add(bus)
                    }
                    adapter.notifyDataSetChanged()
                }
                override fun onCancelled(e: DatabaseError) {}
            })

        // Conductores: también en tiempo real
        db.child("users")
            .orderByChild("empresaId")
            .equalTo(empresaId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snap: DataSnapshot) {
                    listaConductores.clear()
                    for (s in snap.children) {
                        val user = s.getValue(User::class.java) ?: continue
                        if (user.role == User.ROL_CONDUCTOR) listaConductores.add(user)
                    }
                    listaConductores.sortBy { it.name }
                    adapter.notifyDataSetChanged()
                    progressBar.visibility   = View.GONE
                    tvSinConductores.visibility =
                        if (listaConductores.isEmpty()) View.VISIBLE else View.GONE
                }
                override fun onCancelled(e: DatabaseError) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@AsignarBusConductorActivity,
                        "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // ── Diálogo de asignación mejorado ────────────────────────────────────────

    private fun mostrarDialogoAsignar(conductor: User) {
        val busesDisponibles = listaBuses.filter {
            it.estado != Bus.ESTADO_MANTENIMIENTO &&
                    (it.conductorId.isNullOrEmpty() || it.conductorId == conductor.uid)
        }

        if (busesDisponibles.isEmpty()) {
            mostrarDialogoError(
                titulo  = "Sin buses disponibles",
                mensaje = "Todos los buses están asignados o en mantenimiento.\nRevisa el estado de la flota en la sección Gestionar Buses."
            )
            return
        }

        // Layout personalizado del diálogo
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_seleccionar_bus, null)
        val tvTitulo   = dialogView.findViewById<TextView>(R.id.tvTituloDialogBus)
        val rvBuses    = dialogView.findViewById<RecyclerView>(R.id.rvBusesDialogo)

        tvTitulo.text = "Asignar bus a ${conductor.name}"
        rvBuses.layoutManager = LinearLayoutManager(this)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setNegativeButton("Cancelar", null)
            .create()

        rvBuses.adapter = BusSeleccionAdapter(busesDisponibles, conductor.uid) { busElegido ->
            dialog.dismiss()
            realizarAsignacion(conductor, busElegido)
        }

        dialog.show()
    }

    private fun realizarAsignacion(conductor: User, bus: Bus) {
        val conductorUid = conductor.uid ?: return
        progressBar.visibility = View.VISIBLE

        // Liberar bus anterior si tenía uno distinto
        val busAnteriorId = conductor.busAsignado
        if (!busAnteriorId.isNullOrEmpty() && busAnteriorId != bus.id) {
            db.child("buses").child(busAnteriorId).updateChildren(mapOf(
                "conductorId"     to null,
                "conductorNombre" to null
            ))
        }

        val updates = mapOf(
            "buses/${bus.id}/conductorId"                to conductorUid,
            "buses/${bus.id}/conductorNombre"            to (conductor.name ?: ""),
            "users/${conductorUid}/busAsignado"          to bus.id,
            "users/${conductorUid}/busPlaca"             to bus.placa,
            "users/${conductorUid}/busModelo"            to bus.modelo
        )

        // Escritura atómica: todo o nada
        db.updateChildren(updates)
            .addOnSuccessListener {
                progressBar.visibility = View.GONE
                mostrarDialogoExito("Bus asignado", "✅ El bus ${bus.placa} (${bus.modelo}) fue asignado a ${conductor.name} correctamente.")
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                mostrarDialogoError("Error al asignar", e.message ?: "Error desconocido")
            }
    }

    // ── Desasignar ────────────────────────────────────────────────────────────

    private fun desasignarBus(conductor: User) {
        val conductorUid = conductor.uid ?: return

        // FIX: buscar el bus asignado directamente en listaBuses
        // (no depender de conductor.busAsignado que puede no estar hidratado)
        val busAsignado = listaBuses.find { it.conductorId == conductorUid }

        if (busAsignado == null) {
            Toast.makeText(this, "Este conductor no tiene bus asignado", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Desasignar bus")
            .setMessage("¿Quitar el bus ${busAsignado.placa} (${busAsignado.modelo}) a ${conductor.name}?")
            .setPositiveButton("Desasignar") { _, _ ->
                progressBar.visibility = View.VISIBLE

                // Escritura atómica
                val updates = mapOf(
                    "buses/${busAsignado.id}/conductorId"        to null,
                    "buses/${busAsignado.id}/conductorNombre"    to null,
                    "users/${conductorUid}/busAsignado"          to null,
                    "users/${conductorUid}/busPlaca"             to null,
                    "users/${conductorUid}/busModelo"            to null
                )

                db.updateChildren(updates)
                    .addOnSuccessListener {
                        progressBar.visibility = View.GONE
                        // La vista se actualiza sola por el ValueEventListener en tiempo real
                        Toast.makeText(this, "✅ Bus desasignado correctamente", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        progressBar.visibility = View.GONE
                        mostrarDialogoError("Error al desasignar", e.message ?: "Error desconocido")
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ── Diálogos de feedback ──────────────────────────────────────────────────

    private fun mostrarDialogoExito(titulo: String, mensaje: String) {
        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage(mensaje)
            .setPositiveButton("Aceptar", null)
            .show()
    }

    private fun mostrarDialogoError(titulo: String, mensaje: String) {
        AlertDialog.Builder(this)
            .setTitle("⚠️ $titulo")
            .setMessage(mensaje)
            .setPositiveButton("Entendido", null)
            .show()
    }
}

// ── Adapter inline para el diálogo de selección de bus ───────────────────────

class BusSeleccionAdapter(
    private val buses: List<Bus>,
    private val conductorUidActual: String?,
    private val onSeleccionar: (Bus) -> Unit
) : RecyclerView.Adapter<BusSeleccionAdapter.VH>() {

    inner class VH(val view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bus_seleccion, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val bus = buses[position]
        val esActual = bus.conductorId == conductorUidActual

        holder.view.findViewById<TextView>(R.id.tvPlacaSeleccion).text  = bus.placa
        holder.view.findViewById<TextView>(R.id.tvModeloSeleccion).text = bus.modelo
        holder.view.findViewById<TextView>(R.id.tvCapSeleccion).text    = "Cap: ${bus.capacidad} pasajeros"

        val tvEstado = holder.view.findViewById<TextView>(R.id.tvEstadoSeleccion)
        val badge    = holder.view.findViewById<TextView>(R.id.tvBadgeActual)
        val card     = holder.view.findViewById<CardView>(R.id.cardBusSeleccion)

        tvEstado.text = when (bus.estado) {
            Bus.ESTADO_EN_RUTA -> "🟡 En ruta"
            else               -> "🟢 Disponible"
        }

        if (esActual) {
            badge.visibility = View.VISIBLE
            card.setCardBackgroundColor(Color.parseColor("#E3F2FD"))
        } else {
            badge.visibility = View.GONE
            card.setCardBackgroundColor(Color.WHITE)
        }

        holder.view.setOnClickListener { onSeleccionar(bus) }
    }

    override fun getItemCount() = buses.size
}