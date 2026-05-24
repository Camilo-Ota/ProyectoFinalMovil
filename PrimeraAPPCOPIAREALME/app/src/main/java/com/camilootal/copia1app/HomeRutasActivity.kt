package com.camilootal.copia1app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class HomeRutasActivity : AppCompatActivity() {

    private lateinit var btnIrIniciarRecorrido: Button
    private lateinit var btnIrHistorialRecorridos: Button
    private lateinit var btnIrDatosUsuario: Button
    private lateinit var btnIrBlogConductores: Button
    private lateinit var btnCerrarSesion: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RoleGuard.verificar(this, User.ROL_CONDUCTOR, User.ROL_ADMINISTRADOR) {
            iniciarUI()
        }
    }

    private fun iniciarUI() {
        setContentView(R.layout.activity_home_conductor)

        btnIrIniciarRecorrido    = findViewById(R.id.btnIrIniciarRecorrido)
        btnIrHistorialRecorridos = findViewById(R.id.btnIrHistorialRecorridos)
        btnIrDatosUsuario        = findViewById(R.id.btnIrDatosUsuario)
        btnIrBlogConductores     = findViewById(R.id.btnIrBlogConductores)
        btnCerrarSesion          = findViewById(R.id.btnCerrarSesionConductor)

        // Verificar si tiene bus asignado antes de permitir iniciar recorrido
        btnIrIniciarRecorrido.setOnClickListener { verificarBusYNavegar() }

        btnIrHistorialRecorridos.setOnClickListener {
            startActivity(Intent(this, HistorialRecorridosActivity::class.java))
        }
        btnIrDatosUsuario.setOnClickListener {
            startActivity(Intent(this, DatosUsuarioActivity::class.java))
        }
        btnIrBlogConductores.setOnClickListener {
            startActivity(Intent(this, CanalConductoresActivity::class.java))
        }
        btnCerrarSesion.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LogIn::class.java))
            finishAffinity()
        }
    }

    /**
     * Consulta Firebase para verificar si el conductor tiene un bus asignado.
     * Si no tiene bus, muestra un mensaje y bloquea el acceso a los recorridos.
     */
    private fun verificarBusYNavegar() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseDatabase.getInstance().reference
            .child("users").child(uid).child("busAsignado")
            .get()
            .addOnSuccessListener { snap ->
                val busAsignado = snap.getValue(String::class.java)
                if (busAsignado.isNullOrEmpty()) {
                    Toast.makeText(
                        this,
                        "⚠️ No tienes un bus asignado. Contacta a tu administrador para que te asigne uno antes de iniciar un recorrido.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    startActivity(Intent(this, ListaRutasRecorridoActivity::class.java))
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al verificar bus asignado. Intenta de nuevo.", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finishAffinity()
    }
}
