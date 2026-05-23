package com.camilootal.copia1app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class HomeAdminRutasActivity : AppCompatActivity() {

    private lateinit var btnIrCrearRuta: Button
    private lateinit var btnIrConfigurarPuntos: Button
    private lateinit var btnIrIniciarRecorrido: Button
    private lateinit var btnIrHistorialRecorridos: Button
    private lateinit var btnIrDatosUsuario: Button
    private lateinit var btnIrBlogConductores: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RoleGuard.verificar(this, User.ROL_CONDUCTOR, User.ROL_ADMINISTRADOR) {
            iniciarUI()
        }
    }

    private fun iniciarUI() {
        setContentView(R.layout.activity_home_rutas)

        btnIrCrearRuta           = findViewById(R.id.btnIrCrearRuta)
        btnIrConfigurarPuntos    = findViewById(R.id.btnIrConfigurarPuntos)
        btnIrIniciarRecorrido    = findViewById(R.id.btnIrIniciarRecorrido)
        btnIrHistorialRecorridos = findViewById(R.id.btnIrHistorialRecorridos)
        btnIrDatosUsuario        = findViewById(R.id.btnIrDatosUsuario)
        btnIrBlogConductores     = findViewById(R.id.btnIrBlogConductores)

        btnIrCrearRuta.setOnClickListener {
            startActivity(Intent(this, CrearRutaActivity::class.java))
        }
        btnIrConfigurarPuntos.setOnClickListener {
            startActivity(Intent(this, ListaRutasActivity::class.java))
        }
        btnIrIniciarRecorrido.setOnClickListener {
            startActivity(Intent(this, ListaRutasRecorridoActivity::class.java))
        }
        btnIrDatosUsuario.setOnClickListener {
            startActivity(Intent(this, DatosUsuarioActivity::class.java))
        }
        btnIrBlogConductores.setOnClickListener {
            startActivity(Intent(this, CanalConductoresActivity::class.java))
        }

        // Al tocar historial, detectar rol para saber qué mostrar
        btnIrHistorialRecorridos.setOnClickListener {
            abrirHistorialSegunRol()
        }
    }

    private fun abrirHistorialSegunRol() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseDatabase.getInstance().reference
            .child("users").child(uid)
            .get()
            .addOnSuccessListener { snap ->
                val role      = snap.child("role").getValue(String::class.java)
                val empresaId = snap.child("empresaId").getValue(String::class.java)

                val intent = Intent(this, HistorialRecorridosActivity::class.java)

                when (role) {
                    User.ROL_ADMINISTRADOR -> {
                        // Admin ve todos los recorridos de su empresa
                        intent.putExtra("modo", "empresa")
                        intent.putExtra("empresaId", empresaId)
                    }
                    User.ROL_CONDUCTOR -> {
                        // Conductor ve solo sus propios recorridos
                        intent.putExtra("modo", "propio")
                        intent.putExtra("conductorUid", uid)
                    }
                }
                startActivity(intent)
            }
    }
}