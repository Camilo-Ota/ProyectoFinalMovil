package com.camilootal.copia1app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class HomeRutasActivity : AppCompatActivity() {

    // El conductor solo puede: iniciar recorrido, historial, sus datos y chat.
    // Crear rutas y configurar puntos son tareas del administrador.

    private lateinit var btnIrIniciarRecorrido: Button
    private lateinit var btnIrHistorialRecorridos: Button
    private lateinit var btnIrDatosUsuario: Button
    private lateinit var btnIrBlogConductores: Button
    private lateinit var btnCerrarSesion: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ── Verificar que sea conductor o administrador antes de continuar ──
        RoleGuard.verificar(this, User.ROL_CONDUCTOR, User.ROL_ADMINISTRADOR) {
            iniciarUI()
        }
    }

    private fun iniciarUI() {
        setContentView(R.layout.activity_home_conductor)  // nuevo layout (ver XML abajo)

        btnIrIniciarRecorrido   = findViewById(R.id.btnIrIniciarRecorrido)
        btnIrHistorialRecorridos = findViewById(R.id.btnIrHistorialRecorridos)
        btnIrDatosUsuario       = findViewById(R.id.btnIrDatosUsuario)
        btnIrBlogConductores    = findViewById(R.id.btnIrBlogConductores)
        btnCerrarSesion         = findViewById(R.id.btnCerrarSesionConductor)

        btnIrIniciarRecorrido.setOnClickListener {
            startActivity(Intent(this, ListaRutasRecorridoActivity::class.java))
        }
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

    override fun onBackPressed() {
        super.onBackPressed()
        finishAffinity()
    }
}