package com.camilootal.copia1app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LogIn : AppCompatActivity() {

    private lateinit var editEmail: EditText
    private lateinit var editPassword: EditText
    private lateinit var btnLogIn: Button
    private lateinit var btnSingUp: Button
    private lateinit var btnForgotPassword: TextView
    private lateinit var conexion: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_in)

        conexion = FirebaseAuth.getInstance()

        // 🔗 Enlazar vistas
        editEmail = findViewById(R.id.edt_Email)
        editPassword = findViewById(R.id.edt_password)
        btnLogIn = findViewById(R.id.btnLogin)
        btnSingUp = findViewById(R.id.btnSignUp)
        btnForgotPassword = findViewById(R.id.btnForgotPassword)

        // LOGIN
        btnLogIn.setOnClickListener {
            val email = editEmail.text.toString()
            val password = editPassword.text.toString()
            login(email, password)
        }

        // REGISTRAR
        btnSingUp.setOnClickListener {
            val intent = Intent(this@LogIn, SignUp::class.java)
            startActivity(intent)
        }

        // 🔐 RECUPERAR CONTRASEÑA
        btnForgotPassword.setOnClickListener {
            val intent = Intent(this@LogIn, RecoverPassword::class.java)
            startActivity(intent)
        }
    }

    private fun login(email: String, password: String) {
        conexion.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val intent = Intent(this@LogIn, HomeRutasActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(
                        this@LogIn,
                        "Error el usuario no existe.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }
}