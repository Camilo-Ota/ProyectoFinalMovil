package com.camilootal.copia1app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
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

        mAuth = FirebaseAuth.getInstance()

        editName = findViewById(R.id.edt_Name)
        editEmail = findViewById(R.id.edt_Email)
        editPassword = findViewById(R.id.edt_password)
        editPhone = findViewById(R.id.edt_phone)
        btnSingUp = findViewById(R.id.btnSignUp)

        btnSingUp.setOnClickListener {

            val name = editName.text.toString()
            val email = editEmail.text.toString()
            val password = editPassword.text.toString()
            val phone = editPhone.text.toString()

            // Validación básica
            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            singUp(name, email, password, phone)
        }
    }

    private fun singUp(name: String, email: String, password: String, phone: String) {

        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {

                    val uid = mAuth.currentUser?.uid!!

                    addUserToDatabase(name, email, uid, phone, password)

                    val intent = Intent(this@SignUp, HomeRutasActivity::class.java)
                    startActivity(intent)
                    finish()

                } else {
                    Toast.makeText(
                        this@SignUp,
                        "Ha ocurrido un error.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun addUserToDatabase(
        name: String,
        email: String,
        uid: String,
        phone: String,
        password: String
    ) {

        mDbRef = FirebaseDatabase.getInstance().getReference()

        val user = User(name, email, uid, phone, password)

        mDbRef.child("users").child(uid)
            .setValue(user)
            .addOnSuccessListener {
                Toast.makeText(this, "Usuario guardado", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}