package com.camilootal.copia1app

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
import com.camilootal.copia1app.Recorrido

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
        tvSinHistorial = findViewById(R.id.tvSinHistorial)
        progressBarHistorial = findViewById(R.id.progressBarHistorial)

        recyclerHistorialRecorridos.layoutManager = LinearLayoutManager(this)
        historialRecorridosAdapter = HistorialRecorridosAdapter(listaRecorridos)
        recyclerHistorialRecorridos.adapter = historialRecorridosAdapter

        cargarHistorial()
    }

    private fun cargarHistorial() {
        progressBarHistorial.visibility = View.VISIBLE
        tvSinHistorial.visibility = View.GONE

        db.child("recorridos")
            .get()
            .addOnSuccessListener { snapshot ->
                listaRecorridos.clear()

                for (recorridoSnap in snapshot.children) {
                    val recorrido = recorridoSnap.getValue(Recorrido::class.java)
                    if (recorrido != null) {
                        val cantidadPuntos = recorridoSnap.child("puntosRegistrados").childrenCount.toInt()
                        listaRecorridos.add(Pair(recorrido, cantidadPuntos))
                    }
                }

                listaRecorridos.sortByDescending { it.first.inicioTiempo }

                historialRecorridosAdapter.notifyDataSetChanged()
                progressBarHistorial.visibility = View.GONE
                tvSinHistorial.visibility =
                    if (listaRecorridos.isEmpty()) View.VISIBLE else View.GONE
            }
            .addOnFailureListener { e ->
                progressBarHistorial.visibility = View.GONE
                Toast.makeText(
                    this,
                    "Error al cargar historial: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}