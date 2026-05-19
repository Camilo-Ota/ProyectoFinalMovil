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
import com.google.firebase.database.*
import com.camilootal.copia1app.Ruta

class ListaRutasActivity : AppCompatActivity() {

    private lateinit var recyclerRutas: RecyclerView
    private lateinit var tvSinRutas: TextView
    private lateinit var progressBarRutas: ProgressBar

    private val db = FirebaseDatabase.getInstance().reference
    private val listaRutas = mutableListOf<Ruta>()
    private lateinit var rutaAdapter: RutaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_rutas)

        recyclerRutas = findViewById(R.id.recyclerRutas)
        tvSinRutas = findViewById(R.id.tvSinRutas)
        progressBarRutas = findViewById(R.id.progressBarRutas)

        recyclerRutas.layoutManager = LinearLayoutManager(this)

        rutaAdapter = RutaAdapter(listaRutas) { rutaSeleccionada ->
            abrirPantallaConfigurarPuntos(rutaSeleccionada)
        }

        recyclerRutas.adapter = rutaAdapter

        cargarRutas()
    }

    private fun cargarRutas() {
        progressBarRutas.visibility = View.VISIBLE
        tvSinRutas.visibility = View.GONE

        db.child("rutas")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    listaRutas.clear()

                    for (rutaSnap in snapshot.children) {
                        val ruta = rutaSnap.getValue(Ruta::class.java)
                        if (ruta != null) {
                            listaRutas.add(ruta)
                        }
                    }

                    listaRutas.sortByDescending { it.creadaEn }

                    rutaAdapter.notifyDataSetChanged()
                    progressBarRutas.visibility = View.GONE

                    if (listaRutas.isEmpty()) {
                        tvSinRutas.visibility = View.VISIBLE
                    } else {
                        tvSinRutas.visibility = View.GONE
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    progressBarRutas.visibility = View.GONE
                    Toast.makeText(
                        this@ListaRutasActivity,
                        "Error al cargar rutas: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }
    private fun abrirPantallaConfigurarPuntos(ruta: Ruta) {
        val intent = Intent(this, ConfigurarPuntosRutaActivity::class.java)
        intent.putExtra("rutaId", ruta.id)
        intent.putExtra("rutaNombre", ruta.nombre)
        intent.putExtra("rutaRadio", ruta.radioDeteccion)
        startActivity(intent)
    }
}