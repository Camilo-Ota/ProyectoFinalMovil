package com.camilootal.copia1app

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class DatosUsuarioActivity : AppCompatActivity() {

    private lateinit var tvNombre: TextView
    private lateinit var tvCorreo: TextView
    private lateinit var tvTelefono: TextView
    private lateinit var tvRol: TextView
    private lateinit var tvPlaca: TextView
    private lateinit var tvLicencia: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_datos_usuario)

        tvNombre = findViewById(R.id.tvNombre)
        tvCorreo = findViewById(R.id.tvCorreo)
        tvTelefono = findViewById(R.id.tvTelefono)
        tvRol = findViewById(R.id.tvRol)
        tvPlaca = findViewById(R.id.tvPlaca)
        tvLicencia = findViewById(R.id.tvLicencia)

        cargarDatosUsuario()
    }

    private fun cargarDatosUsuario() {

        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null) {

            Toast.makeText(
                this,
                "Usuario no autenticado",
                Toast.LENGTH_SHORT
            ).show()

            finish()

            return
        }

        FirebaseDatabase.getInstance().reference
            .child("users")
            .child(uid)
            .get()
            .addOnSuccessListener { snapshot ->

                if (!snapshot.exists()) {

                    Toast.makeText(
                        this,
                        "No se encontraron datos",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@addOnSuccessListener
                }

                val nombre =
                    snapshot.child("name").getValue(String::class.java)

                val correo =
                    snapshot.child("email").getValue(String::class.java)

                val telefono =
                    snapshot.child("phone").getValue(String::class.java)

                val rol =
                    snapshot.child("role").getValue(String::class.java)

                val placa =
                    snapshot.child("placa").getValue(String::class.java)

                val licencia =
                    snapshot.child("licencia").getValue(String::class.java)

                tvNombre.text = "Nombre: ${nombre ?: "No disponible"}"

                tvCorreo.text = "Correo: ${correo ?: "No disponible"}"

                tvTelefono.text = "Teléfono: ${telefono ?: "No disponible"}"

                tvRol.text = "Rol: ${rol ?: "No disponible"}"

                // Mostrar solo para conductor
                if (rol == User.ROL_CONDUCTOR) {

                    tvPlaca.visibility = View.VISIBLE
                    tvLicencia.visibility = View.VISIBLE

                    tvPlaca.text =
                        "Placa: ${placa ?: "No registrada"}"

                    tvLicencia.text =
                        "Licencia: ${licencia ?: "No registrada"}"
                }
            }

            .addOnFailureListener {

                Toast.makeText(
                    this,
                    "Error al cargar datos",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}