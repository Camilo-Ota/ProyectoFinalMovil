package com.camilootal.copia1app

data class Recorrido(
        var id: String = "",
        var rutaId: String = "",
        var rutaNombre: String = "",
        var usuarioId: String = "",
        var inicioTiempo: Long = 0L,
        var finTiempo: Long = 0L,
        var tiempoTotalMs: Long = 0L,
        var estado: String = "en_proceso"
    )
//sin layout