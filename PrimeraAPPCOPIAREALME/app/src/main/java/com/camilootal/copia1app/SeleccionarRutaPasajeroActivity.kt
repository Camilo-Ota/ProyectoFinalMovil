package com.camilootal.copia1app

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class SeleccionarRutaPasajeroActivity : AppCompatActivity() {

    private val listaRutas = mutableListOf<Ruta>()
    private lateinit var adapter: RutaPasajeroAdapter
    private val db = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seleccionar_ruta_pasajero)

        val recycler = findViewById<RecyclerView>(R.id.recyclerRutasPasajero)
        val progress = findViewById<ProgressBar>(R.id.progressBarRutasPasajero)
        val layoutVacio = findViewById<View>(R.id.layoutSinRutasPasajero)

        adapter = RutaPasajeroAdapter(listaRutas) { ruta ->
            startActivity(Intent(this, MapaRutaEnVivoActivity::class.java).apply {
                putExtra("rutaId", ruta.id)
                putExtra("rutaNombre", ruta.nombre)
            })
        }
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        progress.visibility = View.VISIBLE
        db.child("rutas").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listaRutas.clear()
                for (child in snapshot.children) {
                    val ruta = child.getValue(Ruta::class.java) ?: continue
                    ruta.id = child.key ?: ""; listaRutas.add(ruta)
                }
                progress.visibility = View.GONE
                layoutVacio.visibility = if (listaRutas.isEmpty()) View.VISIBLE else View.GONE
                recycler.visibility   = if (listaRutas.isEmpty()) View.GONE  else View.VISIBLE
                adapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {
                progress.visibility = View.GONE
                Toast.makeText(this@SeleccionarRutaPasajeroActivity, "Error al cargar rutas", Toast.LENGTH_SHORT).show()
            }
        })
    }
}

class RutaPasajeroAdapter(
    private val rutas: List<Ruta>,
    private val onClick: (Ruta) -> Unit
) : RecyclerView.Adapter<RutaPasajeroAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val nombre: TextView = view.findViewById(R.id.tvNombreRutaPasajero)
        val desc: TextView   = view.findViewById(R.id.tvDescripcionRutaPasajero)
        val estado: TextView = view.findViewById(R.id.tvEstadoRutaPasajero)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_ruta_pasajero, parent, false))

    override fun onBindViewHolder(h: VH, pos: Int) {
        val r = rutas[pos]
        h.nombre.text = r.nombre.ifEmpty { "Ruta sin nombre" }
        h.desc.text   = r.descripcion.ifEmpty { "Sin descripción" }
        h.estado.text = "• En servicio"
        h.itemView.setOnClickListener { onClick(r) }
    }

    override fun getItemCount() = rutas.size
}