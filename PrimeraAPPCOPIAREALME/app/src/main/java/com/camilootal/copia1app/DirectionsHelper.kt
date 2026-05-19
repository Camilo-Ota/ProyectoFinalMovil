package com.camilootal.copia1app

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

object DirectionsHelper {

    // ⚠️ Pon aquí tu API Key de Google Cloud (la misma que usas en el Manifest para Maps)
    private const val API_KEY = "AIzaSyCqmKwV8uax2krveCTtGqk2FC6Z6hcXpZA"

    /**
     * Obtiene la polilínea de ruta real entre una lista de puntos.
     * mode: "driving" para conductor, "walking" para pasajero
     * onResult: devuelve lista de LatLng para dibujar, o lista vacía si falla
     */
    fun obtenerRutaReal(
        puntos: List<LatLng>,
        mode: String = "driving",
        onResult: (List<LatLng>) -> Unit
    ) {
        if (puntos.size < 2) {
            onResult(emptyList())
            return
        }

        val origen = puntos.first()
        val destino = puntos.last()

        // Los puntos intermedios son "waypoints"
        val waypoints = if (puntos.size > 2) {
            val intermedios = puntos.subList(1, puntos.size - 1)
            "&waypoints=" + intermedios.joinToString("|") { "${it.latitude},${it.longitude}" }
        } else ""

        val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=${origen.latitude},${origen.longitude}" +
                "&destination=${destino.latitude},${destino.longitude}" +
                waypoints +
                "&mode=$mode" +
                "&key=$API_KEY"

        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("DirectionsHelper", "Error de red: ${e.message}")
                onResult(emptyList())
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: run {
                    onResult(emptyList())
                    return
                }

                try {
                    val json = JSONObject(body)
                    val status = json.getString("status")

                    if (status != "OK") {
                        Log.e("DirectionsHelper", "Directions API status: $status")
                        onResult(emptyList())
                        return
                    }

                    // Extraer la polilínea codificada del primer tramo
                    val routes = json.getJSONArray("routes")
                    val polylineEncoded = routes
                        .getJSONObject(0)
                        .getJSONObject("overview_polyline")
                        .getString("points")

                    val puntosDecode = decodificarPolyline(polylineEncoded)
                    onResult(puntosDecode)

                } catch (e: Exception) {
                    Log.e("DirectionsHelper", "Error parseando respuesta: ${e.message}")
                    onResult(emptyList())
                }
            }
        })
    }

    /**
     * Decodifica el formato de polilínea codificada de Google
     */
    fun decodificarPolyline(encoded: String): List<LatLng> {
        val poly = mutableListOf<LatLng>()
        var index = 0
        var lat = 0
        var lng = 0

        while (index < encoded.length) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0; result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            poly.add(LatLng(lat / 1e5, lng / 1e5))
        }
        return poly
    }
}