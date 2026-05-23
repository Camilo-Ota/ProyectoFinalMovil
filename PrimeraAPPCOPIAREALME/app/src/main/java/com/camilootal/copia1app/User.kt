package com.camilootal.copia1app

class User {
    var name: String? = null
    var email: String? = null
    var uid: String? = null
    var phone: String? = null
    var password: String? = null
    var role: String? = null
    var placa: String? = null
    var licencia: String? = null
    var rutaAsignada: String? = null
    var activo: Boolean = true

    // Vincula al conductor con la empresa que lo creó (null si es conductor global)
    var empresaId: String? = null

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

        // Nuevo: cuenta raíz de una empresa de transporte
        const val ROL_EMPRESA       = "empresa_transporte"
    }
}
