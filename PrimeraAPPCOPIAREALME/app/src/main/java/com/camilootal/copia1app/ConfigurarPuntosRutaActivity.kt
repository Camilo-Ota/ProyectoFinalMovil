package com.camilootal.copia1app

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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.database.FirebaseDatabase
import com.camilootal.copia1app.PuntoRuta

class ConfigurarPuntosRutaActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var tvNombreRutaConfigurar: TextView
    private lateinit var btnRecargarPuntos: Button
    private lateinit var recyclerPuntosRuta: RecyclerView
    private lateinit var tvSinPuntos: TextView

    private val db = FirebaseDatabase.getInstance().reference

    private var rutaId: String = ""
    private var rutaNombre: String = ""
    private var rutaRadio: Float = 30f

    private val listaPuntos = mutableListOf<PuntoRuta>()
    private lateinit var puntoRutaAdapter: PuntoRutaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configurar_puntos_ruta)

        tvNombreRutaConfigurar = findViewById(R.id.tvNombreRutaConfigurar)
        btnRecargarPuntos = findViewById(R.id.btnRecargarPuntos)
        recyclerPuntosRuta = findViewById(R.id.recyclerPuntosRuta)
        tvSinPuntos = findViewById(R.id.tvSinPuntos)

        rutaId = intent.getStringExtra("rutaId") ?: ""
        rutaNombre = intent.getStringExtra("rutaNombre") ?: ""
        rutaRadio = intent.getFloatExtra("rutaRadio", 30f)

        tvNombreRutaConfigurar.text = "Ruta: $rutaNombre"

        recyclerPuntosRuta.layoutManager = LinearLayoutManager(this)
        puntoRutaAdapter = PuntoRutaAdapter(
            listaPuntos,
            onEditarClick = { punto -> mostrarDialogoEditarPunto(punto) },
            onEliminarClick = { punto -> confirmarEliminarPunto(punto) }
        )
        recyclerPuntosRuta.adapter = puntoRutaAdapter

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

        val ubicacionInicial = LatLng(4.60971, -74.08175)
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
                puntoRutaAdapter.notifyDataSetChanged()
                tvSinPuntos.visibility =
                    if (listaPuntos.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE

                dibujarPuntosEnMapa()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar puntos: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun dibujarPuntosEnMapa() {
        googleMap.clear()

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap.isMyLocationEnabled = true
        }

        if (listaPuntos.isEmpty()) return

        // Dibujar marcadores igual que antes
        for (punto in listaPuntos) {
            val posicion = LatLng(punto.latitud, punto.longitud)
            val colorMarcador = when (punto.tipo) {
                "origen" -> BitmapDescriptorFactory.HUE_GREEN
                "fin"    -> BitmapDescriptorFactory.HUE_RED
                else     -> BitmapDescriptorFactory.HUE_AZURE
            }
            googleMap.addMarker(
                MarkerOptions()
                    .position(posicion)
                    .title("${punto.orden}. ${punto.nombre}")
                    .snippet("Tipo: ${punto.tipo}")
                    .icon(BitmapDescriptorFactory.defaultMarker(colorMarcador))
            )
        }

        // ✅ Dibujar ruta real con Directions API (driving = conductor)
        val coordenadas = listaPuntos.map { LatLng(it.latitud, it.longitud) }

        DirectionsHelper.obtenerRutaReal(coordenadas, mode = "driving") { puntosRuta ->
            runOnUiThread {
                if (puntosRuta.isNotEmpty()) {
                    // Ruta real siguiendo calles
                    googleMap.addPolyline(
                        PolylineOptions()
                            .addAll(puntosRuta)
                            .color(android.graphics.Color.parseColor("#1995AD"))
                            .width(8f)
                    )
                } else {
                    // Fallback: línea recta si la API falla
                    val polylineOptions = PolylineOptions()
                    listaPuntos.forEach { polylineOptions.add(LatLng(it.latitud, it.longitud)) }
                    googleMap.addPolyline(polylineOptions)
                    Toast.makeText(this, "No se pudo cargar la ruta real, mostrando ruta directa", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val primerPunto = listaPuntos.first()
        googleMap.animateCamera(
            CameraUpdateFactory.newLatLngZoom(LatLng(primerPunto.latitud, primerPunto.longitud), 16f)
        )
    }

    private fun confirmarEliminarPunto(punto: PuntoRuta) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar punto")
            .setMessage("¿Desea eliminar el punto '${punto.nombre}'?")
            .setPositiveButton("Sí") { _, _ ->
                eliminarPunto(punto)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun eliminarPunto(punto: PuntoRuta) {
        db.child("rutas")
            .child(rutaId)
            .child("puntos")
            .child(punto.id)
            .removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Punto eliminado correctamente", Toast.LENGTH_SHORT).show()
                cargarPuntosRuta()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al eliminar punto: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun mostrarDialogoEditarPunto(punto: PuntoRuta) {
        val vista = layoutInflater.inflate(R.layout.dialog_editar_punto, null)

        val etEditarNombrePunto = vista.findViewById<EditText>(R.id.etEditarNombrePunto)
        val etEditarOrdenPunto = vista.findViewById<EditText>(R.id.etEditarOrdenPunto)
        val spEditarTipoPunto = vista.findViewById<Spinner>(R.id.spEditarTipoPunto)
        val etEditarLatitudPunto = vista.findViewById<EditText>(R.id.etEditarLatitudPunto)
        val etEditarLongitudPunto = vista.findViewById<EditText>(R.id.etEditarLongitudPunto)

        etEditarNombrePunto.setText(punto.nombre)
        etEditarOrdenPunto.setText(punto.orden.toString())
        etEditarLatitudPunto.setText(punto.latitud.toString())
        etEditarLongitudPunto.setText(punto.longitud.toString())

        val tipos = listOf("origen", "marca", "fin")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, tipos)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spEditarTipoPunto.adapter = adapter

        val indexTipo = tipos.indexOf(punto.tipo)
        if (indexTipo >= 0) {
            spEditarTipoPunto.setSelection(indexTipo)
        }

        AlertDialog.Builder(this)
            .setTitle("Editar punto")
            .setView(vista)
            .setPositiveButton("Guardar cambios", null)
            .setNegativeButton("Cancelar", null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    val btnGuardar = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    btnGuardar.setOnClickListener {
                        val nuevoNombre = etEditarNombrePunto.text.toString().trim()
                        val nuevoOrdenTexto = etEditarOrdenPunto.text.toString().trim()
                        val nuevoTipo = spEditarTipoPunto.selectedItem.toString()
                        val nuevaLatitudTexto = etEditarLatitudPunto.text.toString().trim()
                        val nuevaLongitudTexto = etEditarLongitudPunto.text.toString().trim()

                        if (nuevoNombre.isEmpty()) {
                            etEditarNombrePunto.error = "Ingrese el nombre"
                            etEditarNombrePunto.requestFocus()
                            return@setOnClickListener
                        }

                        if (nuevoOrdenTexto.isEmpty()) {
                            etEditarOrdenPunto.error = "Ingrese el orden"
                            etEditarOrdenPunto.requestFocus()
                            return@setOnClickListener
                        }

                        val nuevoOrden = nuevoOrdenTexto.toIntOrNull()
                        if (nuevoOrden == null || nuevoOrden <= 0) {
                            etEditarOrdenPunto.error = "Ingrese un orden válido"
                            etEditarOrdenPunto.requestFocus()
                            return@setOnClickListener
                        }

                        if (nuevaLatitudTexto.isEmpty()) {
                            etEditarLatitudPunto.error = "Ingrese la latitud"
                            etEditarLatitudPunto.requestFocus()
                            return@setOnClickListener
                        }

                        if (nuevaLongitudTexto.isEmpty()) {
                            etEditarLongitudPunto.error = "Ingrese la longitud"
                            etEditarLongitudPunto.requestFocus()
                            return@setOnClickListener
                        }

                        val nuevaLatitud = nuevaLatitudTexto.toDoubleOrNull()
                        if (nuevaLatitud == null || nuevaLatitud < -90 || nuevaLatitud > 90) {
                            etEditarLatitudPunto.error = "Latitud inválida"
                            etEditarLatitudPunto.requestFocus()
                            return@setOnClickListener
                        }

                        val nuevaLongitud = nuevaLongitudTexto.toDoubleOrNull()
                        if (nuevaLongitud == null || nuevaLongitud < -180 || nuevaLongitud > 180) {
                            etEditarLongitudPunto.error = "Longitud inválida"
                            etEditarLongitudPunto.requestFocus()
                            return@setOnClickListener
                        }

                        validarYActualizarPunto(
                            puntoActual = punto,
                            nuevoNombre = nuevoNombre,
                            nuevoOrden = nuevoOrden,
                            nuevoTipo = nuevoTipo,
                            nuevaLatitud = nuevaLatitud,
                            nuevaLongitud = nuevaLongitud,
                            dialog = dialog
                        )
                    }
                }
                dialog.show()
            }
    }

    private fun validarYActualizarPunto(
        puntoActual: PuntoRuta,
        nuevoNombre: String,
        nuevoOrden: Int,
        nuevoTipo: String,
        nuevaLatitud: Double,
        nuevaLongitud: Double,
        dialog: AlertDialog
    ) {
        val existeOrden = listaPuntos.any {
            it.id != puntoActual.id && it.orden == nuevoOrden
        }

        if (existeOrden) {
            Toast.makeText(this, "Ya existe otro punto con ese orden", Toast.LENGTH_LONG).show()
            return
        }

        val cantidadOrigen = listaPuntos.count {
            it.id != puntoActual.id && it.tipo == "origen"
        }

        val cantidadFin = listaPuntos.count {
            it.id != puntoActual.id && it.tipo == "fin"
        }

        if (nuevoTipo == "origen" && cantidadOrigen >= 1) {
            Toast.makeText(this, "Solo puede existir un origen por ruta", Toast.LENGTH_LONG).show()
            return
        }

        if (nuevoTipo == "fin" && cantidadFin >= 1) {
            Toast.makeText(this, "Solo puede existir un fin por ruta", Toast.LENGTH_LONG).show()
            return
        }

        actualizarPunto(
            puntoActual = puntoActual,
            nuevoNombre = nuevoNombre,
            nuevoOrden = nuevoOrden,
            nuevoTipo = nuevoTipo,
            nuevaLatitud = nuevaLatitud,
            nuevaLongitud = nuevaLongitud,
            dialog = dialog
        )
    }

    private fun actualizarPunto(
        puntoActual: PuntoRuta,
        nuevoNombre: String,
        nuevoOrden: Int,
        nuevoTipo: String,
        nuevaLatitud: Double,
        nuevaLongitud: Double,
        dialog: AlertDialog
    ) {
        val puntoActualizado = mapOf(
            "nombre" to nuevoNombre,
            "orden" to nuevoOrden,
            "tipo" to nuevoTipo,
            "latitud" to nuevaLatitud,
            "longitud" to nuevaLongitud
        )

        db.child("rutas")
            .child(rutaId)
            .child("puntos")
            .child(puntoActual.id)
            .updateChildren(puntoActualizado)
            .addOnSuccessListener {
                Toast.makeText(this, "Punto actualizado correctamente", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                cargarPuntosRuta()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al actualizar: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}