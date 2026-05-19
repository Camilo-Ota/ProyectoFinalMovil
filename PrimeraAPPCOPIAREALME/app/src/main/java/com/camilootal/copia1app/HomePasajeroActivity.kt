package com.camilootal.copia1app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class HomePasajeroActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_pasajero)

        val tvBienvenida = findViewById<TextView>(R.id.tvBienvenidaPasajero)
        val btnVerRutas = findViewById<Button>(R.id.btnVerRutasEnVivo)
        val btnCerrar = findViewById<Button>(R.id.btnCerrarSesionPasajero)

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            FirebaseDatabase.getInstance().reference
                .child("users").child(uid).child("name")
                .get()
                .addOnSuccessListener { snap ->
                    tvBienvenida.text = "Hola, ${snap.getValue(String::class.java) ?: "Pasajero"}"
                }
        }

        btnVerRutas.setOnClickListener {
            startActivity(Intent(this, SeleccionarRutaPasajeroActivity::class.java))
        }

        btnCerrar.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LogIn::class.java))
            finish()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        finishAffinity()
    }
}