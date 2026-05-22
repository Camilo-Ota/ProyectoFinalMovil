package com.camilootal.copia1app

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class RecoverPassword : AppCompatActivity() {

    private lateinit var email: EditText
    private lateinit var btn: Button
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recover_password)

        email = findViewById(R.id.edt_emailRecovery)
        btn = findViewById(R.id.btnRecover)
        auth = FirebaseAuth.getInstance()

        btn.setOnClickListener {
            val correo = email.text.toString()

            if (correo.isEmpty()) {
                Toast.makeText(this, "Ingresa un correo", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.sendPasswordResetEmail(correo)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(
                            this,
                            "Correo enviado para recuperar contraseña",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    } else {
                        Toast.makeText(
                            this,
                            "Error: correo no registrado",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }
    }
}