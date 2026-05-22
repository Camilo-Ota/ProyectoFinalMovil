package com.camilootal.copia1app

data class Ruta(
        var id: String = "",
        var nombre: String = "",
        var descripcion: String = "",
        var activa: Boolean = true,
        var radioDeteccion: Float = 30f, //distancia de recepcion al punto de marca
        var creadaEn: Long = System.currentTimeMillis()
    )
//sin layout