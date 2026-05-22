package com.camilootal.copia1app

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.database.FirebaseDatabase
import com.camilootal.copia1app.PuntoRegistrado
import com.camilootal.copia1app.PuntoRuta
import com.camilootal.copia1app.PuntoSeguimientoUI
import com.camilootal.copia1app.Recorrido
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    private lateinit var recyclerPuntosSeguimiento: RecyclerView

    private val db = FirebaseDatabase.getInstance().reference

    private var rutaId: String = ""
    private var rutaNombre: String = ""
    private var rutaRadio: Float = 30f

    private val listaPuntos = mutableListOf<PuntoRuta>()
    private val listaSeguimiento = mutableListOf<PuntoSeguimientoUI>()

    private var recorridoId: String = ""
    private var recorridoIniciado = false
    private var tiempoInicioRecorrido: Long = 0L
    private var tiempoUltimoPunto: Long = 0L
    private var indicePuntoActual = 0
    private var esRecuperacion = false

    private lateinit var puntoSeguimientoAdapter: PuntoSeguimientoAdapter

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    //oncreate
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recorrido_ruta)

        tvRutaRecorrido = findViewById(R.id.tvRutaRecorrido)
        tvEstadoRecorrido = findViewById(R.id.tvEstadoRecorrido)
        tvCantidadPuntosRecorrido = findViewById(R.id.tvCantidadPuntosRecorrido)
        tvPuntoSiguienteRecorrido = findViewById(R.id.tvPuntoSiguienteRecorrido)
        tvTiempoInicioRecorrido = findViewById(R.id.tvTiempoInicioRecorrido)
        tvUbicacionActualRecorrido = findViewById(R.id.tvUbicacionActualRecorrido)
        tvProgresoRecorrido = findViewById(R.id.tvProgresoRecorrido)
        btnIniciarRecorrido = findViewById(R.id.btnIniciarRecorrido)
        btnFinalizarManualRecorrido = findViewById(R.id.btnFinalizarManualRecorrido)
        recyclerPuntosSeguimiento = findViewById(R.id.recyclerPuntosSeguimiento)

        rutaId = intent.getStringExtra("rutaId") ?: ""
        rutaNombre = intent.getStringExtra("rutaNombre") ?: ""
        rutaRadio = intent.getFloatExtra("rutaRadio", 30f)

        recorridoId = intent.getStringExtra("recorridoId") ?: ""
        esRecuperacion = recorridoId.isNotEmpty()

        tvRutaRecorrido.text = "Ruta: $rutaNombre"
        tvEstadoRecorrido.text = "Estado: cargando puntos..."

        recyclerPuntosSeguimiento.layoutManager = LinearLayoutManager(this)
        puntoSeguimientoAdapter = PuntoSeguimientoAdapter(listaSeguimiento)
        recyclerPuntosSeguimiento.adapter = puntoSeguimientoAdapter

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L
        ).apply {
            setMinUpdateIntervalMillis(3000L)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    procesarUbicacion(location)
                }
            }
        }

        btnIniciarRecorrido.setOnClickListener {
            iniciarRecorrido()
        }

        btnFinalizarManualRecorrido.setOnClickListener {
            solicitarClaveFinalizacion()
        }

        cargarPuntosRuta()
    }

    private fun cargarPuntosRuta() {
        db.child("rutas")
            .child(rutaId)
            .child("puntos")
            .get()
            .addOnSuccessListener { snapshot ->
                listaPuntos.clear()

                for (puntoSnap in snapshot.children) {
                    val punto = puntoSnap.getValue(PuntoRuta::class.java)
                    if (punto != null) {
                        listaPuntos.add(punto)
                    }
                }

                listaPuntos.sortBy { it.orden }
                actualizarVistaPuntos()

                if (esRecuperacion){
                    recuperarRecorrido()
                }
            }
            .addOnFailureListener { e ->
                tvEstadoRecorrido.text = "Estado: error al cargar"
                Toast.makeText(this, "Error al cargar puntos: ${e.message}", Toast.LENGTH_LONG)
                    .show()
            }
    }

    private fun actualizarVistaPuntos() {
        tvCantidadPuntosRecorrido.text = "Puntos: ${listaPuntos.size}"
        tvProgresoRecorrido.text = "Progreso: 0/${listaPuntos.size}"

        construirSeguimientoInicial()

        if (listaPuntos.isEmpty()) {
            tvEstadoRecorrido.text = "Estado: la ruta no tiene puntos"
            tvPuntoSiguienteRecorrido.text = "Siguiente punto: --"
            btnIniciarRecorrido.isEnabled = false
            puntoSeguimientoAdapter.notifyDataSetChanged()
            return
        }

        val origen = listaPuntos.count { it.tipo == "origen" }
        val fin = listaPuntos.count { it.tipo == "fin" }

        if (listaPuntos.size < 2 || origen != 1 || fin != 1) {
            tvEstadoRecorrido.text = "Estado: ruta incompleta"
            tvPuntoSiguienteRecorrido.text = "Siguiente punto: --"
            btnIniciarRecorrido.isEnabled = false
            puntoSeguimientoAdapter.notifyDataSetChanged()

            Toast.makeText(
                this,
                "La ruta debe tener al menos 2 puntos, un origen y un fin",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        tvEstadoRecorrido.text = "Estado: lista para iniciar"
        tvPuntoSiguienteRecorrido.text = "Siguiente punto: ${listaPuntos.first().nombre}"
        btnIniciarRecorrido.isEnabled = true
        puntoSeguimientoAdapter.notifyDataSetChanged()
    }

    private fun construirSeguimientoInicial() {
        listaSeguimiento.clear()

        for ((index, punto) in listaPuntos.withIndex()) {
            listaSeguimiento.add(
                PuntoSeguimientoUI(
                    puntoId = punto.id,
                    nombre = punto.nombre,
                    orden = punto.orden,
                    tipo = punto.tipo,
                    latitud = punto.latitud,
                    longitud = punto.longitud,
                    completado = false,
                    esSiguiente = index == 0,
                    tiempoDesdeAnteriorMs = 0L,
                    tiempoAcumuladoRutaMs = 0L
                )
            )
        }
    }

    private fun iniciarRecorrido() {

        if (recorridoIniciado) {
            Toast.makeText(
                this,
                "El recorrido ya fue iniciado",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (listaPuntos.isEmpty()) {
            Toast.makeText(
                this,
                "La ruta no tiene puntos",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        db.child("recorridos")
            .get()
            .addOnSuccessListener { snapshot ->

                var recorridoExistenteId = ""

                for (recorridoSnap in snapshot.children) {

                    val recorrido =
                        recorridoSnap.getValue(Recorrido::class.java)

                    if (
                        recorrido != null &&
                        recorrido.rutaId == rutaId &&
                        recorrido.estado == "en_proceso"
                    ) {

                        recorridoExistenteId =
                            recorridoSnap.key ?: ""

                        break
                    }
                }

                if (recorridoExistenteId.isNotEmpty()) {

                    Toast.makeText(
                        this,
                        "Ya existe un recorrido en proceso",
                        Toast.LENGTH_LONG
                    ).show()

                    recorridoId = recorridoExistenteId

                    esRecuperacion = true

                    recuperarRecorrido()

                } else {

                    crearNuevoRecorrido()
                }
            }
    }

    private fun crearNuevoRecorrido() {

        val ref = db.child("recorridos").push()

        recorridoId = ref.key ?: ""

        tiempoInicioRecorrido = System.currentTimeMillis()
        tiempoUltimoPunto = tiempoInicioRecorrido
        indicePuntoActual = 0

        val recorrido = Recorrido(
            id = recorridoId,
            rutaId = rutaId,
            rutaNombre = rutaNombre,
            usuarioId = "",
            inicioTiempo = tiempoInicioRecorrido,
            finTiempo = 0L,
            tiempoTotalMs = 0L,
            estado = "en_proceso"
        )

        ref.setValue(recorrido)
            .addOnSuccessListener {

                recorridoIniciado = true

                btnIniciarRecorrido.text = "Recorrido recuperado"
                btnIniciarRecorrido.isEnabled = false

                btnFinalizarManualRecorrido.isEnabled = true

                tvEstadoRecorrido.text =
                    "Estado: recorrido iniciado"

                tvTiempoInicioRecorrido.text =
                    "Inicio: ${formatearFechaHora(tiempoInicioRecorrido)}"

                if (listaPuntos.isNotEmpty()) {

                    tvPuntoSiguienteRecorrido.text =
                        "Siguiente punto: ${listaPuntos[indicePuntoActual].nombre}"
                }

                construirSeguimientoInicial()

                actualizarSeguimientoVisual()

                iniciarActualizacionUbicacion()

                Toast.makeText(
                    this,
                    "Recorrido iniciado correctamente",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { e ->

                Toast.makeText(
                    this,
                    "Error al iniciar recorrido: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    @SuppressLint("MissingPermission")
    private fun iniciarActualizacionUbicacion() {
        if (!tienePermisoUbicacion()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                1001
            )
            return
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun detenerActualizacionUbicacion() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun tienePermisoUbicacion(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun procesarUbicacion(location: Location) {
        tvUbicacionActualRecorrido.text =
            "Ubicación actual: ${location.latitude}, ${location.longitude}"

        if (!recorridoIniciado) return
        if (indicePuntoActual >= listaPuntos.size) return

        val puntoEsperado = listaPuntos[indicePuntoActual]
        val distancia = calcularDistanciaMetros(
            location.latitude,
            location.longitude,
            puntoEsperado.latitud,
            puntoEsperado.longitud
        )

        if (distancia <= rutaRadio) {
            registrarLlegadaPunto(puntoEsperado)
            indicePuntoActual++

            tvProgresoRecorrido.text = "Progreso: $indicePuntoActual/${listaPuntos.size}"

            if (indicePuntoActual < listaPuntos.size) {
                tvPuntoSiguienteRecorrido.text =
                    "Siguiente punto: ${listaPuntos[indicePuntoActual].nombre}"
            } else {
                tvPuntoSiguienteRecorrido.text = "Siguiente punto: recorrido completado"
            }

            actualizarSeguimientoVisual()

            if (puntoEsperado.tipo == "fin") {
                finalizarRecorridoAutomatico()
            }
        }
    }

    private fun registrarLlegadaPunto(punto: PuntoRuta) {
        val ahora = System.currentTimeMillis()

        val tiempoDesdeAnterior = if (punto.tipo == "origen" && indicePuntoActual == 0) {
            0L
        } else {
            ahora - tiempoUltimoPunto
        }

        val tiempoAcumuladoRuta = ahora - tiempoInicioRecorrido

        val puntoRegistrado = PuntoRegistrado(
            puntoId = punto.id,
            nombre = punto.nombre,
            orden = punto.orden,
            tiempoLlegada = ahora,
            tiempoDesdeAnteriorMs = tiempoDesdeAnterior,
            tiempoAcumuladoRutaMs = tiempoAcumuladoRuta
        )

        db.child("recorridos")
            .child(recorridoId)
            .child("puntosRegistrados")
            .child(punto.id)
            .setValue(puntoRegistrado)

        tiempoUltimoPunto = ahora

        val indexSeguimiento = listaSeguimiento.indexOfFirst { it.puntoId == punto.id }
        if (indexSeguimiento != -1) {
            listaSeguimiento[indexSeguimiento].completado = true
            listaSeguimiento[indexSeguimiento].esSiguiente = false
            listaSeguimiento[indexSeguimiento].tiempoDesdeAnteriorMs = tiempoDesdeAnterior
            listaSeguimiento[indexSeguimiento].tiempoAcumuladoRutaMs = tiempoAcumuladoRuta
        }

        Toast.makeText(
            this,
            "Llegó al punto: ${punto.nombre}",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun actualizarSeguimientoVisual() {
        for (i in listaSeguimiento.indices) {
            listaSeguimiento[i].esSiguiente = false
        }

        if (indicePuntoActual < listaSeguimiento.size) {
            if (!listaSeguimiento[indicePuntoActual].completado) {
                listaSeguimiento[indicePuntoActual].esSiguiente = true
            }
        }

        puntoSeguimientoAdapter.notifyDataSetChanged()
    }

    private fun finalizarRecorridoAutomatico() {
        if (!recorridoIniciado) return

        val fin = System.currentTimeMillis()
        val total = fin - tiempoInicioRecorrido

        val datosActualizar = mapOf<String, Any>(
            "finTiempo" to fin,
            "tiempoTotalMs" to total,
            "estado" to "finalizado_automatico"
        )

        db.child("recorridos")
            .child(recorridoId)
            .updateChildren(datosActualizar)
            .addOnSuccessListener {
                recorridoIniciado = false
                detenerActualizacionUbicacion()
                btnIniciarRecorrido.isEnabled = false
                btnFinalizarManualRecorrido.isEnabled = false
                tvEstadoRecorrido.text = "Estado: recorrido finalizado automáticamente"
                actualizarSeguimientoVisual()

                Toast.makeText(
                    this,
                    "Ruta finalizada. Tiempo total: ${formatearDuracion(total)}",
                    Toast.LENGTH_LONG
                ).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Error al finalizar recorrido: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun solicitarClaveFinalizacion() {
        val editText = EditText(this)
        editText.hint = "Ingrese la clave"

        AlertDialog.Builder(this)
            .setTitle("Finalización manual")
            .setMessage("Digite la clave para finalizar manualmente")
            .setView(editText)
            .setPositiveButton("Validar") { _, _ ->
                val clave = editText.text.toString().trim()
                if (clave == ConstantesRutas.CLAVE_FINALIZACION_MANUAL) {
                    finalizarRecorridoManual()
                } else {
                    Toast.makeText(this, "Clave incorrecta", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun finalizarRecorridoManual() {
        if (!recorridoIniciado || recorridoId.isEmpty()) {
            Toast.makeText(this, "No hay recorrido iniciado", Toast.LENGTH_SHORT).show()
            return
        }

        val fin = System.currentTimeMillis()
        val total = fin - tiempoInicioRecorrido

        val datosActualizar = mapOf<String, Any>(
            "finTiempo" to fin,
            "tiempoTotalMs" to total,
            "estado" to "finalizado_manual"
        )

        db.child("recorridos")
            .child(recorridoId)
            .updateChildren(datosActualizar)
            .addOnSuccessListener {
                recorridoIniciado = false
                detenerActualizacionUbicacion()
                btnIniciarRecorrido.isEnabled = false
                btnFinalizarManualRecorrido.isEnabled = false
                tvEstadoRecorrido.text = "Estado: recorrido finalizado manualmente"
                tvPuntoSiguienteRecorrido.text = "Siguiente punto: recorrido cerrado"
                actualizarSeguimientoVisual()

                Toast.makeText(
                    this,
                    "Recorrido finalizado. Tiempo total: ${formatearDuracion(total)}",
                    Toast.LENGTH_LONG
                ).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Error al finalizar recorrido: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun calcularDistanciaMetros(
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double
    ): Float {
        val resultados = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, resultados)
        return resultados[0]
    }

    private fun formatearFechaHora(tiempo: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(tiempo))
    }

    private fun formatearDuracion(ms: Long): String {
        val segundos = ms / 1000
        val horas = segundos / 3600
        val minutos = (segundos % 3600) / 60
        val seg = segundos % 60
        return String.format("%02d:%02d:%02d", horas, minutos, seg)
    }

    override fun onDestroy() {
        super.onDestroy()
        detenerActualizacionUbicacion()
    }

    private fun recuperarRecorrido() {
        db.child("recorridos")
            .child(recorridoId)
            .get()
            .addOnSuccessListener { snapshot ->

                val recorrido =
                    snapshot.getValue(Recorrido::class.java)

                if (recorrido != null) {

                    recorridoIniciado = true

                    tiempoInicioRecorrido =
                        recorrido.inicioTiempo

                    tiempoUltimoPunto =
                        System.currentTimeMillis()

                    btnIniciarRecorrido.text =
                        "Recorrido recuperado"

                    btnIniciarRecorrido.isEnabled = false

                    btnFinalizarManualRecorrido.isEnabled = true

                    tvEstadoRecorrido.text =
                        "Estado: recorrido recuperado"

                    cargarPuntosRegistrados()

                    tvTiempoInicioRecorrido.text =
                        "Inicio: ${formatearFechaHora(recorrido.inicioTiempo)}"

                    iniciarActualizacionUbicacion()

                    Toast.makeText(
                        this,
                        "Recorrido recuperado",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun cargarPuntosRegistrados(){
        db.child("recorridos")
            .child(recorridoId)
            .child("puntosRegistrados")
            .get()
            .addOnSuccessListener { snapshot ->

                var puntosCompletados = 0

                for (puntoSnap in snapshot.children){
                    val puntoId = puntoSnap.key ?: continue

                    val index =
                        listaSeguimiento.indexOfFirst {
                            it.puntoId == puntoId
                        }

                    if (index != -1){
                        listaSeguimiento[index].completado = true
                        listaSeguimiento[index].esSiguiente = false

                        puntosCompletados++
                    }
                }

                indicePuntoActual = puntosCompletados

                if(indicePuntoActual<listaPuntos.size){
                    listaSeguimiento[indicePuntoActual]
                        .esSiguiente = true

                    tvPuntoSiguienteRecorrido.text =
                        "Siguiente punto: ${
                            listaPuntos[indicePuntoActual].nombre
                        }"
                } else{
                    tvPuntoSiguienteRecorrido.text =
                        "Siguiente punto: recorrido completado"
                }

                tvProgresoRecorrido.text =
                    "Progreso: $puntosCompletados/${listaPuntos.size}"

                puntoSeguimientoAdapter.notifyDataSetChanged()
            }
    }
}