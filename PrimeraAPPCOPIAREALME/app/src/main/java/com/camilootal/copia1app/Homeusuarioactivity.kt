package com.camilootal.copia1app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class HomeUsuarioActivity : AppCompatActivity() {

    private lateinit var tvBienvenida: TextView
    private lateinit var btnVerRutas: Button
    private lateinit var btnChat: Button
    private lateinit var btnMisDatos: Button
    private lateinit var btnCerrarSesion: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_home_pasajero)

        tvBienvenida = findViewById(R.id.tvBienvenidaPasajero)

        btnVerRutas = findViewById(R.id.btnVerRutasEnVivo)

        btnChat = findViewById(R.id.btnChat)

        btnMisDatos = findViewById(R.id.btnMisDatosUsuario)

        btnCerrarSesion = findViewById(R.id.btnCerrarSesionPasajero)

        cargarNombreUsuario()

        btnVerRutas.setOnClickListener {

            startActivity(
                Intent(this, SeleccionarRutaPasajeroActivity::class.java)
            )
        }

        btnChat.setOnClickListener {

            val intent =
                Intent(this, CanalConductoresActivity::class.java)

            intent.putExtra("soloLectura", true)

            startActivity(intent)
        }

        btnMisDatos.setOnClickListener {

            startActivity(
                Intent(this, DatosUsuarioActivity::class.java)
            )
        }

        btnCerrarSesion.setOnClickListener {

            FirebaseAuth.getInstance().signOut()

            startActivity(Intent(this, LogIn::class.java))

            finishAffinity()
        }
    }

    private fun cargarNombreUsuario() {

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseDatabase.getInstance().reference
            .child("users")
            .child(uid)
            .child("name")
            .get()
            .addOnSuccessListener {

                val nombre =
                    it.getValue(String::class.java) ?: "Usuario"

                tvBienvenida.text = "Hola, $nombre"
            }
    }
}