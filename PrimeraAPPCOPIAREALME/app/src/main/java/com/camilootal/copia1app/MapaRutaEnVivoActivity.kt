package com.camilootal.copia1app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
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
    private lateinit var tvParaderoMasCercanoMapa: TextView
    private lateinit var tvInfoConductorMapa: TextView
    private lateinit var cardInfoBus: View

    private var rutaId = ""
    private var rutaNombre = ""
    private val db = FirebaseDatabase.getInstance().reference
    private val puntosList = mutableListOf<PuntoRuta>()

    // Un mapa por conductor en lugar de un solo marcador
    private val busesActivos = mutableMapOf<String, LatLng>()       // uid → posición
    private val nombresConductores = mutableMapOf<String, String>()  // uid → nombre
    private val busMarkers = mutableMapOf<String, Marker>()          // uid → marcador

    private var busListener: ValueEventListener? = null
    private lateinit var fusedLocation: FusedLocationProviderClient
    private var locationPasajero: Location? = null
    private var locationCallback: LocationCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mapa_ruta_en_vivo)

        rutaId     = intent.getStringExtra("rutaId") ?: ""
        rutaNombre = intent.getStringExtra("rutaNombre") ?: "Ruta"

        tvNombreRutaMapa         = findViewById(R.id.tvNombreRutaMapa)
        tvEstadoBusMapa          = findViewById(R.id.tvEstadoBusMapa)
        tvTiempoEstimadoMapa     = findViewById(R.id.tvTiempoEstimadoMapa)
        tvParaderoMasCercanoMapa = findViewById(R.id.tvParaderoMasCercanoMapa)
        tvInfoConductorMapa      = findViewById(R.id.tvInfoConductorMapa)
        cardInfoBus              = findViewById(R.id.cardInfoBus)

        tvNombreRutaMapa.text = rutaNombre
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
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gpsActivo = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val redActiva = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!gpsActivo && !redActiva) {
            AlertDialog.Builder(this)
                .setTitle("Ubicación desactivada")
                .setMessage("Para ver el bus en tiempo real y calcular el tiempo de llegada a tu paradero, necesitamos acceder a tu ubicación. ¿Deseas activarla ahora?")
                .setCancelable(false)
                .setPositiveButton("Activar") { _, _ ->
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
                .setNegativeButton("Cancelar") { dialog, _ ->
                    dialog.dismiss()
                    Toast.makeText(
                        this,
                        "Sin ubicación no podremos mostrarte el paradero ni el tiempo estimado.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                .show()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
        }
        cargarPuntosRuta()
        escucharBus()
    }
    private fun cargarPuntosRuta() {
        db.child("rutas").child(rutaId).child("puntos").orderByChild("orden")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    puntosList.clear()
                    val pts = mutableListOf<LatLng>()
                    for (child in snapshot.children) {
                        val p = child.getValue(PuntoRuta::class.java) ?: continue
                        p.id = child.key ?: ""
                        puntosList.add(p)
                        val ll = LatLng(p.latitud, p.longitud)
                        pts.add(ll)
                        // Marcadores de paraderos
                        mMap.addMarker(
                            MarkerOptions()
                                .position(ll)
                                .title(p.nombre)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                        )
                    }

                    if (pts.isNotEmpty()) {
                        // ✅ Ruta del BUS en modo driving (calles reales)
                        DirectionsHelper.obtenerRutaReal(pts, mode = "driving") { puntosRuta ->
                            runOnUiThread {
                                val puntosParaDibujar = if (puntosRuta.isNotEmpty()) puntosRuta else pts
                                mMap.addPolyline(
                                    PolylineOptions()
                                        .addAll(puntosParaDibujar)
                                        .color(android.graphics.Color.parseColor("#1995AD"))
                                        .width(8f)
                                )
                                try {
                                    val b = LatLngBounds.builder().also { puntosParaDibujar.forEach(it::include) }.build()
                                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(b, 100))
                                } catch (e: Exception) {
                                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pts.first(), 14f))
                                }
                            }
                        }
                    }
                    actualizarPanelCompleto()
                }
                override fun onCancelled(e: DatabaseError) {
                    Toast.makeText(this@MapaRutaEnVivoActivity, "Error al cargar ruta", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // ✅ NUEVO: dibuja en el mapa cómo caminar desde el pasajero hasta su paradero más cercano
    private var polylineWalking: com.google.android.gms.maps.model.Polyline? = null

    private fun mostrarRutaWalkingAParadero(paradero: PuntoRuta) {
        val pasajero = locationPasajero ?: return

        val origen = LatLng(pasajero.latitude, pasajero.longitude)
        val destino = LatLng(paradero.latitud, paradero.longitud)

        DirectionsHelper.obtenerRutaReal(listOf(origen, destino), mode = "walking") { puntosRuta ->
            runOnUiThread {
                // Borrar la ruta walking anterior si existe
                polylineWalking?.remove()

                if (puntosRuta.isNotEmpty()) {
                    polylineWalking = mMap.addPolyline(
                        PolylineOptions()
                            .addAll(puntosRuta)
                            .color(android.graphics.Color.parseColor("#FF6B35")) // naranja = a pie
                            .width(6f)
                            .pattern(listOf(
                                com.google.android.gms.maps.model.Dot(),
                                com.google.android.gms.maps.model.Gap(10f)
                            )) // línea punteada para distinguirla del bus
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

                        // Actualizar o crear marcador para este conductor
                        if (busMarkers.containsKey(uid)) {
                            busMarkers[uid]?.position = ll
                        } else {

                            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.buslogo)
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
                        // Este conductor finalizó — quitar su marcador
                        busMarkers[uid]?.remove()
                        busMarkers.remove(uid)
                    }
                }

                // Limpiar marcadores de conductores que desaparecieron del snapshot
                val uidsActuales = snapshot.children.map { it.key }
                busMarkers.keys.toList().forEach { uid ->
                    if (!uidsActuales.contains(uid)) {
                        busMarkers[uid]?.remove()
                        busMarkers.remove(uid)
                    }
                }

                cardInfoBus.visibility = View.VISIBLE

                if (busesActivos.isEmpty()) {
                    tvEstadoBusMapa.text          = "🔴 Bus no disponible"
                    tvTiempoEstimadoMapa.text     = "Sin información"
                    tvInfoConductorMapa.text      = "No hay recorrido activo"
                    tvParaderoMasCercanoMapa.text = "Paradero: —"
                } else {
                    tvEstadoBusMapa.text = "🟢 ${busesActivos.size} bus(es) en servicio"
                    actualizarPanelCompleto()
                }
            }
            override fun onCancelled(e: DatabaseError) {}
        }
        //  Escuchar todos los conductores hijos del nodo de la ruta
        db.child("recorridos_activos").child(rutaId).addValueEventListener(busListener!!)
    }

    private fun actualizarPanelCompleto() {
        val pasajero = locationPasajero ?: return
        if (puntosList.isEmpty()) return

        // 1. Paradero más cercano al pasajero
        var paraderoMasCercano: PuntoRuta? = null
        var distanciaMinParadero = Float.MAX_VALUE

        puntosList.forEach { p ->
            val res = FloatArray(1)
            Location.distanceBetween(
                pasajero.latitude, pasajero.longitude,
                p.latitud, p.longitud,
                res
            )
            if (res[0] < distanciaMinParadero) {
                distanciaMinParadero = res[0]
                paraderoMasCercano = p
            }
        }

        // Dentro de actualizarPanelCompleto(), reemplaza el bloque paraderoMasCercano?.let { ... }
        paraderoMasCercano?.let { paradero ->
            val distTexto = if (distanciaMinParadero < 1000)
                "${distanciaMinParadero.toInt()} m"
            else
                "${"%.1f".format(distanciaMinParadero / 1000)} km"
            tvParaderoMasCercanoMapa.text = "Tu paradero: ${paradero.nombre} ($distTexto)"

            // ✅ Mostrar ruta a pie hasta el paradero
            mostrarRutaWalkingAParadero(paradero)
        }

        // 2. De todos los buses activos, elegir el que llegará PRIMERO al paradero
        val paradero = paraderoMasCercano ?: return

        if (busesActivos.isEmpty()) {
            tvTiempoEstimadoMapa.text = "Bus no activo"
            return
        }

        var menorTiempoMin = Int.MAX_VALUE
        var nombreBusMasCercano = ""

        busesActivos.forEach { (uid, busPos) ->
            val distanciaMetros = calcularDistanciaRuta(busPos, paradero)
            val velocidadMpMin = 20000f / 60f  // 20 km/h en m/min
            val minutos = (distanciaMetros / velocidadMpMin).toInt()

            if (minutos < menorTiempoMin) {
                menorTiempoMin = minutos
                nombreBusMasCercano = nombresConductores[uid] ?: "Conductor"
            }
        }

        tvInfoConductorMapa.text = "Conductor más cercano: $nombreBusMasCercano"

        tvTiempoEstimadoMapa.text = when {
            menorTiempoMin <= 0  -> "Llegando ahora"
            menorTiempoMin == 1  -> "~1 min"
            menorTiempoMin < 60  -> "~$menorTiempoMin min"
            else -> {
                val h = menorTiempoMin / 60
                val m = menorTiempoMin % 60
                "~${h}h ${m}min"
            }
        }
    }

    private fun calcularDistanciaRuta(busPos: LatLng, paraderoDestino: PuntoRuta): Float {
        if (puntosList.isEmpty()) return 0f

        // Punto de la ruta más cercano al bus
        var indiceBus = 0
        var distMinBus = Float.MAX_VALUE
        puntosList.forEachIndexed { i, p ->
            val r = FloatArray(1)
            Location.distanceBetween(busPos.latitude, busPos.longitude, p.latitud, p.longitud, r)
            if (r[0] < distMinBus) { distMinBus = r[0]; indiceBus = i }
        }

        val indiceDestino = puntosList.indexOfFirst { it.id == paraderoDestino.id }
        if (indiceDestino == -1) {
            // Fallback: distancia recta bus → paradero
            val r = FloatArray(1)
            Location.distanceBetween(
                busPos.latitude, busPos.longitude,
                paraderoDestino.latitud, paraderoDestino.longitud, r
            )
            return r[0]
        }

        if (indiceDestino <= indiceBus) {
            // El bus ya pasó ese paradero
            return distMinBus
        }

        // Suma de segmentos desde el bus hasta el paradero destino
        var totalMetros = distMinBus
        for (i in indiceBus until indiceDestino) {
            val r = FloatArray(1)
            Location.distanceBetween(
                puntosList[i].latitud, puntosList[i].longitud,
                puntosList[i + 1].latitud, puntosList[i + 1].longitud,
                r
            )
            totalMetros += r[0]
        }
        return totalMetros
    }

    private fun iniciarUbicacionPasajero() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                101
            )
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

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            iniciarUbicacionPasajero()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationCallback?.let { fusedLocation.removeLocationUpdates(it) }
        busListener?.let { db.child("recorridos_activos").child(rutaId).removeEventListener(it) }
    }
}