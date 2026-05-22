package com.camilootal.copia1app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class HomeAdminActivity : AppCompatActivity() {

    private lateinit var db: DatabaseReference
    private lateinit var rvConductores: RecyclerView
    private lateinit var btnAgregarConductor: Button
    private lateinit var btnIrCrearRuta: Button
    private lateinit var btnCerrarSesion: Button
    private lateinit var progressBar: ProgressBar

    private val listaConductores = mutableListOf<User>()
    private lateinit var adapter: ConductorAdminAdapter
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_admin)

        RoleGuard.verificar(this, User.ROL_ADMINISTRADOR) {
            iniciarUI()
        }
    }

    private fun iniciarUI() {

        db = FirebaseDatabase.getInstance().reference

        rvConductores       = findViewById(R.id.rvConductores)
        btnAgregarConductor = findViewById(R.id.btnAgregarConductor)
        btnIrCrearRuta      = findViewById(R.id.btnIrCrearRuta)
        btnCerrarSesion     = findViewById(R.id.btnCerrarSesionAdmin)
        progressBar         = findViewById(R.id.progressBarAdmin)

        adapter = ConductorAdminAdapter(
            lista      = listaConductores,
            onEditar   = { mostrarDialogoEditar(it) },
            onEliminar = { confirmarEliminar(it) }
        )

        rvConductores.layoutManager = LinearLayoutManager(this)
        rvConductores.adapter = adapter

        cargarConductores()

        btnAgregarConductor.setOnClickListener {
            mostrarDialogoCrear()
        }

        btnIrCrearRuta.setOnClickListener {
            startActivity(Intent(this, HomeAdminRutasActivity::class.java))
        }


        btnCerrarSesion.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LogIn::class.java))
            finishAffinity()
        }
    }

    private fun cargarConductores() {

        db.child("users")
            .orderByChild("role")
            .equalTo(User.ROL_CONDUCTOR)
            .addValueEventListener(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    listaConductores.clear()

                    for (child in snapshot.children) {
                        child.getValue(User::class.java)?.let {
                            listaConductores.add(it)
                        }
                    }

                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {

                    Toast.makeText(
                        this@HomeAdminActivity,
                        "Error: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun mostrarDialogoCrear() {

        val view = layoutInflater.inflate(R.layout.dialog_conductor, null)

        val edtNombre = view.findViewById<EditText>(R.id.edtNombreConductor)
        val edtEmail  = view.findViewById<EditText>(R.id.edtEmailConductor)
        val edtPass   = view.findViewById<EditText>(R.id.edtPasswordConductor)
        val edtPhone  = view.findViewById<EditText>(R.id.edtPhoneConductor)
        val edtPlaca  = view.findViewById<EditText>(R.id.edtPlacaConductor)

        AlertDialog.Builder(this)
            .setTitle("Crear conductor")
            .setView(view)
            .setPositiveButton("Crear") { _, _ ->

                val nombre = edtNombre.text.toString().trim()
                val email  = edtEmail.text.toString().trim()
                val pass   = edtPass.text.toString().trim()
                val phone  = edtPhone.text.toString().trim()
                val placa  = edtPlaca.text.toString().trim()

                if (nombre.isEmpty() || email.isEmpty() || pass.isEmpty()) {

                    Toast.makeText(
                        this,
                        "Nombre, email y contraseña son obligatorios",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setPositiveButton
                }

                mostrarCarga(true)

                FunctionsHelper.call(
                    functionName = "crearConductor",
                    data = mapOf(
                        "nombre" to nombre,
                        "email" to email,
                        "password" to pass,
                        "phone" to phone,
                        "placa" to placa
                    ),

                    onSuccess = {

                        mainHandler.post {

                            mostrarCarga(false)

                            Toast.makeText(
                                this,
                                "Conductor creado",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },

                    onFailure = { error ->

                        mainHandler.post {

                            mostrarCarga(false)

                            Toast.makeText(
                                this,
                                "Error: $error",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoEditar(conductor: User) {

        val view = layoutInflater.inflate(R.layout.dialog_conductor, null)

        val edtNombre = view.findViewById<EditText>(R.id.edtNombreConductor)
        val edtEmail  = view.findViewById<EditText>(R.id.edtEmailConductor)
        val edtPass   = view.findViewById<EditText>(R.id.edtPasswordConductor)
        val edtPhone  = view.findViewById<EditText>(R.id.edtPhoneConductor)
        val edtPlaca  = view.findViewById<EditText>(R.id.edtPlacaConductor)

        edtNombre.setText(conductor.name)
        edtPhone.setText(conductor.phone)
        edtPlaca.setText(conductor.placa)
        edtEmail.setText(conductor.email)

        edtEmail.isEnabled = false

        AlertDialog.Builder(this)
            .setTitle("Editar conductor")
            .setView(view)
            .setPositiveButton("Guardar") { _, _ ->

                Toast.makeText(
                    this,
                    "Conductor actualizado",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarEliminar(conductor: User) {

        AlertDialog.Builder(this)
            .setTitle("Eliminar conductor")
            .setMessage("¿Seguro que deseas eliminar a ${conductor.name}?")
            .setPositiveButton("Eliminar") { _, _ ->

                Toast.makeText(
                    this,
                    "Conductor eliminado",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarCarga(mostrar: Boolean) {

        progressBar.visibility =
            if (mostrar) View.VISIBLE else View.GONE
    }
}