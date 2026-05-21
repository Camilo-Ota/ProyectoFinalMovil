package com.camilootal.copia1app

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class CanalConductoresActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var dbRef: DatabaseReference

    private lateinit var tvAvatarPropio: TextView
    private lateinit var etNuevaPublicacion: EditText
    private lateinit var btnPublicar: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutSinPublicaciones: LinearLayout
    private lateinit var recycler: RecyclerView

    // Layout completo del área de publicar (para ocultarlo en modo solo lectura)
    private lateinit var layoutPublicar: View

    private lateinit var tagAlerta: TextView
    private lateinit var tagTrancon: TextView
    private lateinit var tagInfo: TextView
    private lateinit var tagGeneral: TextView

    private lateinit var filtroTodos: TextView
    private lateinit var filtroAlertas: TextView
    private lateinit var filtroTrancons: TextView
    private lateinit var filtroInfo: TextView

    private var categoriaSeleccionada = "Alerta"
    private var filtroActivo = "Todos"
    private val todasLasPublicaciones = mutableListOf<Publicacion>()
    private lateinit var adapter: PublicacionAdapter
    private val listaFiltrada = mutableListOf<Publicacion>()
    private var nombreConductor = "Conductor"

    //  FIX: Detectar si es usuario (solo lectura) o conductor/admin (puede publicar)
    private var soloLectura = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_canal_conductores)

        // El HomeUsuarioActivity pasa este extra para indicar modo solo lectura
        soloLectura = intent.getBooleanExtra("soloLectura", false)

        auth  = FirebaseAuth.getInstance()
        dbRef = FirebaseDatabase.getInstance().getReference("canal_conductores")

        enlazarVistas()
        aplicarModoLectura()
        configurarAdapter()
        configurarTagsCategoria()
        configurarFiltros()
        cargarNombrePropio()
        escucharPublicaciones()

        btnPublicar.setOnClickListener { publicar() }
    }

    private fun enlazarVistas() {
        tvAvatarPropio         = findViewById(R.id.tvAvatarPropio)
        etNuevaPublicacion     = findViewById(R.id.etNuevaPublicacion)
        btnPublicar            = findViewById(R.id.btnPublicar)
        progressBar            = findViewById(R.id.progressBarCanal)
        layoutSinPublicaciones = findViewById(R.id.layoutSinPublicaciones)
        recycler               = findViewById(R.id.recyclerPublicaciones)
        layoutPublicar         = findViewById(R.id.layoutPublicar)

        tagAlerta  = findViewById(R.id.tagAlerta)
        tagTrancon = findViewById(R.id.tagTrancon)
        tagInfo    = findViewById(R.id.tagInfo)
        tagGeneral = findViewById(R.id.tagGeneral)

        filtroTodos    = findViewById(R.id.filtroTodos)
        filtroAlertas  = findViewById(R.id.filtroAlertas)
        filtroTrancons = findViewById(R.id.filtroTrancons)
        filtroInfo     = findViewById(R.id.filtroInfo)
    }

    // FIX: Ocultar toda el área de publicar si es usuario
    private fun aplicarModoLectura() {
        if (soloLectura) {
            layoutPublicar.visibility  = View.GONE
            tvAvatarPropio.visibility  = View.GONE
        }
    }

    private fun configurarAdapter() {
        adapter = PublicacionAdapter(listaFiltrada)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter
    }

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
                tags.keys.forEach { t ->
                    t.setBackgroundResource(R.drawable.tag_normal_background)
                    t.setTextColor(resources.getColor(R.color.colorPrimary, null))
                }
                view.setBackgroundResource(R.drawable.tag_selected_background)
                view.setTextColor(resources.getColor(R.color.colorBlanco, null))
            }
        }
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

    private fun cargarNombrePropio() {
        if (soloLectura) return  // usuarios no necesitan cargar su nombre para publicar
        val uid = auth.currentUser?.uid ?: return
        FirebaseDatabase.getInstance().getReference("users").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)
                    nombreConductor = user?.name ?: "Conductor"
                    tvAvatarPropio.text = nombreConductor
                        .trim().split(" ").take(2)
                        .joinToString("") { it.first().uppercaseChar().toString() }
                        .ifEmpty { "YO" }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun publicar() {
        if (soloLectura) return  // doble protección
        val texto = etNuevaPublicacion.text.toString().trim()
        if (texto.isEmpty()) {
            Toast.makeText(this, "Escribe algo antes de publicar", Toast.LENGTH_SHORT).show()
            return
        }
        val uid = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "Debes iniciar sesión", Toast.LENGTH_SHORT).show()
            return
        }

        btnPublicar.isEnabled = false
        val nuevaRef    = dbRef.push()
        val publicacion = Publicacion(
            id          = nuevaRef.key ?: "",
            autorId     = uid,
            autorNombre = nombreConductor,
            texto       = texto,
            categoria   = categoriaSeleccionada,
            timestamp   = System.currentTimeMillis()
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
                    Toast.makeText(this@CanalConductoresActivity, "Error al cargar: ${error.message}", Toast.LENGTH_SHORT).show()
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
        recycler.visibility               = if (listaFiltrada.isEmpty()) View.GONE else View.VISIBLE
    }
}