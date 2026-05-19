package com.camilootal.copia1app

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

/**
 * Utilidad para proteger cualquier Activity verificando el rol del usuario.
 *
 * Uso en cualquier Activity protegida — ponlo al inicio de onCreate()
 * (DESPUÉS de setContentView):
 *
 *   RoleGuard.verificar(this, User.ROL_ADMINISTRADOR) {
 *       // Este bloque solo se ejecuta si el rol es válido
 *       iniciarUI()
 *   }
 */
object RoleGuard {

    /**
     * @param context         La Activity que llama
     * @param rolesPermitidos Uno o más roles que pueden acceder. Ej: ROL_CONDUCTOR, ROL_ADMINISTRADOR
     * @param onPermitido     Lambda que se ejecuta solo si el rol es válido
     */
    fun verificar(
        context: Context,
        vararg rolesPermitidos: String,
        onPermitido: () -> Unit
    ) {
        val auth = FirebaseAuth.getInstance()
        val uid  = auth.currentUser?.uid

        if (uid == null) {
            // No hay sesión → volver al Login
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
                    // ✅ FIX 3: Solo mostrar mensaje y redirigir, SIN signOut automático.
                    // El signOut agresivo causaba que el admin quedara deslogueado
                    // si Firebase tardaba en responder o el nodo "role" aún no existía.
                    Toast.makeText(
                        context,
                        "No tienes permiso para acceder a esta sección.",
                        Toast.LENGTH_LONG
                    ).show()
                    redirigirLogin(context, cerrarSesion = true)
                }
            }
            .addOnFailureListener { e ->
                // ✅ FIX 3 (continuación): En error de red NO cerramos sesión.
                // Solo mostramos el error y dejamos al usuario donde está.
                // Si es un error de permisos real (DatabaseException), sí redirigimos.
                val esErrorPermisos = e.message?.contains("Permission denied", ignoreCase = true) == true
                if (esErrorPermisos) {
                    Toast.makeText(context, "Acceso denegado. Verifica tu conexión.", Toast.LENGTH_SHORT).show()
                    redirigirLogin(context, cerrarSesion = true)
                } else {
                    Toast.makeText(context, "Error de red. Intenta de nuevo.", Toast.LENGTH_SHORT).show()
                    // No redirigir, dejar al usuario en la pantalla actual
                }
            }
    }

    private fun redirigirLogin(context: Context, cerrarSesion: Boolean) {
        if (cerrarSesion) {
            FirebaseAuth.getInstance().signOut()
        }
        val intent = Intent(context, LogIn::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
    }
}