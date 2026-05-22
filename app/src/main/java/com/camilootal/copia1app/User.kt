package com.camilootal.copia1app

class User {
    var name: String? = null
    var email: String? = null
    var uid: String? = null
    var phone: String? = null
    var password: String? = null

    constructor(){}

    constructor(name: String?, email: String?, uid: String?, phone: String?, password: String?) {
        this.name = name
        this.email = email
        this.uid = uid
        this.phone = phone
        this.password = password
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

