package com.camilootal.copia1app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.camilootal.copia1app.Ruta

class ListaRutasRecorridoActivity : AppCompatActivity() {

    private lateinit var recyclerRutasRecorrido: RecyclerView
    private lateinit var tvSinRutasRecorrido: TextView
    private lateinit var progressBarRutasRecorrido: ProgressBar

    private val db = FirebaseDatabase.getInstance().reference
    private val listaRutas = mutableListOf<Ruta>()
    private lateinit var rutaRecorridoAdapter: RutaRecorridoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_rutas_recorrido)

        recyclerRutasRecorrido = findViewById(R.id.recyclerRutasRecorrido)
        tvSinRutasRecorrido = findViewById(R.id.tvSinRutasRecorrido)
        progressBarRutasRecorrido = findViewById(R.id.progressBarRutasRecorrido)

        recyclerRutasRecorrido.layoutManager = LinearLayoutManager(this)

        rutaRecorridoAdapter = RutaRecorridoAdapter(listaRutas) { ruta ->
            val intent = Intent(this, RecorridoRutaActivity::class.java)
            intent.putExtra("rutaId", ruta.id)
            intent.putExtra("rutaNombre", ruta.nombre)
            intent.putExtra("rutaRadio", ruta.radioDeteccion)
            startActivity(intent)
        }

        recyclerRutasRecorrido.adapter = rutaRecorridoAdapter

        cargarRutas()
    }

    private fun cargarRutas() {
        progressBarRutasRecorrido.visibility = View.VISIBLE
        tvSinRutasRecorrido.visibility = View.GONE

        db.child("rutas")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    listaRutas.clear()

                    for (rutaSnap in snapshot.children) {
                        val ruta = rutaSnap.getValue(Ruta::class.java)
                        if (ruta != null && ruta.activa) {
                            listaRutas.add(ruta)
                        }
                    }

                    listaRutas.sortByDescending { it.creadaEn }
                    rutaRecorridoAdapter.notifyDataSetChanged()

                    progressBarRutasRecorrido.visibility = View.GONE
                    tvSinRutasRecorrido.visibility =
                        if (listaRutas.isEmpty()) View.VISIBLE else View.GONE
                }

                override fun onCancelled(error: DatabaseError) {
                    progressBarRutasRecorrido.visibility = View.GONE
                    Toast.makeText(
                        this@ListaRutasRecorridoActivity,
                        "Error al cargar rutas: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }
}