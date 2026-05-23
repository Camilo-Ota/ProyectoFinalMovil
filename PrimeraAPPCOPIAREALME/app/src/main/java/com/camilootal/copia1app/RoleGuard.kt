package com.camilootal.copia1app

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

object RoleGuard {

    fun verificar(
        context: Context,
        vararg rolesPermitidos: String,
        onPermitido: () -> Unit
    ) {
        val auth = FirebaseAuth.getInstance()
        val uid  = auth.currentUser?.uid

        if (uid == null) {
            redirigirLogin(context, cerrarSesion = false)
            return
        }

        FirebaseDatabase.getInstance().reference
            .child("users").child(uid).child("role")
            .get()
            .addOnSuccessListener { snapshot ->
                val rol = snapshot.getValue(String::class.java)
                if (rol != null && rolesPermitidos.contains(rol)) {
                    onPermitido()
                } else {
                    Toast.makeText(
                        context,
                        "No tienes permiso para acceder a esta sección.",
                        Toast.LENGTH_LONG
                    ).show()
                    redirigirLogin(context, cerrarSesion = true)
                }
            }
            .addOnFailureListener { e ->
                val esErrorPermisos = e.message?.contains("Permission denied", ignoreCase = true) == true
                if (esErrorPermisos) {
                    Toast.makeText(context, "Acceso denegado. Verifica tu conexión.", Toast.LENGTH_SHORT).show()
                    redirigirLogin(context, cerrarSesion = true)
                } else {
                    Toast.makeText(context, "Error de red. Intenta de nuevo.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun redirigirLogin(context: Context, cerrarSesion: Boolean) {
        if (cerrarSesion) FirebaseAuth.getInstance().signOut()
        val intent = Intent(context, LogIn::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
    }
}
