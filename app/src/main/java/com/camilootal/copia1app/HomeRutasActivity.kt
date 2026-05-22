package com.camilootal.copia1app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class HomeRutasActivity : AppCompatActivity() {

    private lateinit var btnIrCrearRuta: Button
    private lateinit var btnIrConfigurarPuntos: Button
    private lateinit var btnIrIniciarRecorrido: Button
    private lateinit var btnIrHistorialRecorridos: Button
    private lateinit var btnIrDatosUsuario: Button
    private lateinit var btnIrBlogConductores: Button  // ← nuevo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_rutas)

        btnIrCrearRuta = findViewById(R.id.btnIrCrearRuta)
        btnIrConfigurarPuntos = findViewById(R.id.btnIrConfigurarPuntos)
        btnIrIniciarRecorrido = findViewById(R.id.btnIrIniciarRecorrido)
        btnIrHistorialRecorridos = findViewById(R.id.btnIrHistorialRecorridos)
        btnIrDatosUsuario = findViewById(R.id.btnIrDatosUsuario)
        btnIrBlogConductores = findViewById(R.id.btnIrBlogConductores)  // ← nuevo

        btnIrCrearRuta.setOnClickListener {
            startActivity(Intent(this, CrearRutaActivity::class.java))
        }
        btnIrConfigurarPuntos.setOnClickListener {
            startActivity(Intent(this, ListaRutasActivity::class.java))
        }
        btnIrIniciarRecorrido.setOnClickListener {
            startActivity(Intent(this, ListaRutasRecorridoActivity::class.java))
        }
        btnIrHistorialRecorridos.setOnClickListener {
            startActivity(Intent(this, HistorialRecorridosActivity::class.java))
        }
        btnIrDatosUsuario.setOnClickListener {
            startActivity(Intent(this, DatosUsuarioActivity::class.java))
        }
        btnIrBlogConductores.setOnClickListener {  // ← nuevo
            startActivity(Intent(this, CanalConductoresActivity::class.java))
        }
    }
}