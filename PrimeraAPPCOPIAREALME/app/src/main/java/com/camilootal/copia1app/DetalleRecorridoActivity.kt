package com.camilootal.copia1app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class DetalleRecorridoActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var tvDetalleRuta: TextView
    private lateinit var tvDetalleEstado: TextView
    private lateinit var tvDetalleInicio: TextView
    private lateinit var tvDetalleFin: TextView
    private lateinit var tvDetalleDuracion: TextView
    private lateinit var tvDetallePuntos: TextView
    private lateinit var btnContinuarRecorrido: Button
    private lateinit var btnFinalizarRecorrido: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var cardAcciones: View

    private val db = FirebaseDatabase.getInstance().reference
    private var recorridoId = ""
    private var rutaId = ""
    private var rutaNombre = ""
    private var estado = ""
    private var inicioTiempo = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_recorrido)

        recorridoId  = intent.getStringExtra("recorridoId") ?: ""
        rutaId       = intent.getStringExtra("rutaId") ?: ""
        rutaNombre   = intent.getStringExtra("rutaNombre") ?: ""
        estado       = intent.getStringExtra("estado") ?: ""
        inicioTiempo = intent.getLongExtra("inicioTiempo", 0L)

        tvDetalleRuta      = findViewById(R.id.tvDetalleRuta)
        tvDetalleEstado    = findViewById(R.id.tvDetalleEstado)
        tvDetalleInicio    = findViewById(R.id.tvDetalleInicio)
        tvDetalleFin       = findViewById(R.id.tvDetalleFin)
        tvDetalleDuracion  = findViewById(R.id.tvDetalleDuracion)
        tvDetallePuntos    = findViewById(R.id.tvDetallePuntos)
        btnContinuarRecorrido = findViewById(R.id.btnContinuarRecorrido)
        btnFinalizarRecorrido = findViewById(R.id.btnFinalizarRecorrido)
        progressBar        = findViewById(R.id.progressBarDetalle)
        cardAcciones       = findViewById(R.id.cardAcciones)

        tvDetalleRuta.text   = "Ruta: $rutaNombre"
        tvDetalleEstado.text = "Estado: $estado"

        if (estado == "en_proceso") {
            cardAcciones.visibility = View.VISIBLE
            btnContinuarRecorrido.setOnClickListener { continuarRecorrido() }
            btnFinalizarRecorrido.setOnClickListener { confirmarFinalizar() }
        } else {
            cardAcciones.visibility = View.GONE
        }

        (supportFragmentManager.findFragmentById(R.id.mapFragmentDetalle) as SupportMapFragment)
            .getMapAsync(this)

        cargarDatosRecorrido()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
        dibujarPuntosEnMapa()
    }

    private fun cargarDatosRecorrido() {
        progressBar.visibility = View.VISIBLE
        db.child("recorridos").child(recorridoId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    progressBar.visibility = View.GONE
                    val recorrido = snapshot.getValue(Recorrido::class.java) ?: return

                    tvDetalleInicio.text = "Inicio: ${formatearFechaHora(recorrido.inicioTiempo)}"
                    tvDetalleFin.text    = if (recorrido.finTiempo > 0L)
                        "Fin: ${formatearFechaHora(recorrido.finTiempo)}"
                    else "Fin: En curso"

                    val duracionMs = if (recorrido.tiempoTotalMs > 0L)
                        recorrido.tiempoTotalMs
                    else if (recorrido.inicioTiempo > 0L)
                        System.currentTimeMillis() - recorrido.inicioTiempo
                    else 0L
                    tvDetalleDuracion.text = "Duración: ${formatearDuracion(duracionMs)}"

                    val cantidadPuntos = snapshot.child("puntosRegistrados").childrenCount.toInt()
                    tvDetallePuntos.text = "Puntos recorridos: $cantidadPuntos"

                    val colorEstado = when (recorrido.estado) {
                        "finalizado_automatico" -> Color.parseColor("#1B5E20")
                        "finalizado_manual"     -> Color.parseColor("#E65100")
                        "en_proceso"            -> Color.parseColor("#0D47A1")
                        else                    -> Color.parseColor("#424242")
                    }
                    tvDetalleEstado.setTextColor(colorEstado)
                    tvDetalleEstado.text = "Estado: ${recorrido.estado.replace("_", " ")}"
                }
                override fun onCancelled(e: DatabaseError) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@DetalleRecorridoActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun dibujarPuntosEnMapa() {
        db.child("rutas").child(rutaId).child("puntos")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(rutaSnap: DataSnapshot) {
                    val coordenadasPorId = mutableMapOf<String, PuntoRuta>()
                    for (child in rutaSnap.children) {
                        val p = child.getValue(PuntoRuta::class.java) ?: continue
                        p.id = child.key ?: continue
                        coordenadasPorId[p.id] = p
                    }
                    dibujarPuntosRegistrados(coordenadasPorId)
                }
                override fun onCancelled(e: DatabaseError) { dibujarRutaBase() }
            })
    }

    private fun dibujarPuntosRegistrados(coordenadasPorId: Map<String, PuntoRuta>) {
        db.child("recorridos").child(recorridoId).child("puntosRegistrados")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val puntosRegistrados = mutableListOf<PuntoRegistrado>()
                    for (child in snapshot.children) {
                        val p = child.getValue(PuntoRegistrado::class.java)
                        if (p != null) puntosRegistrados.add(p)
                    }
                    puntosRegistrados.sortBy { it.orden }

                    if (puntosRegistrados.isEmpty()) {
                        dibujarRutaBase()
                        return
                    }

                    val latLngs = puntosRegistrados.mapNotNull { p ->
                        coordenadasPorId[p.puntoId]?.let { LatLng(it.latitud, it.longitud) }
                    }

                    if (latLngs.isEmpty()) {
                        dibujarRutaBase()
                        return
                    }

                    // FIX: Usar DirectionsHelper igual que en MapaRutaEnVivoActivity
                    // para trazar la ruta por calles reales en lugar de línea recta.
                    DirectionsHelper.obtenerRutaReal(latLngs, mode = "driving") { puntosRuta ->
                        runOnUiThread {
                            val paraDibujar = if (puntosRuta.isNotEmpty()) puntosRuta else latLngs

                            mMap.addPolyline(
                                PolylineOptions()
                                    .addAll(paraDibujar)
                                    .color(Color.parseColor("#1995AD"))
                                    .width(8f)
                            )

                            // Marcadores de cada punto visitado
                            puntosRegistrados.forEachIndexed { index, p ->
                                val puntoRuta = coordenadasPorId[p.puntoId] ?: return@forEachIndexed
                                val pos = LatLng(puntoRuta.latitud, puntoRuta.longitud)
                                val color = when {
                                    index == 0                          -> BitmapDescriptorFactory.HUE_GREEN
                                    index == puntosRegistrados.size - 1 -> BitmapDescriptorFactory.HUE_RED
                                    else                                -> BitmapDescriptorFactory.HUE_AZURE
                                }
                                val durTexto = if (p.tiempoDesdeAnteriorMs > 0L)
                                    "\n⏱ ${formatearDuracion(p.tiempoDesdeAnteriorMs)} desde anterior"
                                else ""

                                mMap.addMarker(
                                    MarkerOptions()
                                        .position(pos)
                                        .title("${p.nombre}$durTexto")
                                        .icon(BitmapDescriptorFactory.defaultMarker(color))
                                )
                            }

                            // Centrar cámara
                            try {
                                val bounds = LatLngBounds.builder()
                                    .also { paraDibujar.forEach(it::include) }.build()
                                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
                            } catch (e: Exception) {
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngs.first(), 14f))
                            }
                        }
                    }
                }
                override fun onCancelled(e: DatabaseError) { dibujarRutaBase() }
            })
    }

    private fun dibujarRutaBase() {
        db.child("rutas").child(rutaId).child("puntos").orderByChild("orden")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val pts = mutableListOf<LatLng>()
                    for (child in snapshot.children) {
                        val p = child.getValue(PuntoRuta::class.java) ?: continue
                        val ll = LatLng(p.latitud, p.longitud)
                        pts.add(ll)
                        mMap.addMarker(
                            MarkerOptions()
                                .position(ll)
                                .title(p.nombre)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                        )
                    }
                    if (pts.isNotEmpty()) {
                        // FIX: También usar DirectionsHelper en el fallback de ruta base
                        DirectionsHelper.obtenerRutaReal(pts, mode = "driving") { puntosRuta ->
                            runOnUiThread {
                                val paraDibujar = if (puntosRuta.isNotEmpty()) puntosRuta else pts
                                mMap.addPolyline(
                                    PolylineOptions().addAll(paraDibujar)
                                        .color(Color.parseColor("#BDBDBD")).width(6f)
                                )
                                try {
                                    val b = LatLngBounds.builder()
                                        .also { paraDibujar.forEach(it::include) }.build()
                                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(b, 100))
                                } catch (e: Exception) {
                                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pts.first(), 14f))
                                }
                            }
                        }
                    }
                }
                override fun onCancelled(e: DatabaseError) {}
            })
    }

    private fun continuarRecorrido() {
        val intent = Intent(this, RecorridoRutaActivity::class.java)
        intent.putExtra("rutaId", rutaId)
        intent.putExtra("rutaNombre", rutaNombre)
        intent.putExtra("recorridoId", recorridoId)
        intent.putExtra("reanudar", true)
        startActivity(intent)
    }

    private fun confirmarFinalizar() {
        AlertDialog.Builder(this)
            .setTitle("Finalizar recorrido")
            .setMessage("¿Seguro que deseas finalizar este recorrido manualmente?")
            .setPositiveButton("Finalizar") { _, _ -> finalizarRecorrido() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun finalizarRecorrido() {
        val fin   = System.currentTimeMillis()
        val total = if (inicioTiempo > 0L) fin - inicioTiempo else 0L

        db.child("recorridos").child(recorridoId).updateChildren(
            mapOf(
                "finTiempo"     to fin,
                "tiempoTotalMs" to total,
                "estado"        to "finalizado_manual"
            )
        ).addOnSuccessListener {
            db.child("recorridos_activos").child(rutaId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (child in snapshot.children) {
                            child.ref.child("activo").setValue(false)
                        }
                    }
                    override fun onCancelled(e: DatabaseError) {}
                })

            Toast.makeText(this, "Recorrido finalizado correctamente", Toast.LENGTH_SHORT).show()
            estado = "finalizado_manual"
            cardAcciones.visibility = View.GONE
            tvDetalleEstado.text = "Estado: finalizado manual"
            tvDetalleEstado.setTextColor(Color.parseColor("#E65100"))
            tvDetalleFin.text = "Fin: ${formatearFechaHora(fin)}"
            tvDetalleDuracion.text = "Duración: ${formatearDuracion(total)}"
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatearFechaHora(tiempo: Long): String =
        SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(tiempo))

    private fun formatearDuracion(ms: Long): String {
        val s = ms / 1000
        return String.format("%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
    }
}