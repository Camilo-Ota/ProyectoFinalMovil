package com.camilootal.copia1app

data class Recorrido(
    var id: String = "",
    var rutaId: String = "",
    var rutaNombre: String = "",
    var usuarioId: String = "",
    var inicioTiempo: Long = 0L,
    var finTiempo: Long = 0L,
    var tiempoTotalMs: Long = 0L,
    var estado: String = "en_proceso",

    // ── Info del conductor y bus desnormalizada ───────────────────────────────
    // Se guarda al iniciar el recorrido para que el historial siempre muestre
    // quién condujo y con qué bus, aunque luego se reasigne el bus.
    var conductorNombre: String = "",
    var busId: String = "",
    var busPlaca: String = "",
    var busModelo: String = ""
)
