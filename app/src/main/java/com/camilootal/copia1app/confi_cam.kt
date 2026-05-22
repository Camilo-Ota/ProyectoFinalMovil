package com.camilootal.copia1app

class confi_cam {
/*
package com.whgg.miprimerapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.database.FirebaseDatabase
import com.whgg.miprimerapp.PuntoRuta

class ConfigurarPuntosRutaActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var tvNombreRutaConfigurar: TextView
    private lateinit var btnRecargarPuntos: Button

    private val db = FirebaseDatabase.getInstance().reference

    private var rutaId: String = ""
    private var rutaNombre: String = ""
    private var rutaRadio: Float = 30f

    private val listaPuntos = mutableListOf<PuntoRuta>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configurar_puntos_ruta)

        tvNombreRutaConfigurar = findViewById(R.id.tvNombreRutaConfigurar)
        btnRecargarPuntos = findViewById(R.id.btnRecargarPuntos)

        rutaId = intent.getStringExtra("rutaId") ?: ""
        rutaNombre = intent.getStringExtra("rutaNombre") ?: ""
        rutaRadio = intent.getFloatExtra("rutaRadio", 30f)

        tvNombreRutaConfigurar.text = "Ruta: $rutaNombre"

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapConfigurarPuntos) as SupportMapFragment
        mapFragment.getMapAsync(this)

        btnRecargarPuntos.setOnClickListener {
            cargarPuntosRuta()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isCompassEnabled = true
        googleMap.uiSettings.isMapToolbarEnabled = true

        habilitarMiUbicacion()

        val ubicacionInicial = LatLng(4.60971, -74.08175) // Bogotá como punto inicial
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ubicacionInicial, 12f))

        googleMap.setOnMapClickListener { latLng ->
            mostrarDialogoAgregarPunto(latLng)
        }

        cargarPuntosRuta()
    }

    private fun habilitarMiUbicacion() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap.isMyLocationEnabled = true
        }
    }

    private fun mostrarDialogoAgregarPunto(latLng: LatLng) {
        val vista = layoutInflater.inflate(R.layout.dialog_agregar_punto, null)

        val etNombrePunto = vista.findViewById<EditText>(R.id.etNombrePunto)
        val etOrdenPunto = vista.findViewById<EditText>(R.id.etOrdenPunto)
        val spTipoPunto = vista.findViewById<Spinner>(R.id.spTipoPunto)
        val tvCoordenadasPunto = vista.findViewById<TextView>(R.id.tvCoordenadasPunto)

        tvCoordenadasPunto.text = "Latitud: ${latLng.latitude}\nLongitud: ${latLng.longitude}"

        val tipos = listOf("origen", "marca", "fin")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, tipos)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spTipoPunto.adapter = adapter

        AlertDialog.Builder(this)
            .setTitle("Agregar punto a la ruta")
            .setView(vista)
            .setPositiveButton("Guardar", null)
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    val botonGuardar = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    botonGuardar.setOnClickListener {
                        val nombre = etNombrePunto.text.toString().trim()
                        val ordenTexto = etOrdenPunto.text.toString().trim()
                        val tipo = spTipoPunto.selectedItem.toString()

                        if (nombre.isEmpty()) {
                            etNombrePunto.error = "Ingrese el nombre del punto"
                            etNombrePunto.requestFocus()
                            return@setOnClickListener
                        }

                        if (ordenTexto.isEmpty()) {
                            etOrdenPunto.error = "Ingrese el orden"
                            etOrdenPunto.requestFocus()
                            return@setOnClickListener
                        }

                        val orden = ordenTexto.toIntOrNull()
                        if (orden == null || orden <= 0) {
                            etOrdenPunto.error = "Ingrese un orden válido"
                            etOrdenPunto.requestFocus()
                            return@setOnClickListener
                        }

                        validarYGuardarPunto(
                            nombre = nombre,
                            latitud = latLng.latitude,
                            longitud = latLng.longitude,
                            orden = orden,
                            tipo = tipo,
                            dialog = dialog
                        )
                    }
                }
                dialog.show()
            }
    }

    private fun validarYGuardarPunto(
        nombre: String,
        latitud: Double,
        longitud: Double,
        orden: Int,
        tipo: String,
        dialog: AlertDialog
    ) {
        val existeOrden = listaPuntos.any { it.orden == orden }

        if (existeOrden) {
            Toast.makeText(
                this,
                "Ya existe un punto con ese orden en esta ruta",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val cantidadOrigen = listaPuntos.count { it.tipo == "origen" }
        val cantidadFin = listaPuntos.count { it.tipo == "fin" }

        if (tipo == "origen" && cantidadOrigen >= 1) {
            Toast.makeText(this, "Solo puede existir un punto origen por ruta", Toast.LENGTH_LONG).show()
            return
        }

        if (tipo == "fin" && cantidadFin >= 1) {
            Toast.makeText(this, "Solo puede existir un punto final por ruta", Toast.LENGTH_LONG).show()
            return
        }

        guardarPuntoRuta(nombre, latitud, longitud, orden, tipo, dialog)
    }

    private fun guardarPuntoRuta(
        nombre: String,
        latitud: Double,
        longitud: Double,
        orden: Int,
        tipo: String,
        dialog: AlertDialog
    ) {
        val puntoRef = db.child("rutas").child(rutaId).child("puntos").push()
        val puntoId = puntoRef.key ?: ""

        val punto = PuntoRuta(
            id = puntoId,
            nombre = nombre,
            latitud = latitud,
            longitud = longitud,
            orden = orden,
            tipo = tipo
        )

        puntoRef.setValue(punto)
            .addOnSuccessListener {
                Toast.makeText(this, "Punto guardado correctamente", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                cargarPuntosRuta()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al guardar punto: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun cargarPuntosRuta() {
        db.child("rutas").child(rutaId).child("puntos")
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
                dibujarPuntosEnMapa()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar puntos: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun dibujarPuntosEnMapa() {
        googleMap.clear()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap.isMyLocationEnabled = true
        }

        if (listaPuntos.isEmpty()) {
            return
        }

        val polylineOptions = PolylineOptions()

        for (punto in listaPuntos) {
            val posicion = LatLng(punto.latitud, punto.longitud)
            polylineOptions.add(posicion)

            val colorMarcador = when (punto.tipo) {
                "origen" -> BitmapDescriptorFactory.HUE_GREEN
                "fin" -> BitmapDescriptorFactory.HUE_RED
                else -> BitmapDescriptorFactory.HUE_AZURE
            }

            googleMap.addMarker(
                MarkerOptions()
                    .position(posicion)
                    .title("${punto.orden}. ${punto.nombre}")
                    .snippet("Tipo: ${punto.tipo}")
                    .icon(BitmapDescriptorFactory.defaultMarker(colorMarcador))
            )
        }

        googleMap.addPolyline(polylineOptions)

        val primerPunto = listaPuntos.first()
        googleMap.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(primerPunto.latitud, primerPunto.longitud),
                16f
            )
        )
    }
}
 */

}