package com.camilootal.copia1app

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Muestra en un mapa el punto exacto donde un conductor hizo una publicación
 * en el canal de conductores.
 *
 * Extras esperados en el Intent:
 *   "latitud"     Double  – Latitud GPS de la publicación
 *   "longitud"    Double  – Longitud GPS de la publicación
 *   "categoria"   String  – "Alerta" | "Trancón" | "Info" | "General"
 *   "autorNombre" String  – Nombre del conductor
 *   "texto"       String  – Texto de la publicación
 *   "timestamp"   Long    – Momento en que se publicó
 */
class MapaAlertaActivity : AppCompatActivity(), OnMapReadyCallback {

    // ── Views ─────────────────────────────────────────────────────────────────
    private lateinit var mMap: GoogleMap
    private lateinit var btnVolver: ImageButton
    private lateinit var tvCategoriaAlerta: TextView
    private lateinit var tvAutorAlerta: TextView
    private lateinit var tvTextoAlerta: TextView
    private lateinit var tvHoraAlerta: TextView
    private lateinit var tvCoordsAlerta: TextView

    // ── Datos del Intent ──────────────────────────────────────────────────────
    private var latitud     = 0.0
    private var longitud    = 0.0
    private var categoria   = "General"
    private var autorNombre = "Conductor"
    private var texto       = ""
    private var timestamp   = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mapa_alerta)

        recibirDatos()
        enlazarVistas()
        mostrarInfoTarjeta()

        (supportFragmentManager.findFragmentById(R.id.mapFragmentAlerta) as SupportMapFragment)
            .getMapAsync(this)
    }

    // ── Datos del Intent ──────────────────────────────────────────────────────

    private fun recibirDatos() {
        latitud     = intent.getDoubleExtra("latitud", 0.0)
        longitud    = intent.getDoubleExtra("longitud", 0.0)
        categoria   = intent.getStringExtra("categoria")   ?: "General"
        autorNombre = intent.getStringExtra("autorNombre") ?: "Conductor"
        texto       = intent.getStringExtra("texto")       ?: ""
        timestamp   = intent.getLongExtra("timestamp", 0L)
    }

    // ── Vistas ────────────────────────────────────────────────────────────────

    private fun enlazarVistas() {
        btnVolver         = findViewById(R.id.btnVolverAlerta)
        tvCategoriaAlerta = findViewById(R.id.tvCategoriaAlerta)
        tvAutorAlerta     = findViewById(R.id.tvAutorAlerta)
        tvTextoAlerta     = findViewById(R.id.tvTextoAlerta)
        tvHoraAlerta      = findViewById(R.id.tvHoraAlerta)
        tvCoordsAlerta    = findViewById(R.id.tvCoordsAlerta)

        btnVolver.setOnClickListener { finish() }
    }

    private fun mostrarInfoTarjeta() {
        tvCategoriaAlerta.text = categoria
        tvAutorAlerta.text     = autorNombre
        tvTextoAlerta.text     = texto
        tvHoraAlerta.text      = formatearFecha(timestamp)
        tvCoordsAlerta.text    = "%.5f, %.5f".format(latitud, longitud)

        // Color del badge según categoría
        val colorBadge = when (categoria) {
            "Alerta"  -> getColor(R.color.warning)
            "Trancón" -> getColor(R.color.error)
            "Info"    -> getColor(R.color.colorPrimary)
            else      -> getColor(R.color.colorPrimary)
        }
        tvCategoriaAlerta.setBackgroundColor(colorBadge)
    }

    // ── Mapa ──────────────────────────────────────────────────────────────────

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Estilo oscuro para mejor visibilidad
        try {
            mMap.setMapStyle(MapStyleOptions(MAP_STYLE_ALERTA))
        } catch (_: Exception) { }

        val posAlerta = LatLng(latitud, longitud)

        // Color del marcador según categoría
        val colorMarcador = when (categoria) {
            "Alerta"  -> BitmapDescriptorFactory.HUE_RED
            "Trancón" -> BitmapDescriptorFactory.HUE_ORANGE
            "Info"    -> BitmapDescriptorFactory.HUE_AZURE
            else      -> BitmapDescriptorFactory.HUE_VIOLET
        }

        mMap.addMarker(
            MarkerOptions()
                .position(posAlerta)
                .title("$categoria · $autorNombre")
                .snippet(texto.take(60) + if (texto.length > 60) "…" else "")
                .icon(BitmapDescriptorFactory.defaultMarker(colorMarcador))
        )?.showInfoWindow()

        // Centrar con zoom de calle
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(posAlerta, 16f))

        mMap.uiSettings.apply {
            isZoomControlsEnabled  = true
            isCompassEnabled       = true
            isMapToolbarEnabled    = true
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun formatearFecha(ts: Long): String {
        if (ts == 0L) return "–"
        return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(ts))
    }

    // ── Estilo de mapa oscuro ─────────────────────────────────────────────────

    companion object {
        private val MAP_STYLE_ALERTA = """
            [{"elementType":"geometry","stylers":[{"color":"#1d2c4d"}]},
             {"elementType":"labels.text.fill","stylers":[{"color":"#8ec3b9"}]},
             {"elementType":"labels.text.stroke","stylers":[{"color":"#1a3646"}]},
             {"featureType":"administrative.country","elementType":"geometry.stroke","stylers":[{"color":"#4b6878"}]},
             {"featureType":"road","elementType":"geometry","stylers":[{"color":"#304a7d"}]},
             {"featureType":"road","elementType":"labels.text.fill","stylers":[{"color":"#98a5be"}]},
             {"featureType":"road.highway","elementType":"geometry","stylers":[{"color":"#2c6675"}]},
             {"featureType":"water","elementType":"geometry","stylers":[{"color":"#0e1626"}]},
             {"featureType":"water","elementType":"labels.text.fill","stylers":[{"color":"#4e6d70"}]}]
        """.trimIndent()
    }
}