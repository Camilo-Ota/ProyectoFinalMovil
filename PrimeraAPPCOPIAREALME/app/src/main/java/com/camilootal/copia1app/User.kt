package com.camilootal.copia1app

class User {
    var name: String? = null
    var email: String? = null
    var uid: String? = null
    var phone: String? = null
    var password: String? = null
    var role: String? = null  // "conductor" o "pasajero"
    var placa: String? = null
    var licencia: String? = null
    var rutaAsignada: String? = null
    var activo: Boolean = true

    constructor() {}

    constructor(
        name: String?,
        email: String?,
        uid: String?,
        phone: String?,
        password: String?,
        role: String?
    ) {
        this.name = name
        this.email = email
        this.uid = uid
        this.phone = phone
        this.password = password
        this.role = role


    }

    companion object {
        const val ROL_ADMINISTRADOR = "administrador"
        const val ROL_CONDUCTOR = "conductor"
        const val ROL_USUARIO = "usuario"
    }
}





    /*
    data class User(
    var name: String? = null,
    var email: String? = null,
    var uid: String? = null,
    var phone: String? = null,
    var role: String? = null,
    var plate: String? = null,
    var createdAt: Long? = null
)
     */

