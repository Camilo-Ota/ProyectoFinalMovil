package com.camilootal.copia1app

class User {
    var name: String?      = null
    var email: String?     = null
    var uid: String?       = null
    var phone: String?     = null
    var password: String?  = null
    var role: String?      = null
    var placa: String?     = null
    var licencia: String?  = null
    var rutaAsignada: String? = null
    var activo: Boolean    = true
    var empresaId: String? = null

    // ── Campos nuevos para asignación de bus ──────────────────────────────────
    var busAsignado: String? = null   // ID del bus en Firebase
    var busPlaca: String?    = null   // Placa del bus (desnormalizado para mostrar rápido)
    var busModelo: String?   = null   // Modelo del bus (desnormalizado)

    constructor() {}

    constructor(
        name: String?,
        email: String?,
        uid: String?,
        phone: String?,
        password: String?,
        role: String?,
        empresaId: String? = null
    ) {
        this.name      = name
        this.email     = email
        this.uid       = uid
        this.phone     = phone
        this.password  = password
        this.role      = role
        this.empresaId = empresaId
    }

    companion object {
        const val ROL_ADMINISTRADOR = "administrador"
        const val ROL_CONDUCTOR     = "conductor"
        const val ROL_USUARIO       = "usuario"
        const val ROL_EMPRESA       = "empresa_transporte"
    }
}
