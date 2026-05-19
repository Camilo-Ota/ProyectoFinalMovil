package com.camilootal.copia1app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuth

class SignUp : AppCompatActivity() {

    private lateinit var editName: EditText
    private lateinit var editEmail: EditText
    private lateinit var editPassword: EditText
    private lateinit var editPhone: EditText
    private lateinit var btnSingUp: Button
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mDbRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        supportActionBar?.hide()

        mAuth  = FirebaseAuth.getInstance()
        mDbRef = FirebaseDatabase.getInstance().reference

        editName     = findViewById(R.id.edt_Name)
        editEmail    = findViewById(R.id.edt_Email)
        editPassword = findViewById(R.id.edt_password)
        editPhone    = findViewById(R.id.edt_phone)
        btnSingUp    = findViewById(R.id.btnSignUp)

        // ── El RadioGroup de conductor/pasajero ya NO existe en esta pantalla.
        // En el layout activity_register elimina el RadioGroup o hazlo GONE.
        // El rol siempre será "usuario" aquí.

        btnSingUp.setOnClickListener {
            val name     = editName.text.toString().trim()
            val email    = editEmail.text.toString().trim()
            val password = editPassword.text.toString().trim()
            val phone    = editPhone.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            registrarUsuario(name, email, password, phone)
        }
    }

    private fun registrarUsuario(name: String, email: String, password: String, phone: String) {
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val uid  = mAuth.currentUser!!.uid
                    val user = User(
                        name     = name,
                        email    = email,
                        uid      = uid,
                        phone    = phone,
                        password = password,
                        role     = User.ROL_USUARIO   // ← siempre "usuario"
                    )
                    mDbRef.child("users").child(uid).setValue(user)
                        .addOnSuccessListener {
                            startActivity(Intent(this, HomeUsuarioActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                } else {
                    Toast.makeText(this, "Ha ocurrido un error.", Toast.LENGTH_SHORT).show()
                }
            }
    }
}