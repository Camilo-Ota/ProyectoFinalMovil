package com.camilootal.copia1app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class RecorridoRutaActivity : AppCompatActivity() {

    private lateinit var tvRutaRecorrido: TextView
    private lateinit var tvEstadoRecorrido: TextView
    private lateinit var tvCantidadPuntosRecorrido: TextView
    private lateinit var tvPuntoSiguienteRecorrido: TextView
    private lateinit var tvTiempoInicioRecorrido: TextView
    private lateinit var tvUbicacionActualRecorrido: TextView
    private lateinit var tvProgresoRecorrido: TextView
    private lateinit var btnIniciarRecorrido: Button
    private lateinit var btnFinalizarManualRecorrido: Button
    private lateinit var btnNavegacionConductor: Button
    private lateinit var recyclerPuntosSeguimiento: RecyclerView

    private val db = FirebaseDatabase.getInstance().reference

    private var rutaId = ""
    private var rutaNombre = ""
    private var rutaRadio = 30f

    private val listaPuntos = mutableListOf<PuntoRuta>()
    private val listaSeguimiento = mutableListOf<PuntoSeguimientoUI>()

    private var recorridoId = ""
    private var recorridoIniciado = false
    private var tiempoInicioRecorrido = 0L
    private var tiempoUltimoPunto = 0L
    private var indicePuntoActual = 0
    private var esReanudacion = false

    private lateinit var puntoSeguimientoAdapter: PuntoSeguimientoAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recorrido_ruta)

        tvRutaRecorrido              = findViewById(R.id.tvRutaRecorrido)
        tvEstadoRecorrido            = findViewById(R.id.tvEstadoRecorrido)
        tvCantidadPuntosRecorrido    = findViewById(R.id.tvCantidadPuntosRecorrido)
        tvPuntoSiguienteRecorrido    = findViewById(R.id.tvPuntoSiguienteRecorrido)
        tvTiempoInicioRecorrido      = findViewById(R.id.tvTiempoInicioRecorrido)
        tvUbicacionActualRecorrido   = findViewById(R.id.tvUbicacionActualRecorrido)
        tvProgresoRecorrido          = findViewById(R.id.tvProgresoRecorrido)
        btnIniciarRecorrido          = findViewById(R.id.btnIniciarRecorrido)
        btnFinalizarManualRecorrido  = findViewById(R.id.btnFinalizarManualRecorrido)
        btnNavegacionConductor       = findViewById(R.id.btnNavegacionConductor)
        recyclerPuntosSeguimiento    = findViewById(R.id.recyclerPuntosSeguimiento)

        rutaId     = intent.getStringExtra("rutaId") ?: ""
        rutaNombre = intent.getStringExtra("rutaNombre") ?: ""
        rutaRadio  = intent.getFloatExtra("rutaRadio", 30f)

        esReanudacion = intent.getBooleanExtra("reanudar", false)
        if (esReanudacion) {
            recorridoId           = intent.getStringExtra("recorridoId") ?: ""
            tiempoInicioRecorrido = intent.getLongExtra("inicioTiempo", System.currentTimeMillis())
        }

        tvRutaRecorrido.text   = "Ruta: $rutaNombre"
        tvEstadoRecorrido.text = "Estado: cargando puntos..."

        recyclerPuntosSeguimiento.layoutManager = LinearLayoutManager(this)
        recyclerPuntosSeguimiento.isNestedScrollingEnabled = false
        puntoSeguimientoAdapter = PuntoSeguimientoAdapter(listaSeguimiento)
        recyclerPuntosSeguimiento.adapter = puntoSeguimientoAdapter

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
            .apply { setMinUpdateIntervalMillis(3000L) }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) procesarUbicacion(location)
            }
        }

        btnIniciarRecorrido.setOnClickListener { iniciarRecorrido() }
        btnFinalizarManualRecorrido.setOnClickListener { solicitarClaveFinalizacion() }

        btnNavegacionConductor.isEnabled = false
        btnNavegacionConductor.setOnClickListener { abrirNavegacionConductor() }

        cargarPuntosRuta()
    }

    private fun abrirNavegacionConductor() {
        if (!recorridoIniciado) {
            Toast.makeText(this, "Primero inicia el recorrido", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(this, NavegacionConductorActivity::class.java).apply {
            putExtra("rutaId",       rutaId)
            putExtra("rutaNombre",   rutaNombre)
            putExtra("recorridoId",  recorridoId)
            putExtra("indicePunto",  indicePuntoActual)
            putExtra("puntosNombres",   listaPuntos.map { it.nombre }.toTypedArray())
            putExtra("puntosLats",      listaPuntos.map { it.latitud }.toDoubleArray())
            putExtra("puntosLngs",      listaPuntos.map { it.longitud }.toDoubleArray())
            putExtra("puntosTipos",     listaPuntos.map { it.tipo }.toTypedArray())
            putExtra("puntosIds",       listaPuntos.map { it.id }.toTypedArray())
            putExtra("puntosOrdenes",   listaPuntos.map { it.orden }.toIntArray())
            putExtra("rutaRadio",       rutaRadio)
            putExtra("inicioTiempo",    tiempoInicioRecorrido)
        }
        startActivityForResult(intent, REQ_NAVEGACION)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_NAVEGACION && data != null) {
            val nuevoIndice = data.getIntExtra("indicePunto", indicePuntoActual)
            if (nuevoIndice > indicePuntoActual) {
                indicePuntoActual = nuevoIndice
                tvProgresoRecorrido.text = "Progreso: $indicePuntoActual/${listaPuntos.size}"
                tvPuntoSiguienteRecorrido.text = if (indicePuntoActual < listaPuntos.size)
                    "Siguiente punto: ${listaPuntos[indicePuntoActual].nombre}"
                else "Siguiente punto: recorrido completado"
                actualizarSeguimientoVisual()
            }
        }
    }

    private fun cargarPuntosRuta() {
        db.child("rutas").child(rutaId).child("puntos").get()
            .addOnSuccessListener { snapshot ->
                listaPuntos.clear()
                for (puntoSnap in snapshot.children) {
                    val punto = puntoSnap.getValue(PuntoRuta::class.java)
                    if (punto != null) {
                        if (punto.id.isEmpty()) punto.id = puntoSnap.key ?: ""
                        listaPuntos.add(punto)
                    }
                }
                listaPuntos.sortBy { it.orden }
                actualizarVistaPuntos()

                if (esReanudacion && recorridoId.isNotEmpty()) {
                    reanudarRecorrido()
                }
            }
            .addOnFailureListener { e ->
                tvEstadoRecorrido.text = "Estado: error al cargar"
                Toast.makeText(this, "Error al cargar puntos: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun actualizarVistaPuntos() {
        tvCantidadPuntosRecorrido.text = "Puntos: ${listaPuntos.size}"
        tvProgresoRecorrido.text       = "Progreso: 0/${listaPuntos.size}"
        construirSeguimientoInicial()

        if (listaPuntos.isEmpty()) {
            tvEstadoRecorrido.text            = "Estado: la ruta no tiene puntos"
            tvPuntoSiguienteRecorrido.text    = "Siguiente punto: --"
            btnIniciarRecorrido.isEnabled     = false
            puntoSeguimientoAdapter.notifyDataSetChanged()
            return
        }

        val origen = listaPuntos.count { it.tipo == "origen" }
        val fin    = listaPuntos.count { it.tipo == "fin" }

        if (listaPuntos.size < 2 || origen != 1 || fin != 1) {
            tvEstadoRecorrido.text         = "Estado: ruta incompleta"
            tvPuntoSiguienteRecorrido.text = "Siguiente punto: --"
            btnIniciarRecorrido.isEnabled  = false
            puntoSeguimientoAdapter.notifyDataSetChanged()
            Toast.makeText(this, "La ruta debe tener al menos 2 puntos, un origen y un fin", Toast.LENGTH_LONG).show()
            return
        }

        tvEstadoRecorrido.text         = "Estado: lista para iniciar"
        tvPuntoSiguienteRecorrido.text = "Siguiente punto: ${listaPuntos.first().nombre}"
        btnIniciarRecorrido.isEnabled  = true
        puntoSeguimientoAdapter.notifyDataSetChanged()
        recyclerPuntosSeguimiento.scrollToPosition(0)
    }

    private fun construirSeguimientoInicial() {
        listaSeguimiento.clear()
        for ((index, punto) in listaPuntos.withIndex()) {
            listaSeguimiento.add(
                PuntoSeguimientoUI(
                    puntoId               = punto.id,
                    nombre                = punto.nombre,
                    orden                 = punto.orden,
                    tipo                  = punto.tipo,
                    latitud               = punto.latitud,
                    longitud              = punto.longitud,
                    completado            = false,
                    esSiguiente           = index == 0,
                    tiempoDesdeAnteriorMs = 0L,
                    tiempoAcumuladoRutaMs = 0L
                )
            )
        }
    }

    private fun reanudarRecorrido() {
        recorridoIniciado                    = true
        tiempoUltimoPunto                    = System.currentTimeMillis()
        indicePuntoActual                    = 0
        btnIniciarRecorrido.isEnabled        = false
        btnFinalizarManualRecorrido.isEnabled = true
        btnNavegacionConductor.isEnabled     = true
        tvEstadoRecorrido.text               = "Estado: recorrido reanudado"
        tvTiempoInicioRecorrido.text         = "Inicio: ${formatearFechaHora(tiempoInicioRecorrido)}"
        if (listaPuntos.isNotEmpty()) {
            tvPuntoSiguienteRecorrido.text = "Siguiente punto: ${listaPuntos[0].nombre}"
        }
        construirSeguimientoInicial()
        actualizarSeguimientoVisual()
        iniciarActualizacionUbicacion()
        Toast.makeText(this, "Recorrido reanudado", Toast.LENGTH_SHORT).show()
    }

    /**
     * Inicia el recorrido. Antes de guardarlo, lee el perfil completo del conductor
     * (nombre, busAsignado, busPlaca, busModelo) para desnormalizarlo en el recorrido.
     * Así el historial siempre muestra quién manejó y con qué bus.
     */
    private fun iniciarRecorrido() {
        if (recorridoIniciado) {
            Toast.makeText(this, "El recorrido ya fue iniciado", Toast.LENGTH_SHORT).show()
            return
        }
        if (listaPuntos.isEmpty()) {
            Toast.makeText(this, "La ruta no tiene puntos", Toast.LENGTH_SHORT).show()
            return
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        // Leer perfil del conductor para obtener nombre y datos del bus asignado
        db.child("users").child(uid).get()
            .addOnSuccessListener { snap ->
                val conductor = snap.getValue(User::class.java)

                val ref = db.child("recorridos").push()
                recorridoId           = ref.key ?: ""
                tiempoInicioRecorrido = System.currentTimeMillis()
                tiempoUltimoPunto     = tiempoInicioRecorrido
                indicePuntoActual     = 0

                val recorrido = Recorrido(
                    id              = recorridoId,
                    rutaId          = rutaId,
                    rutaNombre      = rutaNombre,
                    usuarioId       = uid,
                    inicioTiempo    = tiempoInicioRecorrido,
                    finTiempo       = 0L,
                    tiempoTotalMs   = 0L,
                    estado          = "en_proceso",
                    // ── Info del conductor y bus al momento de iniciar ────────
                    conductorNombre = conductor?.name    ?: "",
                    busId           = conductor?.busAsignado ?: "",
                    busPlaca        = conductor?.busPlaca    ?: "",
                    busModelo       = conductor?.busModelo   ?: ""
                )

                ref.setValue(recorrido).addOnSuccessListener {
                    recorridoIniciado                    = true
                    btnIniciarRecorrido.isEnabled        = false
                    btnFinalizarManualRecorrido.isEnabled = true
                    btnNavegacionConductor.isEnabled     = true
                    tvEstadoRecorrido.text               = "Estado: recorrido iniciado"
                    tvTiempoInicioRecorrido.text         = "Inicio: ${formatearFechaHora(tiempoInicioRecorrido)}"
                    if (listaPuntos.isNotEmpty()) {
                        tvPuntoSiguienteRecorrido.text = "Siguiente punto: ${listaPuntos[indicePuntoActual].nombre}"
                    }
                    construirSeguimientoInicial()
                    actualizarSeguimientoVisual()
                    iniciarActualizacionUbicacion()
                    Toast.makeText(this, "Recorrido iniciado correctamente", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar datos del conductor", Toast.LENGTH_LONG).show()
            }
    }

    @SuppressLint("MissingPermission")
    private fun iniciarActualizacionUbicacion() {
        if (!tienePermisoUbicacion()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                1001
            )
            return
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    private fun detenerActualizacionUbicacion() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun tienePermisoUbicacion() =
        ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun procesarUbicacion(location: Location) {
        tvUbicacionActualRecorrido.text = "Ubicación: ${String.format("%.5f", location.latitude)}, ${String.format("%.5f", location.longitude)}"

        if (recorridoIniciado && rutaId.isNotEmpty()) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            db.child("users").child(uid).child("name").get()
                .addOnSuccessListener { snap ->
                    val nombre = snap.getValue(String::class.java) ?: "Conductor"
                    db.child("recorridos_activos").child(rutaId).child(uid).setValue(
                        mapOf(
                            "latitud"         to location.latitude,
                            "longitud"        to location.longitude,
                            "timestamp"       to System.currentTimeMillis(),
                            "activo"          to true,
                            "conductorId"     to uid,
                            "conductorNombre" to nombre
                        )
                    )
                }
        }

        if (!recorridoIniciado || indicePuntoActual >= listaPuntos.size) return

        val puntoEsperado = listaPuntos[indicePuntoActual]
        val distancia = calcularDistanciaMetros(
            location.latitude, location.longitude,
            puntoEsperado.latitud, puntoEsperado.longitud
        )

        if (distancia <= rutaRadio) {
            registrarLlegadaPunto(puntoEsperado)
            indicePuntoActual++
            tvProgresoRecorrido.text = "Progreso: $indicePuntoActual/${listaPuntos.size}"
            tvPuntoSiguienteRecorrido.text = if (indicePuntoActual < listaPuntos.size)
                "Siguiente punto: ${listaPuntos[indicePuntoActual].nombre}"
            else "✅ Recorrido completado"
            actualizarSeguimientoVisual()

            if (indicePuntoActual < listaSeguimiento.size) {
                recyclerPuntosSeguimiento.scrollToPosition(indicePuntoActual)
            }

            if (puntoEsperado.tipo == "fin") finalizarRecorridoAutomatico()
        }
    }

    private fun registrarLlegadaPunto(punto: PuntoRuta) {
        val ahora               = System.currentTimeMillis()
        val tiempoDesdeAnterior = ahora - tiempoUltimoPunto
        val tiempoAcumulado     = ahora - tiempoInicioRecorrido

        val puntoRegistrado = PuntoRegistrado(
            puntoId               = punto.id,
            nombre                = punto.nombre,
            orden                 = punto.orden,
            tiempoLlegada         = ahora,
            tiempoDesdeAnteriorMs = tiempoDesdeAnterior,
            tiempoAcumuladoRutaMs = tiempoAcumulado
        )

        db.child("recorridos").child(recorridoId)
            .child("puntosRegistrados").child(punto.id).setValue(puntoRegistrado)

        tiempoUltimoPunto = ahora

        val idx = listaSeguimiento.indexOfFirst { it.puntoId == punto.id }
        if (idx != -1) {
            listaSeguimiento[idx].completado            = true
            listaSeguimiento[idx].esSiguiente           = false
            listaSeguimiento[idx].tiempoDesdeAnteriorMs = tiempoDesdeAnterior
            listaSeguimiento[idx].tiempoAcumuladoRutaMs = tiempoAcumulado
        }
        Toast.makeText(this, "✅ Llegó a: ${punto.nombre}", Toast.LENGTH_SHORT).show()
    }

    private fun actualizarSeguimientoVisual() {
        for (i in listaSeguimiento.indices) {
            listaSeguimiento[i].esSiguiente = false
            if (i < indicePuntoActual) listaSeguimiento[i].completado = true
        }
        if (indicePuntoActual < listaSeguimiento.size) {
            listaSeguimiento[indicePuntoActual].esSiguiente = true
        }
        puntoSeguimientoAdapter.notifyDataSetChanged()
    }

    private fun finalizarRecorridoAutomatico() {
        if (!recorridoIniciado) return
        marcarConductorInactivo()
        val fin   = System.currentTimeMillis()
        val total = fin - tiempoInicioRecorrido
        db.child("recorridos").child(recorridoId).updateChildren(
            mapOf("finTiempo" to fin, "tiempoTotalMs" to total, "estado" to "finalizado_automatico")
        )
        recorridoIniciado                    = false
        detenerActualizacionUbicacion()
        tvEstadoRecorrido.text               = "✅ Recorrido completado"
        btnFinalizarManualRecorrido.isEnabled = false
        btnNavegacionConductor.isEnabled     = false
        Toast.makeText(this, "Recorrido finalizado automáticamente 🎉", Toast.LENGTH_LONG).show()
    }

    private fun finalizarRecorridoManual() {
        if (!recorridoIniciado) return
        marcarConductorInactivo()
        val fin   = System.currentTimeMillis()
        val total = fin - tiempoInicioRecorrido
        db.child("recorridos").child(recorridoId).updateChildren(
            mapOf("finTiempo" to fin, "tiempoTotalMs" to total, "estado" to "finalizado_manual")
        )
        recorridoIniciado                    = false
        detenerActualizacionUbicacion()
        tvEstadoRecorrido.text               = "Estado: recorrido finalizado"
        btnFinalizarManualRecorrido.isEnabled = false
        btnNavegacionConductor.isEnabled     = false
        Toast.makeText(this, "Recorrido finalizado", Toast.LENGTH_SHORT).show()
    }

    private fun marcarConductorInactivo() {
        if (rutaId.isNotEmpty()) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            db.child("recorridos_activos").child(rutaId).child(uid)
                .setValue(mapOf("activo" to false))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        detenerActualizacionUbicacion()
        if (recorridoIniciado) marcarConductorInactivo()
    }

    private fun calcularDistanciaMetros(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val r = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, r)
        return r[0]
    }

    private fun formatearFechaHora(tiempo: Long) =
        SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(tiempo))

    private fun solicitarClaveFinalizacion() {
        AlertDialog.Builder(this)
            .setTitle("Finalizar recorrido")
            .setMessage("¿Seguro que deseas finalizar el recorrido manualmente?")
            .setPositiveButton("Finalizar") { _, _ -> finalizarRecorridoManual() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            iniciarActualizacionUbicacion()
        }
    }

    companion object {
        const val REQ_NAVEGACION = 2001
    }
}
