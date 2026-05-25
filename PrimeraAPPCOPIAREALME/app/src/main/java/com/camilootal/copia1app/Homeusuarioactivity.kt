package com.camilootal.copia1app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class HomeUsuarioActivity : AppCompatActivity() {

    private lateinit var tvBienvenida: TextView
    private lateinit var btnVerRutas: android.widget.Button
    private lateinit var btnCerrarSesion: android.widget.Button

    // Canal de conductores (solo lectura)
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutSinPublicaciones: LinearLayout
    private lateinit var recycler: RecyclerView
    private lateinit var filtroTodos: TextView
    private lateinit var filtroAlertas: TextView
    private lateinit var filtroTrancons: TextView
    private lateinit var filtroInfo: TextView

    private lateinit var dbRef: DatabaseReference
    private val todasLasPublicaciones = mutableListOf<Publicacion>()
    private val listaFiltrada = mutableListOf<Publicacion>()
    private lateinit var adapter: PublicacionAdapter
    private var filtroActivo = "Todos"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_pasajero)

        tvBienvenida      = findViewById(R.id.tvBienvenidaPasajero)
        btnVerRutas       = findViewById(R.id.btnVerRutasEnVivo)
        btnCerrarSesion   = findViewById(R.id.btnCerrarSesionPasajero)

        progressBar           = findViewById(R.id.progressBarCanal)
        layoutSinPublicaciones = findViewById(R.id.layoutSinPublicaciones)
        recycler              = findViewById(R.id.recyclerPublicaciones)
        filtroTodos           = findViewById(R.id.filtroTodos)
        filtroAlertas         = findViewById(R.id.filtroAlertas)
        filtroTrancons        = findViewById(R.id.filtroTrancons)
        filtroInfo            = findViewById(R.id.filtroInfo)

        dbRef = FirebaseDatabase.getInstance().getReference("canal_conductores")

        cargarNombreUsuario()
        configurarAdapter()
        configurarFiltros()
        escucharPublicaciones()

        btnVerRutas.setOnClickListener {
            startActivity(Intent(this, SeleccionarRutaPasajeroActivity::class.java))
        }

        btnCerrarSesion.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LogIn::class.java))
            finishAffinity()
        }
    }

    private fun cargarNombreUsuario() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            tvBienvenida.text = "Hola, Visitante"
            return
        }
        FirebaseDatabase.getInstance().reference
            .child("users")
            .child(uid)
            .child("name")
            .get()
            .addOnSuccessListener {
                val nombre = it.getValue(String::class.java) ?: "Usuario"
                tvBienvenida.text = "Hola, $nombre"
            }
    }

    private fun configurarAdapter() {
        adapter = PublicacionAdapter(listaFiltrada) { publicacion ->
            if (!publicacion.tieneUbicacion) {
                Toast.makeText(this, "Esta publicación no tiene ubicación registrada", Toast.LENGTH_SHORT).show()
                return@PublicacionAdapter
            }
            val intent = Intent(this, MapaAlertaActivity::class.java).apply {
                putExtra("latitud",     publicacion.latitud)
                putExtra("longitud",    publicacion.longitud)
                putExtra("categoria",   publicacion.categoria)
                putExtra("autorNombre", publicacion.autorNombre)
                putExtra("texto",       publicacion.texto)
                putExtra("timestamp",   publicacion.timestamp)
            }
            startActivity(intent)
        }
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter
    }

    private fun configurarFiltros() {
        val filtros = mapOf(
            filtroTodos    to "Todos",
            filtroAlertas  to "Alerta",
            filtroTrancons to "Trancón",
            filtroInfo     to "Info"
        )
        filtros.forEach { (view, filtro) ->
            view.setOnClickListener {
                filtroActivo = filtro
                filtros.keys.forEach { f ->
                    f.setBackgroundResource(R.drawable.tag_normal_background)
                    f.setTextColor(resources.getColor(R.color.colorPrimary, null))
                }
                view.setBackgroundResource(R.drawable.tag_selected_background)
                view.setTextColor(resources.getColor(R.color.colorBlanco, null))
                aplicarFiltro()
            }
        }
    }

    private fun escucharPublicaciones() {
        progressBar.visibility = View.VISIBLE
        dbRef.orderByChild("timestamp")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    progressBar.visibility = View.GONE
                    todasLasPublicaciones.clear()
                    for (child in snapshot.children) {
                        val pub = child.getValue(Publicacion::class.java)
                        if (pub != null) todasLasPublicaciones.add(pub)
                    }
                    todasLasPublicaciones.reverse()
                    aplicarFiltro()
                }
                override fun onCancelled(error: DatabaseError) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@HomeUsuarioActivity,
                        "Error al cargar: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun aplicarFiltro() {
        listaFiltrada.clear()
        listaFiltrada.addAll(
            if (filtroActivo == "Todos") todasLasPublicaciones
            else todasLasPublicaciones.filter { it.categoria == filtroActivo }
        )
        adapter.notifyDataSetChanged()
        layoutSinPublicaciones.visibility = if (listaFiltrada.isEmpty()) View.VISIBLE else View.GONE
        recycler.visibility               = if (listaFiltrada.isEmpty()) View.GONE   else View.VISIBLE
    }
}
