package com.camilootal.copia1app

data class PuntoSeguimientoUI(
    var puntoId: String = "",
    var nombre: String = "",
    var orden: Int = 0,
    var tipo: String = "",
    var latitud: Double = 0.0,
    var longitud: Double = 0.0,
    var completado: Boolean = false,
    var esSiguiente: Boolean = false,
    var tiempoDesdeAnteriorMs: Long = 0L,
    var tiempoAcumuladoRutaMs: Long = 0L
)