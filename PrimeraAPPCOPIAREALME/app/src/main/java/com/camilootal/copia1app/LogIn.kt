package com.camilootal.copia1app

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class LogIn : AppCompatActivity() {

    private lateinit var editEmail: EditText
    private lateinit var editPassword: EditText
    private lateinit var btnLogIn: Button
    private lateinit var btnSignUp: Button
    private lateinit var btnForgotPassword: TextView
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_in)

        auth              = FirebaseAuth.getInstance()
        editEmail         = findViewById(R.id.edt_Email)
        editPassword      = findViewById(R.id.edt_password)
        btnLogIn          = findViewById(R.id.btnLogin)
        btnSignUp         = findViewById(R.id.btnSignUp)
        btnForgotPassword = findViewById(R.id.btnForgotPassword)

        btnLogIn.setOnClickListener {
            val email    = editEmail.text.toString().trim()
            val password = editPassword.text.toString().trim()
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            login(email, password)
        }

        btnSignUp.setOnClickListener {
            startActivity(Intent(this, SignUp::class.java))
        }

        btnForgotPassword.setOnClickListener {
            startActivity(Intent(this, RecoverPassword::class.java))
        }
    }

    private fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (!task.isSuccessful) {
                    Toast.makeText(this, "Correo o contraseña incorrectos.", Toast.LENGTH_SHORT).show()
                    return@addOnCompleteListener
                }
                val uid = auth.currentUser?.uid ?: return@addOnCompleteListener
                leerRolYNavegar(uid)
            }
    }

    private fun leerRolYNavegar(uid: String) {
        FirebaseDatabase.getInstance().reference
            .child("users").child(uid).child("role")
            .get()
            .addOnSuccessListener { snapshot ->
                val rol = snapshot.getValue(String::class.java)
                if (rol.isNullOrEmpty()) {
                    auth.signOut()
                    Toast.makeText(this,
                        "Tu cuenta no tiene un rol asignado. Contacta al administrador.",
                        Toast.LENGTH_LONG).show()
                } else {
                    navegarSegunRol(rol)
                }
            }
            .addOnFailureListener {
                auth.signOut()
                Toast.makeText(this, "Error al verificar permisos. Intenta de nuevo.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun navegarSegunRol(rol: String) {
        val destino = when (rol) {
            User.ROL_ADMINISTRADOR -> HomeAdminActivity::class.java
            User.ROL_CONDUCTOR     -> HomeRutasActivity::class.java
            User.ROL_EMPRESA       -> HomeEmpresaActivity::class.java  // ← nuevo
            else                   -> HomeUsuarioActivity::class.java
        }
        startActivity(Intent(this, destino))
        finish()
    }
}
