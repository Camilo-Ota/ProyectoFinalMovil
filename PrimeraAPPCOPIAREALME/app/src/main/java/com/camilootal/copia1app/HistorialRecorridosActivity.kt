package com.camilootal.copia1app

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class HistorialRecorridosActivity : AppCompatActivity() {

    private lateinit var recyclerHistorialRecorridos: RecyclerView
    private lateinit var tvSinHistorial: TextView
    private lateinit var progressBarHistorial: ProgressBar

    private val db = FirebaseDatabase.getInstance().reference
    private val listaRecorridos = mutableListOf<Pair<Recorrido, Int>>()
    private lateinit var historialRecorridosAdapter: HistorialRecorridosAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial_recorridos)

        recyclerHistorialRecorridos = findViewById(R.id.recyclerHistorialRecorridos)
        tvSinHistorial              = findViewById(R.id.tvSinHistorial)
        progressBarHistorial        = findViewById(R.id.progressBarHistorial)
        recyclerHistorialRecorridos.layoutManager = LinearLayoutManager(this)

        historialRecorridosAdapter = HistorialRecorridosAdapter(listaRecorridos) { recorrido ->
            val intent = android.content.Intent(this, DetalleRecorridoActivity::class.java)
            intent.putExtra("recorridoId",  recorrido.id)
            intent.putExtra("rutaId",       recorrido.rutaId)
            intent.putExtra("rutaNombre",   recorrido.rutaNombre)
            intent.putExtra("estado",       recorrido.estado)
            intent.putExtra("inicioTiempo", recorrido.inicioTiempo)
            startActivity(intent)
        }
        recyclerHistorialRecorridos.adapter = historialRecorridosAdapter

        // ── Leer el modo con que fue abierta esta pantalla ──────────────────
        //
        // "empresa"  → admin ve historial general de su empresa (todos sus conductores)
        // "conductor"→ admin toca "Ver historial" en un conductor específico
        // "propio"   → conductor ve su propio historial
        // (sin modo) → fallback: uid propio (conductor desde otra pantalla)

        val modo           = intent.getStringExtra("modo")
        val conductorUid   = intent.getStringExtra("conductorUid")
        val conductorNombre= intent.getStringExtra("conductorNombre")
        val empresaId      = intent.getStringExtra("empresaId")

        when (modo) {
            "empresa" -> {
                supportActionBar?.title = "Historial de mi empresa"
                if (empresaId.isNullOrEmpty()) {
                    Toast.makeText(this, "No se encontró la empresa", Toast.LENGTH_SHORT).show()
                    finish()
                    return
                }
                cargarHistorialEmpresa(empresaId)
            }
            "conductor" -> {
                supportActionBar?.title = "Historial de ${conductorNombre ?: "conductor"}"
                if (conductorUid.isNullOrEmpty()) {
                    Toast.makeText(this, "Conductor no válido", Toast.LENGTH_SHORT).show()
                    finish()
                    return
                }
                cargarHistorialUid(conductorUid)
            }
            else -> {
                // Conductor viendo el suyo propio
                val uid = conductorUid
                    ?: FirebaseAuth.getInstance().currentUser?.uid
                if (uid.isNullOrEmpty()) {
                    Toast.makeText(this, "Sesión no válida", Toast.LENGTH_SHORT).show()
                    finish()
                    return
                }
                supportActionBar?.title = "Mi historial"
                cargarHistorialUid(uid)
            }
        }
    }

    /** Modo PROPIO o admin viendo UN conductor: filtra por usuarioId */
    private fun cargarHistorialUid(uid: String) {
        progressBarHistorial.visibility = View.VISIBLE
        tvSinHistorial.visibility       = View.GONE

        db.child("recorridos")
            .orderByChild("usuarioId")
            .equalTo(uid)
            .get()
            .addOnSuccessListener { snapshot ->
                procesarSnapshot(snapshot)
            }
            .addOnFailureListener { e ->
                progressBarHistorial.visibility = View.GONE
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    /**
     * Modo EMPRESA: primero busca todos los conductores de la empresa,
     * luego carga sus recorridos y los combina en una sola lista.
     */
    private fun cargarHistorialEmpresa(empresaId: String) {
        progressBarHistorial.visibility = View.VISIBLE
        tvSinHistorial.visibility       = View.GONE

        // 1. Obtener todos los conductores de esta empresa
        db.child("users")
            .orderByChild("empresaId")
            .equalTo(empresaId)
            .get()
            .addOnSuccessListener { usersSnap ->

                val uidsConductores = usersSnap.children
                    .mapNotNull { it.getValue(User::class.java) }
                    .filter { it.role == User.ROL_CONDUCTOR }
                    .mapNotNull { it.uid }

                if (uidsConductores.isEmpty()) {
                    progressBarHistorial.visibility = View.GONE
                    tvSinHistorial.visibility       = View.VISIBLE
                    return@addOnSuccessListener
                }

                // 2. Cargar todos los recorridos y filtrar por los uids de conductores
                db.child("recorridos").get()
                    .addOnSuccessListener { recorridosSnap ->
                        listaRecorridos.clear()

                        for (recorridoSnap in recorridosSnap.children) {
                            val recorrido = recorridoSnap.getValue(Recorrido::class.java) ?: continue
                            // Solo incluir si el conductor pertenece a la empresa
                            if (recorrido.usuarioId !in uidsConductores) continue
                            val cantidadPuntos = recorridoSnap.child("puntosRegistrados").childrenCount.toInt()
                            listaRecorridos.add(Pair(recorrido, cantidadPuntos))
                        }

                        listaRecorridos.sortByDescending { it.first.inicioTiempo }
                        historialRecorridosAdapter.notifyDataSetChanged()
                        progressBarHistorial.visibility = View.GONE
                        tvSinHistorial.visibility =
                            if (listaRecorridos.isEmpty()) View.VISIBLE else View.GONE
                    }
                    .addOnFailureListener { e ->
                        progressBarHistorial.visibility = View.GONE
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                progressBarHistorial.visibility = View.GONE
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun procesarSnapshot(snapshot: com.google.firebase.database.DataSnapshot) {
        listaRecorridos.clear()
        for (recorridoSnap in snapshot.children) {
            val recorrido = recorridoSnap.getValue(Recorrido::class.java) ?: continue
            val cantidadPuntos = recorridoSnap.child("puntosRegistrados").childrenCount.toInt()
            listaRecorridos.add(Pair(recorrido, cantidadPuntos))
        }
        listaRecorridos.sortByDescending { it.first.inicioTiempo }
        historialRecorridosAdapter.notifyDataSetChanged()
        progressBarHistorial.visibility = View.GONE
        tvSinHistorial.visibility =
            if (listaRecorridos.isEmpty()) View.VISIBLE else View.GONE
    }
}