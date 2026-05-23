package com.camilootal.copia1app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.firebase.database.*

class MapaRutaEnVivoActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var tvNombreRutaMapa: TextView
    private lateinit var tvEstadoBusMapa: TextView
    private lateinit var tvTiempoEstimadoMapa: TextView
    private lateinit var tvTiempoWalkingMapa: TextView
    private lateinit var tvParaderoMasCercanoMapa: TextView
    private lateinit var tvInfoConductorMapa: TextView
    private lateinit var tvHintParadero: TextView
    private lateinit var cardInfoBus: View

    private var rutaId = ""
    private var rutaNombre = ""
    private val db = FirebaseDatabase.getInstance().reference
    private val puntosList = mutableListOf<PuntoRuta>()

    private val busesActivos = mutableMapOf<String, LatLng>()
    private val nombresConductores = mutableMapOf<String, String>()
    private val busMarkers = mutableMapOf<String, Marker>()
    private val paraderoMarkers = mutableMapOf<String, Marker>()

    private var busListener: ValueEventListener? = null
    private lateinit var fusedLocation: FusedLocationProviderClient
    private var locationPasajero: Location? = null
    private var locationCallback: LocationCallback? = null
    private var mapaListo = false
    private var puntosListos = false

    // Paradero actualmente seleccionado (null = el más cercano automático)
    private var paraderoSeleccionado: PuntoRuta? = null
    private var polylineWalking: Polyline? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mapa_ruta_en_vivo)

        rutaId     = intent.getStringExtra("rutaId") ?: ""
        rutaNombre = intent.getStringExtra("rutaNombre") ?: "Ruta"

        tvNombreRutaMapa         = findViewById(R.id.tvNombreRutaMapa)
        tvEstadoBusMapa          = findViewById(R.id.tvEstadoBusMapa)
        tvTiempoEstimadoMapa     = findViewById(R.id.tvTiempoEstimadoMapa)
        tvTiempoWalkingMapa      = findViewById(R.id.tvTiempoWalkingMapa)
        tvParaderoMasCercanoMapa = findViewById(R.id.tvParaderoMasCercanoMapa)
        tvInfoConductorMapa      = findViewById(R.id.tvInfoConductorMapa)
        tvHintParadero           = findViewById(R.id.tvHintParadero)
        cardInfoBus              = findViewById(R.id.cardInfoBus)

        tvNombreRutaMapa.text         = rutaNombre
        cardInfoBus.visibility        = View.VISIBLE
        tvEstadoBusMapa.text          = "⏳ Buscando buses..."
        tvTiempoEstimadoMapa.text     = "Calculando..."
        tvTiempoWalkingMapa.text      = "Calculando..."
        tvParaderoMasCercanoMapa.text = "Obteniendo tu ubicación..."
        tvInfoConductorMapa.text      = ""

        fusedLocation = LocationServices.getFusedLocationProviderClient(this)

        (supportFragmentManager.findFragmentById(R.id.mapFragmentPasajero) as SupportMapFragment)
            .getMapAsync(this)

        verificarUbicacionActivada()
        iniciarUbicacionPasajero()
    }

    override fun onResume() {
        super.onResume()
        verificarUbicacionActivada()
    }

    private fun verificarUbicacionActivada() {
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
            !lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            AlertDialog.Builder(this)
                .setTitle("Ubicación desactivada")
                .setMessage("Para ver el bus en tiempo real y calcular el tiempo de llegada necesitamos tu ubicación. ¿Deseas activarla?")
                .setCancelable(false)
                .setPositiveButton("Activar") { _, _ ->
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
                .setNegativeButton("Cancelar") { dialog, _ ->
                    dialog.dismiss()
                    Toast.makeText(this, "Sin ubicación no podemos mostrarte el tiempo estimado.", Toast.LENGTH_LONG).show()
                }
                .show()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mapaListo = true
        mMap.uiSettings.isZoomControlsEnabled = true

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
        }

        // Click en un marcador de paradero → seleccionar ese paradero
        mMap.setOnMarkerClickListener { marker ->
            val paradero = puntosList.find { it.nombre == marker.title }
            if (paradero != null) {
                seleccionarParadero(paradero, porUsuario = true)
                marker.showInfoWindow()
                true
            } else {
                false
            }
        }

        // Click en el mapa (zona vacía) → volver al paradero automático
        mMap.setOnMapClickListener {
            seleccionarParadero(null, porUsuario = false)
        }

        cargarPuntosRuta()
        escucharBus()
    }

    private fun cargarPuntosRuta() {
        db.child("rutas").child(rutaId).child("puntos").orderByChild("orden")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    puntosList.clear()
                    paraderoMarkers.clear()
                    val pts = mutableListOf<LatLng>()

                    for (child in snapshot.children) {
                        val p = child.getValue(PuntoRuta::class.java) ?: continue
                        p.id = child.key ?: ""
                        puntosList.add(p)
                        val ll = LatLng(p.latitud, p.longitud)
                        pts.add(ll)

                        val marker = mMap.addMarker(
                            MarkerOptions()
                                .position(ll)
                                .title(p.nombre)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                        )
                        if (marker != null) paraderoMarkers[p.id] = marker
                    }

                    if (pts.isNotEmpty()) {
                        DirectionsHelper.obtenerRutaReal(pts, mode = "driving") { puntosRuta ->
                            runOnUiThread {
                                val paraDibujar = if (puntosRuta.isNotEmpty()) puntosRuta else pts
                                mMap.addPolyline(
                                    PolylineOptions()
                                        .addAll(paraDibujar)
                                        .color(android.graphics.Color.parseColor("#1995AD"))
                                        .width(8f)
                                )
                                try {
                                    val b = LatLngBounds.builder().also { paraDibujar.forEach(it::include) }.build()
                                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(b, 100))
                                } catch (e: Exception) {
                                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pts.first(), 14f))
                                }
                            }
                        }
                    }

                    puntosListos = true
                    actualizarPanelCompleto()
                }
                override fun onCancelled(e: DatabaseError) {
                    Toast.makeText(this@MapaRutaEnVivoActivity, "Error al cargar ruta", Toast.LENGTH_SHORT).show()
                }
            })
    }

    /**
     * Selecciona un paradero específico (toque del usuario) o lo limpia (volver al automático).
     * porUsuario = true  → el usuario tocó un paradero → fija la selección
     * porUsuario = false → toque en mapa vacío → vuelve al más cercano automático
     */
    private fun seleccionarParadero(paradero: PuntoRuta?, porUsuario: Boolean) {
        paraderoSeleccionado = paradero

        if (!porUsuario || paradero == null) {
            // Restaurar hint y recalcular automáticamente
            tvHintParadero.text = "💡 Toca un paradero en el mapa para ver su info"
            actualizarPanelCompleto()
            return
        }

        // Actualizar UI con el paradero seleccionado manualmente
        tvHintParadero.text = "📌 ${paradero.nombre} seleccionado · Toca el mapa para volver al automático"

        val pasajero = locationPasajero
        if (pasajero == null) {
            tvParaderoMasCercanoMapa.text = "📍 ${paradero.nombre}"
            tvTiempoWalkingMapa.text      = "Esperando GPS..."
        } else {
            actualizarInfoParadero(paradero, pasajero)
        }
        mostrarRutaWalkingAParadero(paradero)
        actualizarTiempoBus(paradero)
    }

    private fun mostrarRutaWalkingAParadero(paradero: PuntoRuta) {
        val pasajero = locationPasajero ?: return
        val origen  = LatLng(pasajero.latitude, pasajero.longitude)
        val destino = LatLng(paradero.latitud, paradero.longitud)

        DirectionsHelper.obtenerRutaReal(listOf(origen, destino), mode = "walking") { puntosRuta ->
            runOnUiThread {
                polylineWalking?.remove()
                if (puntosRuta.isNotEmpty()) {
                    polylineWalking = mMap.addPolyline(
                        PolylineOptions()
                            .addAll(puntosRuta)
                            .color(android.graphics.Color.parseColor("#FF6B00"))
                            .width(12f)                           // más grueso y visible
                            .pattern(listOf(
                                Dot(),
                                Gap(16f)                          // puntos más espaciados
                            ))
                            .jointType(JointType.ROUND)
                    )
                }
            }
        }
    }

    private fun escucharBus() {
        busListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                busesActivos.clear()
                nombresConductores.clear()

                for (conductorSnap in snapshot.children) {
                    val uid    = conductorSnap.key ?: continue
                    val lat    = conductorSnap.child("latitud").getValue(Double::class.java)
                    val lng    = conductorSnap.child("longitud").getValue(Double::class.java)
                    val nombre = conductorSnap.child("conductorNombre").getValue(String::class.java) ?: "Conductor"
                    val activo = conductorSnap.child("activo").getValue(Boolean::class.java) ?: false

                    if (lat != null && lng != null && activo) {
                        val ll = LatLng(lat, lng)
                        busesActivos[uid] = ll
                        nombresConductores[uid] = nombre

                        if (busMarkers.containsKey(uid)) {
                            busMarkers[uid]?.position = ll
                        } else {
                            val bitmap      = BitmapFactory.decodeResource(resources, R.drawable.buslogo)
                            val smallMarker = Bitmap.createScaledBitmap(bitmap, 80, 80, false)
                            val marker = mMap.addMarker(
                                MarkerOptions()
                                    .position(ll)
                                    .title("Bus: $nombre")
                                    .icon(BitmapDescriptorFactory.fromBitmap(smallMarker))
                            )
                            if (marker != null) busMarkers[uid] = marker
                        }
                    } else {
                        busMarkers[uid]?.remove()
                        busMarkers.remove(uid)
                    }
                }

                val uidsActuales = snapshot.children.map { it.key }
                busMarkers.keys.toList().forEach { uid ->
                    if (!uidsActuales.contains(uid)) {
                        busMarkers[uid]?.remove()
                        busMarkers.remove(uid)
                    }
                }

                if (busesActivos.isEmpty()) {
                    tvEstadoBusMapa.text          = "🔴 Bus no disponible"
                    tvTiempoEstimadoMapa.text     = "Sin información"
                    tvInfoConductorMapa.text      = "No hay recorrido activo"
                } else {
                    tvEstadoBusMapa.text = "🟢 ${busesActivos.size} bus(es) en servicio"
                    actualizarPanelCompleto()
                }
            }
            override fun onCancelled(e: DatabaseError) {
                tvEstadoBusMapa.text = "🔴 Error al obtener buses"
            }
        }
        db.child("recorridos_activos").child(rutaId).addValueEventListener(busListener!!)
    }

    private fun actualizarPanelCompleto() {
        val pasajero = locationPasajero ?: run {
            tvParaderoMasCercanoMapa.text = "Esperando tu ubicación GPS..."
            return
        }
        if (!puntosListos || puntosList.isEmpty()) {
            tvParaderoMasCercanoMapa.text = "Cargando paraderos..."
            return
        }

        // Si el usuario seleccionó un paradero manualmente, respetar esa selección
        val paraderoObjetivo = paraderoSeleccionado ?: encontrarParaderoCercano(pasajero)
        paraderoObjetivo ?: return

        actualizarInfoParadero(paraderoObjetivo, pasajero)
        mostrarRutaWalkingAParadero(paraderoObjetivo)
        actualizarTiempoBus(paraderoObjetivo)
    }

    private fun encontrarParaderoCercano(pasajero: Location): PuntoRuta? {
        var paraderoMasCercano: PuntoRuta? = null
        var distanciaMin = Float.MAX_VALUE
        puntosList.forEach { p ->
            val res = FloatArray(1)
            Location.distanceBetween(pasajero.latitude, pasajero.longitude, p.latitud, p.longitud, res)
            if (res[0] < distanciaMin) {
                distanciaMin = res[0]
                paraderoMasCercano = p
            }
        }
        return paraderoMasCercano
    }

    /** Actualiza el nombre del paradero y el tiempo caminando */
    private fun actualizarInfoParadero(paradero: PuntoRuta, pasajero: Location) {
        val res = FloatArray(1)
        Location.distanceBetween(pasajero.latitude, pasajero.longitude, paradero.latitud, paradero.longitud, res)
        val distanciaM = res[0]

        val distTexto = if (distanciaM < 1000)
            "${distanciaM.toInt()} m"
        else
            "${"%.1f".format(distanciaM / 1000)} km"

        tvParaderoMasCercanoMapa.text = "📍 ${paradero.nombre} ($distTexto)"

        // Tiempo caminando: velocidad promedio peatonal ~83 m/min (5 km/h)
        val minutosCaminando = (distanciaM / 83f).toInt()
        tvTiempoWalkingMapa.text = when {
            minutosCaminando <= 0 -> "Ya estás aquí"
            minutosCaminando == 1 -> "~1 min caminando"
            minutosCaminando < 60 -> "~$minutosCaminando min caminando"
            else -> {
                val h = minutosCaminando / 60; val m = minutosCaminando % 60
                "~${h}h ${m}min caminando"
            }
        }
    }

    /** Actualiza el tiempo estimado de llegada del bus al paradero */
    private fun actualizarTiempoBus(paradero: PuntoRuta) {
        if (busesActivos.isEmpty()) {
            tvTiempoEstimadoMapa.text = "Bus no activo"
            tvInfoConductorMapa.text  = ""
            return
        }

        var menorTiempoMin = Int.MAX_VALUE
        var nombreBusMasCercano = ""

        busesActivos.forEach { (uid, busPos) ->
            val distanciaMetros = calcularDistanciaRuta(busPos, paradero)
            val velocidadMpMin  = 20000f / 60f  // 20 km/h en zonas urbanas
            val minutos         = (distanciaMetros / velocidadMpMin).toInt()
            if (minutos < menorTiempoMin) {
                menorTiempoMin      = minutos
                nombreBusMasCercano = nombresConductores[uid] ?: "Conductor"
            }
        }

        tvInfoConductorMapa.text = "Conductor: $nombreBusMasCercano"
        tvTiempoEstimadoMapa.text = when {
            menorTiempoMin <= 0 -> "Llegando ahora 🚌"
            menorTiempoMin == 1 -> "~1 min"
            menorTiempoMin < 60 -> "~$menorTiempoMin min"
            else -> {
                val h = menorTiempoMin / 60; val m = menorTiempoMin % 60
                "~${h}h ${m}min"
            }
        }
    }

    private fun calcularDistanciaRuta(busPos: LatLng, paraderoDestino: PuntoRuta): Float {
        if (puntosList.isEmpty()) return 0f
        var indiceBus = 0; var distMinBus = Float.MAX_VALUE
        puntosList.forEachIndexed { i, p ->
            val r = FloatArray(1)
            Location.distanceBetween(busPos.latitude, busPos.longitude, p.latitud, p.longitud, r)
            if (r[0] < distMinBus) { distMinBus = r[0]; indiceBus = i }
        }
        val indiceDestino = puntosList.indexOfFirst { it.id == paraderoDestino.id }
        if (indiceDestino == -1) {
            val r = FloatArray(1)
            Location.distanceBetween(busPos.latitude, busPos.longitude, paraderoDestino.latitud, paraderoDestino.longitud, r)
            return r[0]
        }
        if (indiceDestino <= indiceBus) return distMinBus
        var totalMetros = distMinBus
        for (i in indiceBus until indiceDestino) {
            val r = FloatArray(1)
            Location.distanceBetween(puntosList[i].latitud, puntosList[i].longitud, puntosList[i+1].latitud, puntosList[i+1].longitud, r)
            totalMetros += r[0]
        }
        return totalMetros
    }

    private fun iniciarUbicacionPasajero() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 101)
            return
        }
        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L).build()
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(r: LocationResult) {
                locationPasajero = r.lastLocation
                actualizarPanelCompleto()
            }
        }
        fusedLocation.requestLocationUpdates(req, locationCallback!!, Looper.getMainLooper())
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            iniciarUbicacionPasajero()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationCallback?.let { fusedLocation.removeLocationUpdates(it) }
        busListener?.let { db.child("recorridos_activos").child(rutaId).removeEventListener(it) }
    }
}