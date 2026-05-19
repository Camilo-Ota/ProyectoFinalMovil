package com.camilootal.copia1app

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

// Esta activity reemplaza HomePasajeroActivity con el nuevo nombre de rol "usuario"
class HomeUsuarioActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_pasajero)  // reutiliza el layout existente

        val tvBienvenida = findViewById<TextView>(R.id.tvBienvenidaPasajero)
        val btnVerRutas  = findViewById<Button>(R.id.btnVerRutasEnVivo)
        val btnChat      = findViewById<Button>(R.id.btnChat)          // agrega este botón al layout
        val btnCerrar    = findViewById<Button>(R.id.btnCerrarSesionPasajero)

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            FirebaseDatabase.getInstance().reference
                .child("users").child(uid).child("name")
                .get()
                .addOnSuccessListener { snap ->
                    tvBienvenida.text = "Hola, ${snap.getValue(String::class.java) ?: "Usuario"}"
                }
        }

        // Solo puede ver rutas en vivo (lectura, no puede crear ni editar)
        btnVerRutas.setOnClickListener {
            startActivity(Intent(this, SeleccionarRutaPasajeroActivity::class.java))
        }

        // Chat grupal (igual que el conductor)
        btnChat.setOnClickListener {
            startActivity(Intent(this, CanalConductoresActivity::class.java))
        }

        btnCerrar.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LogIn::class.java))
            finishAffinity()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finishAffinity()
    }
}