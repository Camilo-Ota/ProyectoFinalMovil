package com.camilootal.copia1app

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class HomeUsuarioActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_pasajero)

        val tvBienvenida = findViewById<TextView>(R.id.tvBienvenidaPasajero)
        val btnVerRutas  = findViewById<Button>(R.id.btnVerRutasEnVivo)
        val btnChat      = findViewById<Button>(R.id.btnChat)
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

        btnVerRutas.setOnClickListener {
            startActivity(Intent(this, SeleccionarRutaPasajeroActivity::class.java))
        }

        //  : Pasar soloLectura=true para que el usuario no pueda publicar
        btnChat.setOnClickListener {
            val intent = Intent(this, CanalConductoresActivity::class.java)
            intent.putExtra("soloLectura", true)
            startActivity(intent)
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