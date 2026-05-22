package com.camilootal.copia1app

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

// Activity del Canal de Conductores.
// Permite publicar novedades (alertas, trancos, info) y leer las de otros conductores.
// Los datos se guardan en Firebase Realtime Database bajo "canal_conductores/"
class CanalConductoresActivity : AppCompatActivity() {

    // ───── Firebase ─────
    private lateinit var auth: FirebaseAuth
    private lateinit var dbRef: DatabaseReference

    // ───── Vistas ─────
    private lateinit var tvAvatarPropio: TextView
    private lateinit var etNuevaPublicacion: EditText
    private lateinit var btnPublicar: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutSinPublicaciones: LinearLayout
    private lateinit var recycler: RecyclerView

    // Tags de categoría (selector al publicar)
    private lateinit var tagAlerta: TextView
    private lateinit var tagTrancon: TextView
    private lateinit var tagInfo: TextView
    private lateinit var tagGeneral: TextView

    // Filtros de la lista
    private lateinit var filtroTodos: TextView
    private lateinit var filtroAlertas: TextView
    private lateinit var filtroTrancons: TextView
    private lateinit var filtroInfo: TextView

    // ───── Estado ─────
    private var categoriaSeleccionada = "Alerta"   // categoría activa al publicar
    private var filtroActivo = "Todos"              // filtro activo en la lista
    private val todasLasPublicaciones = mutableListOf<Publicacion>()
    private lateinit var adapter: PublicacionAdapter
    private val listaFiltrada = mutableListOf<Publicacion>()

    // Nombre del conductor actual (se carga desde la DB igual que en el resto del proyecto)
    private var nombreConductor = "Conductor"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_canal_conductores)

        auth = FirebaseAuth.getInstance()
        // Nodo raíz del canal en Realtime Database
        dbRef = FirebaseDatabase.getInstance().getReference("canal_conductores")

        enlazarVistas()
        configurarAdapter()
        configurarTagsCategoria()
        configurarFiltros()
        cargarNombrePropio()
        escucharPublicaciones()

        btnPublicar.setOnClickListener { publicar() }
    }

    // ── Enlaza todos los findViewById de una vez ──────────────────────────────
    private fun enlazarVistas() {
        tvAvatarPropio        = findViewById(R.id.tvAvatarPropio)
        etNuevaPublicacion    = findViewById(R.id.etNuevaPublicacion)
        btnPublicar           = findViewById(R.id.btnPublicar)
        progressBar           = findViewById(R.id.progressBarCanal)
        layoutSinPublicaciones = findViewById(R.id.layoutSinPublicaciones)
        recycler              = findViewById(R.id.recyclerPublicaciones)

        tagAlerta   = findViewById(R.id.tagAlerta)
        tagTrancon  = findViewById(R.id.tagTrancon)
        tagInfo     = findViewById(R.id.tagInfo)
        tagGeneral  = findViewById(R.id.tagGeneral)

        filtroTodos    = findViewById(R.id.filtroTodos)
        filtroAlertas  = findViewById(R.id.filtroAlertas)
        filtroTrancons = findViewById(R.id.filtroTrancons)
        filtroInfo     = findViewById(R.id.filtroInfo)
    }

    // ── Configura el RecyclerView con su adapter ──────────────────────────────
    private fun configurarAdapter() {
        adapter = PublicacionAdapter(listaFiltrada)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter
    }

    // ── Lógica de selección de categoría al publicar ──────────────────────────
    private fun configurarTagsCategoria() {
        val tags = mapOf(
            tagAlerta  to "Alerta",
            tagTrancon to "Trancón",
            tagInfo    to "Info",
            tagGeneral to "General"
        )
        tags.forEach { (view, categoria) ->
            view.setOnClickListener {
                categoriaSeleccionada = categoria
                // Resetea estilos de todos los tags
                tags.keys.forEach { t ->
                    t.setBackgroundResource(R.drawable.tag_normal_background)
                    t.setTextColor(resources.getColor(R.color.colorPrimary, null))
                }
                // Marca el seleccionado
                view.setBackgroundResource(R.drawable.tag_selected_background)
                view.setTextColor(resources.getColor(R.color.colorBlanco, null))
            }
        }
    }

    // ── Lógica de filtros de la lista ─────────────────────────────────────────
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
                // Resetea estilos
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

    // ── Carga el nombre del conductor desde users/{uid} (igual que en SignUp) ─
    private fun cargarNombrePropio() {
        val uid = auth.currentUser?.uid ?: return
        FirebaseDatabase.getInstance().getReference("users").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)
                    nombreConductor = user?.name ?: "Conductor"
                    // Avatar con las iniciales del nombre
                    tvAvatarPropio.text = nombreConductor
                        .trim().split(" ").take(2)
                        .joinToString("") { it.first().uppercaseChar().toString() }
                        .ifEmpty { "YO" }
                }
                override fun onCancelled(error: DatabaseError) { /* no-op */ }
            })
    }

    // ── Publica en Firebase ───────────────────────────────────────────────────
    private fun publicar() {
        val texto = etNuevaPublicacion.text.toString().trim()
        if (texto.isEmpty()) {
            Toast.makeText(this, "Escribe algo antes de publicar", Toast.LENGTH_SHORT).show()
            return
        }

        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Debes iniciar sesión", Toast.LENGTH_SHORT).show()
            return
        }

        btnPublicar.isEnabled = false

        // Genera una clave única igual que el resto del proyecto usa .push()
        val nuevaRef = dbRef.push()
        val publicacion = Publicacion(
            id           = nuevaRef.key ?: "",
            autorId      = uid,
            autorNombre  = nombreConductor,
            texto        = texto,
            categoria    = categoriaSeleccionada,
            timestamp    = System.currentTimeMillis()
        )

        nuevaRef.setValue(publicacion)
            .addOnSuccessListener {
                etNuevaPublicacion.setText("")
                btnPublicar.isEnabled = true
                Toast.makeText(this, "Publicado ✓", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                btnPublicar.isEnabled = true
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // ── Escucha en tiempo real todas las publicaciones ────────────────────────
    private fun escucharPublicaciones() {
        progressBar.visibility = View.VISIBLE

        // orderByChild("timestamp") = más recientes al final → invertimos la lista
        dbRef.orderByChild("timestamp")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    progressBar.visibility = View.GONE
                    todasLasPublicaciones.clear()

                    for (child in snapshot.children) {
                        val pub = child.getValue(Publicacion::class.java)
                        if (pub != null) todasLasPublicaciones.add(pub)
                    }
                    // Más reciente primero
                    todasLasPublicaciones.reverse()
                    aplicarFiltro()
                }

                override fun onCancelled(error: DatabaseError) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@CanalConductoresActivity,
                        "Error al cargar: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    // ── Aplica el filtro activo y refresca el adapter ─────────────────────────
    private fun aplicarFiltro() {
        listaFiltrada.clear()
        listaFiltrada.addAll(
            if (filtroActivo == "Todos") todasLasPublicaciones
            else todasLasPublicaciones.filter { it.categoria == filtroActivo }
        )
        adapter.notifyDataSetChanged()

        // Muestra/oculta el estado vacío
        layoutSinPublicaciones.visibility =
            if (listaFiltrada.isEmpty()) View.VISIBLE else View.GONE
        recycler.visibility =
            if (listaFiltrada.isEmpty()) View.GONE else View.VISIBLE
    }
}