package com.camilootal.copia1app

data class PuntoRegistrado(
    var puntoId: String = "",
    var nombre: String = "",
    var orden: Int = 0,
    var tiempoLlegada: Long = 0L,
    var tiempoDesdeAnteriorMs: Long = 0L,
    var tiempoAcumuladoRutaMs: Long = 0L
)
//sin layout