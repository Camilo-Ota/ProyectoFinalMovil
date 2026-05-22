package com.camilootal.copia1app

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase
import com.camilootal.copia1app.Ruta

class CrearRutaActivity : AppCompatActivity() {

    private lateinit var etNombreRuta: EditText
    private lateinit var etDescripcionRuta: EditText
    private lateinit var etRadioDeteccion: EditText
    private lateinit var btnGuardarRuta: Button
    private lateinit var tvEstadoRuta: TextView

    private val db = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_ruta)

        etNombreRuta = findViewById(R.id.etNombreRuta)
        etDescripcionRuta = findViewById(R.id.etDescripcionRuta)
        etRadioDeteccion = findViewById(R.id.etRadioDeteccion)
        btnGuardarRuta = findViewById(R.id.btnGuardarRuta)
        tvEstadoRuta = findViewById(R.id.tvEstadoRuta)

        btnGuardarRuta.setOnClickListener {
            guardarRuta()
        }
    }

    private fun guardarRuta() {
        val nombre = etNombreRuta.text.toString().trim()
        val descripcion = etDescripcionRuta.text.toString().trim()
        val radioTexto = etRadioDeteccion.text.toString().trim()

        if (nombre.isEmpty()) {
            etNombreRuta.error = "Ingrese el nombre de la ruta"
            etNombreRuta.requestFocus()
            return
        }

        if (descripcion.isEmpty()) {
            etDescripcionRuta.error = "Ingrese la descripción"
            etDescripcionRuta.requestFocus()
            return
        }

        if (radioTexto.isEmpty()) {
            etRadioDeteccion.error = "Ingrese el radio de detección"
            etRadioDeteccion.requestFocus()
            return
        }

        val radio = radioTexto.toFloatOrNull()
        if (radio == null || radio <= 0f) {
            etRadioDeteccion.error = "Ingrese un valor válido"
            etRadioDeteccion.requestFocus()
            return
        }

        btnGuardarRuta.isEnabled = false
        tvEstadoRuta.text = "Guardando ruta..."

        val rutaRef = db.child("rutas").push()
        val rutaId = rutaRef.key ?: ""

        val ruta = Ruta(
            id = rutaId,
            nombre = nombre,
            descripcion = descripcion,
            activa = true,
            radioDeteccion = radio,
            creadaEn = System.currentTimeMillis()
        )

        rutaRef.setValue(ruta)
            .addOnSuccessListener {
                tvEstadoRuta.text = "Ruta guardada correctamente db"
                Toast.makeText(this, "Ruta creada con éxito", Toast.LENGTH_SHORT).show()
                limpiarCampos()
                btnGuardarRuta.isEnabled = true
            }
            .addOnFailureListener { e ->
                tvEstadoRuta.text = "Error al guardar la ruta"
                btnGuardarRuta.isEnabled = true
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun limpiarCampos() {
        etNombreRuta.setText("")
        etDescripcionRuta.setText("")
        etRadioDeteccion.setText("")
        etNombreRuta.requestFocus()
    }
}