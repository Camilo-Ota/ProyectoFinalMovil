package com.camilootal.copia1app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class NavegacionConductorActivity : AppCompatActivity(), OnMapReadyCallback {

    // ── Views ─────────────────────────────────────────────────────────────────
    private lateinit var mMap: GoogleMap
    private lateinit var tvNombreRutaNav: TextView
    private lateinit var tvParaderoActualNav: TextView
    private lateinit var tvSiguienteParaderoNav: TextView
    private lateinit var tvDistanciaNav: TextView
    private lateinit var tvTiempoEstNav: TextView
    private lateinit var tvProgresoNav: TextView
    private lateinit var tvTipoParaderoNav: TextView
    private lateinit var layoutLlegandoNav: LinearLayout
    private lateinit var btnCentrarMapa: ImageButton
    private lateinit var btnVerRutaCompleta: ImageButton

    // ── Datos ─────────────────────────────────────────────────────────────────
    private var rutaId       = ""
    private var rutaNombre   = ""
    private var recorridoId  = ""
    private var rutaRadio    = 30f
    private var inicioTiempo = 0L
    private val listaPuntos  = mutableListOf<PuntoRuta>()
    private var indiceActual = 0

    // ── Mapa ──────────────────────────────────────────────────────────────────
    private var polylineRutaCompleta: Polyline? = null   // ruta completa (gris claro)
    private var polylineNavActiva: Polyline?    = null   // tramo actual (azul brillante)
    private var markerConductor: Marker?        = null
    private var markerDestino: Marker?          = null
    private val marcadoresParaderos             = mutableListOf<Marker>()
    private var siguiendoConductor              = true   // false = el usuario movió el mapa

    // ── Ubicación ─────────────────────────────────────────────────────────────
    private lateinit var fusedLocation: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var ultimaUbicacion: Location?      = null
    private var ultimaBearing                   = 0f

    private val db = FirebaseDatabase.getInstance().reference

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_navegacion_conductor)

        bindViews()
        recibirDatos()
        configurarBotones()
        iniciarUbicacion()

        (supportFragmentManager.findFragmentById(R.id.mapNavConductor) as SupportMapFragment)
            .getMapAsync(this)
    }

    private fun bindViews() {
        tvNombreRutaNav        = findViewById(R.id.tvNombreRutaNav)
        tvParaderoActualNav    = findViewById(R.id.tvParaderoActualNav)
        tvSiguienteParaderoNav = findViewById(R.id.tvSiguienteParaderoNav)
        tvDistanciaNav         = findViewById(R.id.tvDistanciaNav)
        tvTiempoEstNav         = findViewById(R.id.tvTiempoEstNav)
        tvProgresoNav          = findViewById(R.id.tvProgresoNav)
        tvTipoParaderoNav      = findViewById(R.id.tvTipoParaderoNav)
        layoutLlegandoNav      = findViewById(R.id.layoutLlegandoNav)
        btnCentrarMapa         = findViewById(R.id.btnCentrarMapa)
        btnVerRutaCompleta     = findViewById(R.id.btnVerRutaCompleta)
    }

    private fun recibirDatos() {
        rutaId       = intent.getStringExtra("rutaId")      ?: ""
        rutaNombre   = intent.getStringExtra("rutaNombre")  ?: "Ruta"
        recorridoId  = intent.getStringExtra("recorridoId") ?: ""
        indiceActual = intent.getIntExtra("indicePunto", 0)
        rutaRadio    = intent.getFloatExtra("rutaRadio", 30f)
        inicioTiempo = intent.getLongExtra("inicioTiempo", System.currentTimeMillis())

        val nombres = intent.getStringArrayExtra("puntosNombres")  ?: emptyArray()
        val lats    = intent.getDoubleArrayExtra("puntosLats")     ?: DoubleArray(0)
        val lngs    = intent.getDoubleArrayExtra("puntosLngs")     ?: DoubleArray(0)
        val tipos   = intent.getStringArrayExtra("puntosTipos")    ?: emptyArray()
        val ids     = intent.getStringArrayExtra("puntosIds")      ?: emptyArray()
        val ordenes = intent.getIntArrayExtra("puntosOrdenes")     ?: IntArray(0)

        listaPuntos.clear()
        for (i in nombres.indices) {
            listaPuntos.add(PuntoRuta(
                id       = ids.getOrElse(i) { "" },
                nombre   = nombres[i],
                latitud  = lats.getOrElse(i) { 0.0 },
                longitud = lngs.getOrElse(i) { 0.0 },
                tipo     = tipos.getOrElse(i) { "marca" },
                orden    = ordenes.getOrElse(i) { i }
            ))
        }

        tvNombreRutaNav.text = rutaNombre
        actualizarPanelInfo()
    }

    private fun configurarBotones() {
        // Volver a seguir al conductor
        btnCentrarMapa.setOnClickListener {
            siguiendoConductor = true
            ultimaUbicacion?.let { centrarEnConductor(it) }
        }
        // Ver toda la ruta
        btnVerRutaCompleta.setOnClickListener {
            siguiendoConductor = false
            verRutaCompleta()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mapa
    // ─────────────────────────────────────────────────────────────────────────

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Estilo oscuro tipo navegación
        try {
            mMap.setMapStyle(MapStyleOptions(MAP_STYLE_NOCHE))
        } catch (_: Exception) { }

        mMap.uiSettings.isZoomControlsEnabled  = false
        mMap.uiSettings.isCompassEnabled       = false
        mMap.uiSettings.isMapToolbarEnabled    = false
        mMap.uiSettings.isMyLocationButtonEnabled = false

        // Detectar cuando el usuario mueve el mapa manualmente
        mMap.setOnCameraMoveStartedListener { reason ->
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                siguiendoConductor = false
                btnCentrarMapa.visibility = View.VISIBLE
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = false
        }

        dibujarRutaCompleta()
    }

    /** Dibuja TODA la ruta en gris como fondo de referencia */
    private fun dibujarRutaCompleta() {
        if (listaPuntos.size < 2) return
        val pts = listaPuntos.map { LatLng(it.latitud, it.longitud) }

        DirectionsHelper.obtenerRutaReal(pts, mode = "driving") { puntosRuta ->
            runOnUiThread {
                val toDraw = if (puntosRuta.isNotEmpty()) puntosRuta else pts

                // Ruta completa en gris translúcido (referencia)
                polylineRutaCompleta?.remove()
                polylineRutaCompleta = mMap.addPolyline(
                    PolylineOptions()
                        .addAll(toDraw)
                        .color(Color.argb(120, 158, 158, 158))
                        .width(8f)
                        .jointType(JointType.ROUND)
                        .startCap(RoundCap())
                        .endCap(RoundCap())
                )

                // Marcadores de paraderos (completados, pendientes, destino)
                dibujarMarcadoresParaderos()

                // Centrar mapa en la ruta al inicio
                try {
                    val b = LatLngBounds.builder().also { toDraw.forEach(it::include) }.build()
                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(b, 100))
                } catch (_: Exception) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pts.first(), 14f))
                }
            }
        }
    }

    private fun dibujarMarcadoresParaderos() {
        marcadoresParaderos.forEach { it.remove() }
        marcadoresParaderos.clear()

        listaPuntos.forEachIndexed { i, p ->
            val ll = LatLng(p.latitud, p.longitud)
            val icon = when {
                i < indiceActual  -> crearIconoParadero(p.nombre, ESTADO_COMPLETADO, i + 1)
                i == indiceActual -> crearIconoParadero(p.nombre, ESTADO_DESTINO,    i + 1)
                else              -> crearIconoParadero(p.nombre, ESTADO_PENDIENTE,  i + 1)
            }
            val m = mMap.addMarker(MarkerOptions().position(ll).icon(icon).anchor(0.5f, 1f))
            if (m != null) marcadoresParaderos.add(m)
        }
    }

    /** Dibuja el tramo activo: ubicación del conductor → próximo paradero */
    private fun dibujarTramoActivo(origen: LatLng, destino: LatLng) {
        DirectionsHelper.obtenerRutaReal(listOf(origen, destino), mode = "driving") { pts ->
            runOnUiThread {
                polylineNavActiva?.remove()
                val toDraw = if (pts.isNotEmpty()) pts else listOf(origen, destino)

                // Sombra (más ancha, más oscura)
                mMap.addPolyline(
                    PolylineOptions()
                        .addAll(toDraw)
                        .color(Color.argb(80, 0, 60, 160))
                        .width(22f)
                        .jointType(JointType.ROUND)
                        .startCap(RoundCap())
                        .endCap(RoundCap())
                )

                // Línea principal azul brillante
                polylineNavActiva = mMap.addPolyline(
                    PolylineOptions()
                        .addAll(toDraw)
                        .color(Color.parseColor("#1E88E5"))
                        .width(16f)
                        .jointType(JointType.ROUND)
                        .startCap(RoundCap())
                        .endCap(RoundCap())
                        .zIndex(10f)
                )

                // Actualizar marcador destino
                markerDestino?.remove()
                markerDestino = mMap.addMarker(
                    MarkerOptions()
                        .position(destino)
                        .icon(crearIconoDestino())
                        .anchor(0.5f, 1f)
                        .zIndex(5f)
                )
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Marcadores personalizados (dibujados con Canvas)
    // ─────────────────────────────────────────────────────────────────────────

    private fun crearIconoConductor(bearing: Float): BitmapDescriptor {
        val size = 120
        val bmp  = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c    = Canvas(bmp)

        // Círculo exterior blanco
        val paintOuter = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        c.drawCircle(size / 2f, size / 2f, size / 2f - 4, paintOuter)

        // Círculo azul principal
        val paintInner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E88E5")
            style = Paint.Style.FILL
        }
        c.drawCircle(size / 2f, size / 2f, size / 2f - 12, paintInner)

        // Flecha de dirección
        val paintArrow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val path = Path().apply {
            moveTo(size / 2f, 18f)
            lineTo(size / 2f + 16f, size / 2f + 18f)
            lineTo(size / 2f, size / 2f + 6f)
            lineTo(size / 2f - 16f, size / 2f + 18f)
            close()
        }
        c.save()
        c.rotate(bearing, size / 2f, size / 2f)
        c.drawPath(path, paintArrow)
        c.restore()

        return BitmapDescriptorFactory.fromBitmap(bmp)
    }

    private fun crearIconoDestino(): BitmapDescriptor {
        val w = 160; val h = 200
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c   = Canvas(bmp)

        // Pin rojo
        val paintPin = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E53935")
            style = Paint.Style.FILL
        }
        val radio = w / 2f - 8
        c.drawCircle(w / 2f, radio + 8, radio, paintPin)

        // Cola del pin
        val path = Path().apply {
            moveTo(w / 2f - 20f, radio * 1.5f + 8)
            lineTo(w / 2f + 20f, radio * 1.5f + 8)
            lineTo(w / 2f, h.toFloat() - 4)
            close()
        }
        c.drawPath(path, paintPin)

        // Punto blanco interior
        val paintWhite = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; style = Paint.Style.FILL
        }
        c.drawCircle(w / 2f, radio + 8, radio * 0.38f, paintWhite)

        return BitmapDescriptorFactory.fromBitmap(bmp)
    }

    private fun crearIconoParadero(nombre: String, estado: Int, numero: Int): BitmapDescriptor {
        val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize  = 28f
            typeface  = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        val textW = paintText.measureText(numero.toString()).toInt()
        val w = maxOf(64, textW + 24)
        val h = 80
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c   = Canvas(bmp)

        val (bgColor, textColor, alpha) = when (estado) {
            ESTADO_COMPLETADO -> Triple(Color.parseColor("#4CAF50"), Color.WHITE, 180)
            ESTADO_DESTINO    -> Triple(Color.parseColor("#E53935"), Color.WHITE, 255)
            else              -> Triple(Color.parseColor("#455A64"), Color.parseColor("#B0BEC5"), 160)
        }

        // Fondo circular
        val paintBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColor; style = Paint.Style.FILL; this.alpha = alpha
        }
        val radio = w / 2f - 4
        c.drawCircle(w / 2f, radio + 4, radio, paintBg)

        // Número
        paintText.color = textColor
        paintText.alpha = alpha
        val textY = radio + 4 + (paintText.descent() - paintText.ascent()) / 2 - paintText.descent()
        c.drawText(numero.toString(), w / 2f, textY, paintText)

        // Cola del pin solo para el destino activo
        if (estado == ESTADO_DESTINO) {
            val paintPin = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = bgColor; style = Paint.Style.FILL
            }
            val path = Path().apply {
                moveTo(w / 2f - 10f, radio * 2 + 4)
                lineTo(w / 2f + 10f, radio * 2 + 4)
                lineTo(w / 2f, h.toFloat())
                close()
            }
            c.drawPath(path, paintPin)
        }

        return BitmapDescriptorFactory.fromBitmap(bmp)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Cámara
    // ─────────────────────────────────────────────────────────────────────────

    private fun centrarEnConductor(location: Location) {
        val pos = LatLng(location.latitude, location.longitude)
        val bearing = if (location.hasBearing()) location.bearing else ultimaBearing
        mMap.animateCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.builder()
                    .target(pos)
                    .zoom(18f)
                    .bearing(bearing)
                    .tilt(60f)          // inclinación tipo Waze
                    .build()
            ), 800, null
        )
        btnCentrarMapa.visibility = View.GONE
    }

    private fun verRutaCompleta() {
        if (listaPuntos.isEmpty()) return
        val pts = listaPuntos.map { LatLng(it.latitud, it.longitud) }
        try {
            val b = LatLngBounds.builder().also { pts.forEach(it::include) }.build()
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(b, 80))
        } catch (_: Exception) { }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Ubicación
    // ─────────────────────────────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    private fun iniciarUbicacion() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1002)
            return
        }
        fusedLocation = LocationServices.getFusedLocationProviderClient(this)
        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
            .setMinUpdateIntervalMillis(1000L)
            .build()
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(r: LocationResult) {
                r.lastLocation?.let { procesarUbicacion(it) }
            }
        }
        fusedLocation.requestLocationUpdates(req, locationCallback!!, Looper.getMainLooper())
    }

    private fun procesarUbicacion(location: Location) {
        ultimaUbicacion = location
        if (location.hasBearing()) ultimaBearing = location.bearing

        val myPos = LatLng(location.latitude, location.longitude)

        // Marcador conductor con flecha de dirección
        val iconConductor = crearIconoConductor(ultimaBearing)
        if (markerConductor == null) {
            markerConductor = mMap.addMarker(
                MarkerOptions()
                    .position(myPos)
                    .icon(iconConductor)
                    .anchor(0.5f, 0.5f)
                    .flat(true)
                    .zIndex(10f)
            )
        } else {
            markerConductor?.position = myPos
            markerConductor?.setIcon(iconConductor)
        }

        // Seguir al conductor si el usuario no movió el mapa
        if (siguiendoConductor) centrarEnConductor(location)

        // Publicar posición en Firebase
        publicarPosicion(location)

        if (indiceActual >= listaPuntos.size) return

        val paradero = listaPuntos[indiceActual]
        val dist = FloatArray(1)
        Location.distanceBetween(location.latitude, location.longitude,
            paradero.latitud, paradero.longitud, dist)
        val distM = dist[0]

        actualizarDistanciaYTiempo(distM)
        dibujarTramoActivo(myPos, LatLng(paradero.latitud, paradero.longitud))

        // Banner verde cuando está llegando
        layoutLlegandoNav.visibility = if (distM <= rutaRadio * 2.5f) View.VISIBLE else View.GONE

        // Llegada detectada
        if (distM <= rutaRadio) {
            registrarLlegada(paradero)
            indiceActual++
            dibujarMarcadoresParaderos()   // re-pintar con el nuevo estado
            actualizarPanelInfo()
            if (paradero.tipo == "fin") finalizarRecorrido()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Panel de info
    // ─────────────────────────────────────────────────────────────────────────

    private fun actualizarPanelInfo() {
        val total = listaPuntos.size
        tvProgresoNav.text = "$indiceActual / $total paradas"

        if (indiceActual >= total) {
            tvParaderoActualNav.text    = "Recorrido completado ✅"
            tvSiguienteParaderoNav.text = ""
            tvTipoParaderoNav.text      = ""
            tvDistanciaNav.text         = "--"
            tvTiempoEstNav.text         = "--"
            layoutLlegandoNav.visibility= View.GONE
            return
        }

        val actual    = listaPuntos[indiceActual]
        val siguiente = listaPuntos.getOrNull(indiceActual + 1)

        tvParaderoActualNav.text    = actual.nombre
        tvTipoParaderoNav.text      = when (actual.tipo) {
            "origen" -> "🟢 Punto de origen"
            "fin"    -> "🔴 Punto final"
            else     -> "📍 Paradero ${indiceActual + 1}"
        }
        tvSiguienteParaderoNav.text = if (siguiente != null)
            "Luego: ${siguiente.nombre}" else "Última parada"
    }

    private fun actualizarDistanciaYTiempo(distM: Float) {
        tvDistanciaNav.text = if (distM < 1000)
            "${distM.toInt()} m" else "${"%.1f".format(distM / 1000)} km"

        val minutos = (distM / 333f).toInt()   // ~20 km/h urbano
        tvTiempoEstNav.text = when {
            minutos <= 0 -> "¡Llegando!"
            minutos == 1 -> "~1 min"
            minutos < 60 -> "~$minutos min"
            else         -> "~${minutos / 60}h ${minutos % 60}min"
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Firebase
    // ─────────────────────────────────────────────────────────────────────────

    private fun registrarLlegada(punto: PuntoRuta) {
        val ahora = System.currentTimeMillis()
        val puntoReg = PuntoRegistrado(
            puntoId               = punto.id,
            nombre                = punto.nombre,
            orden                 = punto.orden,
            tiempoLlegada         = ahora,
            tiempoDesdeAnteriorMs = 0L,
            tiempoAcumuladoRutaMs = ahora - inicioTiempo
        )
        if (recorridoId.isNotEmpty()) {
            db.child("recorridos").child(recorridoId)
                .child("puntosRegistrados").child(punto.id).setValue(puntoReg)
        }
        Toast.makeText(this, "✅ ${punto.nombre}", Toast.LENGTH_SHORT).show()
    }

    private fun publicarPosicion(location: Location) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.child("users").child(uid).child("name").get().addOnSuccessListener { snap ->
            val nombre = snap.getValue(String::class.java) ?: "Conductor"
            db.child("recorridos_activos").child(rutaId).child(uid).setValue(mapOf(
                "latitud"         to location.latitude,
                "longitud"        to location.longitude,
                "timestamp"       to System.currentTimeMillis(),
                "activo"          to true,
                "conductorId"     to uid,
                "conductorNombre" to nombre
            ))
        }
    }

    private fun finalizarRecorrido() {
        locationCallback?.let { fusedLocation.removeLocationUpdates(it) }
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        db.child("recorridos_activos").child(rutaId).child(uid)
            .setValue(mapOf("activo" to false))
        Toast.makeText(this, "🎉 ¡Recorrido finalizado!", Toast.LENGTH_LONG).show()
        devolverResultado()
    }

    private fun devolverResultado() {
        setResult(RESULT_OK, Intent().apply { putExtra("indicePunto", indiceActual) })
        finish()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Misc
    // ─────────────────────────────────────────────────────────────────────────

    override fun onBackPressed() { devolverResultado() }

    override fun onDestroy() {
        super.onDestroy()
        locationCallback?.let { fusedLocation.removeLocationUpdates(it) }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1002 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            iniciarUbicacion()
    }

    companion object {
        private const val ESTADO_COMPLETADO = 0
        private const val ESTADO_DESTINO    = 1
        private const val ESTADO_PENDIENTE  = 2

        // Estilo JSON oscuro tipo Google Maps noche
        private val MAP_STYLE_NOCHE = """
        [{"elementType":"geometry","stylers":[{"color":"#1a2535"}]},
         {"elementType":"labels.text.fill","stylers":[{"color":"#8ec3b9"}]},
         {"elementType":"labels.text.stroke","stylers":[{"color":"#1a3646"}]},
         {"featureType":"road","elementType":"geometry","stylers":[{"color":"#2c3e50"}]},
         {"featureType":"road","elementType":"geometry.stroke","stylers":[{"color":"#212a37"}]},
         {"featureType":"road","elementType":"labels.text.fill","stylers":[{"color":"#9ca5b3"}]},
         {"featureType":"road.highway","elementType":"geometry","stylers":[{"color":"#3a4f6b"}]},
         {"featureType":"road.highway","elementType":"geometry.stroke","stylers":[{"color":"#1f2835"}]},
         {"featureType":"road.highway","elementType":"labels.text.fill","stylers":[{"color":"#f3d19c"}]},
         {"featureType":"transit","elementType":"geometry","stylers":[{"color":"#2f3948"}]},
         {"featureType":"water","elementType":"geometry","stylers":[{"color":"#17263c"}]},
         {"featureType":"water","elementType":"labels.text.fill","stylers":[{"color":"#515c6d"}]},
         {"featureType":"poi","stylers":[{"visibility":"off"}]},
         {"featureType":"poi.park","elementType":"geometry","stylers":[{"color":"#1d3a2f"}]},
         {"featureType":"poi.park","stylers":[{"visibility":"on"}]}]
        """.trimIndent()
    }
}