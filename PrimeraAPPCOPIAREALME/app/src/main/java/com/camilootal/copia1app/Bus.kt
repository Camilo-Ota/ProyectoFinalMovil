package com.camilootal.copia1app

data class Bus(
    var id: String         = "",
    var placa: String      = "",
    var modelo: String     = "",
    var capacidad: Int     = 0,
    var empresaId: String  = "",
    var conductorId: String? = null,
    var conductorNombre: String? = null,
    var activo: Boolean    = true,
    var estado: String     = ESTADO_DISPONIBLE
) {
    companion object {
        const val ESTADO_DISPONIBLE   = "disponible"
        const val ESTADO_EN_RUTA      = "en_ruta"
        const val ESTADO_MANTENIMIENTO= "mantenimiento"
    }
}
