package com.camilootal.copia1app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var gMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_fragment) as SupportMapFragment

        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        gMap = map

        // Punto de prueba (SABANA)
        val bogota = LatLng(4.8703, -74.0326)
        gMap.addMarker(MarkerOptions().position(bogota).title("SABANA"))
        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(bogota, 13f))

        val adportas = LatLng(4.86276, -74.0333415)
        gMap.addMarker(MarkerOptions().position(adportas).title("ADPORTAS"))

    }
}