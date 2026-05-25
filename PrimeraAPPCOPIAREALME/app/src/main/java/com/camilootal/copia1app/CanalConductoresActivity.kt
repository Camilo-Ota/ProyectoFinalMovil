package com.camilootal.copia1app

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.LocationServices
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
    private var soloLectura = false

    // ── Ubicación capturada al momento de publicar ───────────────────────────
    private var ultimaUbicacion: Location? = null

    // ── Notificaciones ───────────────────────────────────────────────────────
    private val CANAL_ID = "canal_conductores_alertas"
    private var ultimoTimestampVisto = 0L  // para no re-notificar publicaciones antiguas

    companion object {
        private var instanciaActiva = false   // true si la Activity está en primer plano
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_canal_conductores)

        soloLectura = intent.getBooleanExtra("soloLectura", false)

        auth  = FirebaseAuth.getInstance()
        dbRef = FirebaseDatabase.getInstance().getReference("canal_conductores")

        crearCanalNotificacion()
        enlazarVistas()
        aplicarModoLectura()
        configurarAdapter()
        configurarTagsCategoria()
        configurarFiltros()
        cargarNombrePropio()
        capturarUbicacionActual()
        escucharPublicaciones()

        btnPublicar.setOnClickListener { publicar() }
    }

    override fun onResume() {
        super.onResume()
        instanciaActiva = true
        // Refrescar ubicación cada vez que el usuario vuelve a la pantalla
        capturarUbicacionActual()
    }

    override fun onPause() {
        super.onPause()
        instanciaActiva = false
    }

    // ── Vistas ───────────────────────────────────────────────────────────────

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

    private fun aplicarModoLectura() {
        if (soloLectura) {
            layoutPublicar.visibility  = View.GONE
            tvAvatarPropio.visibility  = View.GONE
        }
    }

    // ── Adapter ──────────────────────────────────────────────────────────────

    private fun configurarAdapter() {
        adapter = PublicacionAdapter(listaFiltrada) { publicacion ->
            // Click en una publicación → abrir mapa de la alerta
            abrirMapaAlerta(publicacion)
        }
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter
    }

    // ── Tags y filtros ───────────────────────────────────────────────────────

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

    // ── Carga de nombre propio ───────────────────────────────────────────────

    private fun cargarNombrePropio() {
        if (soloLectura) return
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

    // ── Captura de ubicación ─────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    private fun capturarUbicacionActual() {
        val tienePermiso = ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

        if (!tienePermiso) return   // Si no hay permiso, se publica sin ubicación

        LocationServices.getFusedLocationProviderClient(this)
            .lastLocation
            .addOnSuccessListener { location: Location? ->
                ultimaUbicacion = location
            }
    }

    // ── Publicar ─────────────────────────────────────────────────────────────

    private fun publicar() {
        if (soloLectura) return
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
        val nuevaRef = dbRef.push()

        // Capturar ubicación al momento exacto de publicar
        val loc = ultimaUbicacion
        val publicacion = Publicacion(
            id            = nuevaRef.key ?: "",
            autorId       = uid,
            autorNombre   = nombreConductor,
            texto         = texto,
            categoria     = categoriaSeleccionada,
            timestamp     = System.currentTimeMillis(),
            latitud       = loc?.latitude ?: 0.0,
            longitud      = loc?.longitude ?: 0.0,
            tieneUbicacion = loc != null
        )

        nuevaRef.setValue(publicacion)
            .addOnSuccessListener {
                etNuevaPublicacion.setText("")
                btnPublicar.isEnabled = true
                // Refrescar ubicación para la próxima publicación
                capturarUbicacionActual()
                Toast.makeText(this, "Publicado ✓", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                btnPublicar.isEnabled = true
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // ── Escuchar publicaciones de Firebase ───────────────────────────────────

    private fun escucharPublicaciones() {
        progressBar.visibility = View.VISIBLE
        // ultimoTimestampVisto = ahora, para no notificar publicaciones históricas al entrar
        ultimoTimestampVisto = System.currentTimeMillis()

        dbRef.orderByChild("timestamp")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    progressBar.visibility = View.GONE
                    todasLasPublicaciones.clear()

                    for (child in snapshot.children) {
                        val pub = child.getValue(Publicacion::class.java)
                        if (pub != null) {
                            todasLasPublicaciones.add(pub)

                            // Notificar solo publicaciones nuevas y de otros conductores
                            val esNueva   = pub.timestamp > ultimoTimestampVisto
                            val esMia     = pub.autorId == auth.currentUser?.uid
                            val estaFuera = !instanciaActiva   // si la app está en segundo plano

                            if (esNueva && !esMia) {
                                // Actualizar el marcador de "visto" al más reciente
                                if (pub.timestamp > ultimoTimestampVisto) {
                                    ultimoTimestampVisto = pub.timestamp
                                }
                                // Solo mostrar notificación del sistema si está en 2do plano
                                if (estaFuera) {
                                    mostrarNotificacion(pub)
                                }
                            }
                        }
                    }

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

    // ── Mapa de alerta ───────────────────────────────────────────────────────

    /**
     * Abre MapaAlertaActivity con las coordenadas de la publicación.
     * Si la publicación no tiene ubicación, muestra un aviso.
     */
    private fun abrirMapaAlerta(publicacion: Publicacion) {
        if (!publicacion.tieneUbicacion) {
            Toast.makeText(
                this,
                "Esta publicación no tiene ubicación registrada",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val intent = Intent(this, MapaAlertaActivity::class.java).apply {
            putExtra("latitud",      publicacion.latitud)
            putExtra("longitud",     publicacion.longitud)
            putExtra("categoria",    publicacion.categoria)
            putExtra("autorNombre",  publicacion.autorNombre)
            putExtra("texto",        publicacion.texto)
            putExtra("timestamp",    publicacion.timestamp)
        }
        startActivity(intent)
    }

    // ── Notificaciones locales ───────────────────────────────────────────────

    /** Crea el canal de notificación (requerido en Android 8+). */
    private fun crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CANAL_ID,
                "Canal de Conductores",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertas y novedades del canal de conductores"
                enableVibration(true)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(canal)
        }
    }

    /**
     * Lanza una notificación del sistema para [pub].
     * Al tocarla, abre MapaAlertaActivity directamente con las coordenadas.
     */
    private fun mostrarNotificacion(pub: Publicacion) {
        // Intent que abre el mapa de la alerta al tocar la notificación
        val destino = if (pub.tieneUbicacion) {
            Intent(this, MapaAlertaActivity::class.java).apply {
                putExtra("latitud",     pub.latitud)
                putExtra("longitud",    pub.longitud)
                putExtra("categoria",   pub.categoria)
                putExtra("autorNombre", pub.autorNombre)
                putExtra("texto",       pub.texto)
                putExtra("timestamp",   pub.timestamp)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        } else {
            // Sin ubicación → abrir el canal de conductores
            Intent(this, CanalConductoresActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            pub.timestamp.toInt(),   // requestCode único por publicación
            destino,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Icono y color según categoría
        val iconoCategoria = when (pub.categoria) {
            "Alerta"   -> android.R.drawable.ic_dialog_alert
            "Trancón"  -> android.R.drawable.ic_dialog_info
            "Info"     -> android.R.drawable.ic_dialog_info
            else       -> android.R.drawable.ic_dialog_info
        }

        val ubicacionTexto = if (pub.tieneUbicacion) " • Toca para ver en el mapa 📍" else ""

        val notif = NotificationCompat.Builder(this, CANAL_ID)
            .setSmallIcon(iconoCategoria)
            .setContentTitle("${pub.categoria} · ${pub.autorNombre}")
            .setContentText(pub.texto + ubicacionTexto)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(pub.texto + ubicacionTexto))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Usar timestamp como ID para que cada publicación tenga su propia notificación
        nm.notify(pub.timestamp.toInt(), notif)
    }
}