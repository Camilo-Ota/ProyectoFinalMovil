package com.camilootal.copia1app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

/**
 * Pantalla de la EMPRESA para gestionar su flota de buses.
 * Permite crear, editar estado y eliminar buses.
 * Accesible desde HomeEmpresaActivity.
 */
class GestionBusesActivity : AppCompatActivity() {

    private lateinit var db: DatabaseReference
    private lateinit var rvBuses: RecyclerView
    private lateinit var btnAgregarBus: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvSinBuses: TextView

    private val listaBuses = mutableListOf<Bus>()
    private lateinit var adapter: BusAdapter

    private val empresaUid: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gestion_buses)

        db           = FirebaseDatabase.getInstance().reference
        rvBuses      = findViewById(R.id.rvBuses)
        btnAgregarBus= findViewById(R.id.btnAgregarBus)
        progressBar  = findViewById(R.id.progressBarBuses)
        tvSinBuses   = findViewById(R.id.tvSinBuses)

        rvBuses.layoutManager = LinearLayoutManager(this)
        adapter = BusAdapter(
            lista      = listaBuses,
            onEditar   = { mostrarDialogoEditarEstado(it) },
            onEliminar = { confirmarEliminar(it) }
        )
        rvBuses.adapter = adapter

        btnAgregarBus.setOnClickListener { mostrarDialogoCrearBus() }

        cargarBuses()
    }

    // ── Cargar ────────────────────────────────────────────────────────────────

    private fun cargarBuses() {
        progressBar.visibility = View.VISIBLE
        db.child("buses")
            .orderByChild("empresaId")
            .equalTo(empresaUid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    listaBuses.clear()
                    for (snap in snapshot.children) {
                        val bus = snap.getValue(Bus::class.java) ?: continue
                        bus.id = snap.key ?: continue
                        listaBuses.add(bus)
                    }
                    listaBuses.sortBy { it.placa }
                    adapter.notifyDataSetChanged()
                    progressBar.visibility = View.GONE
                    tvSinBuses.visibility  = if (listaBuses.isEmpty()) View.VISIBLE else View.GONE
                }
                override fun onCancelled(e: DatabaseError) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@GestionBusesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // ── Crear bus ─────────────────────────────────────────────────────────────

    private fun mostrarDialogoCrearBus() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_crear_bus, null)
        val etPlaca     = view.findViewById<EditText>(R.id.etPlacaBus)
        val etModelo    = view.findViewById<EditText>(R.id.etModeloBus)
        val etCapacidad = view.findViewById<EditText>(R.id.etCapacidadBus)

        AlertDialog.Builder(this)
            .setTitle("Agregar bus")
            .setView(view)
            .setPositiveButton("Crear") { _, _ ->
                val placa     = etPlaca.text.toString().trim().uppercase()
                val modelo    = etModelo.text.toString().trim()
                val capacidad = etCapacidad.text.toString().trim().toIntOrNull() ?: 0

                if (placa.isEmpty() || modelo.isEmpty()) {
                    Toast.makeText(this, "Placa y modelo son obligatorios", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                crearBus(placa, modelo, capacidad)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun crearBus(placa: String, modelo: String, capacidad: Int) {
        val ref = db.child("buses").push()
        val busId = ref.key ?: return
        val bus = Bus(
            id        = busId,
            placa     = placa,
            modelo    = modelo,
            capacidad = capacidad,
            empresaId = empresaUid,
            activo    = true,
            estado    = Bus.ESTADO_DISPONIBLE
        )
        ref.setValue(bus)
            .addOnSuccessListener {
                Toast.makeText(this, "Bus $placa creado ✅", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al crear bus: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // ── Editar estado ─────────────────────────────────────────────────────────

    private fun mostrarDialogoEditarEstado(bus: Bus) {
        val estados = arrayOf("Disponible", "En ruta", "Mantenimiento")
        val valores = arrayOf(Bus.ESTADO_DISPONIBLE, Bus.ESTADO_EN_RUTA, Bus.ESTADO_MANTENIMIENTO)
        val actual  = valores.indexOf(bus.estado).coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("Estado de ${bus.placa}")
            .setSingleChoiceItems(estados, actual) { dialog, which ->
                db.child("buses").child(bus.id).child("estado").setValue(valores[which])
                    .addOnSuccessListener {
                        Toast.makeText(this, "Estado actualizado", Toast.LENGTH_SHORT).show()
                    }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ── Eliminar ──────────────────────────────────────────────────────────────

    private fun confirmarEliminar(bus: Bus) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar bus")
            .setMessage("¿Eliminar el bus ${bus.placa} (${bus.modelo})?\nSi tiene conductor asignado, quedará sin bus.")
            .setPositiveButton("Eliminar") { _, _ ->
                // Quitar la asignación del conductor si aplica
                if (!bus.conductorId.isNullOrEmpty()) {
                    db.child("users").child(bus.conductorId!!).child("busAsignado").removeValue()
                }
                db.child("buses").child(bus.id).removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Bus eliminado", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
