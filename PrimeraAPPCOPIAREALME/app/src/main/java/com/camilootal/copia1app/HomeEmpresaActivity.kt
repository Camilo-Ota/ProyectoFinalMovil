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

/**
 * Pantalla principal del rol EMPRESA_TRANSPORTE.
 *
 * La empresa crea administradores (ROL_ADMINISTRADOR) vinculados
 * a ella mediante el campo empresaId. Esos admins usan la misma
 * HomeAdminActivity de siempre, pero al detectar su empresaId
 * solo ven y crean conductores de su empresa.
 */
class HomeEmpresaActivity : AppCompatActivity() {

    private lateinit var db: DatabaseReference
    private lateinit var rvAdmins: RecyclerView
    private lateinit var btnAgregarAdmin: Button
    private lateinit var btnCerrarSesion: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvNombreEmpresa: TextView

    private val listaAdmins = mutableListOf<User>()
    private lateinit var adapter: AdminEmpresaAdapter
    private val mainHandler = Handler(Looper.getMainLooper())

    private val empresaUid: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_empresa)

        RoleGuard.verificar(this, User.ROL_EMPRESA) {
            iniciarUI()
        }
    }

    private fun iniciarUI() {
        db = FirebaseDatabase.getInstance().reference

        tvNombreEmpresa = findViewById(R.id.tvNombreEmpresa)
        rvAdmins        = findViewById(R.id.rvAdminsEmpresa)
        btnAgregarAdmin = findViewById(R.id.btnAgregarAdmin)
        btnCerrarSesion = findViewById(R.id.btnCerrarSesionEmpresa)
        progressBar     = findViewById(R.id.progressBarEmpresa)

        adapter = AdminEmpresaAdapter(
            lista      = listaAdmins,
            onEliminar = { confirmarEliminar(it) }
        )
        rvAdmins.layoutManager = LinearLayoutManager(this)
        rvAdmins.adapter = adapter

        cargarNombreEmpresa()
        cargarAdmins()

        btnAgregarAdmin.setOnClickListener { mostrarDialogoCrearAdmin() }

        btnCerrarSesion.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LogIn::class.java))
            finishAffinity()
        }
    }

    private fun cargarNombreEmpresa() {
        db.child("users").child(empresaUid).child("name").get()
            .addOnSuccessListener { snap ->
                tvNombreEmpresa.text = snap.getValue(String::class.java) ?: "Mi empresa"
            }
    }

    private fun cargarAdmins() {
        // Filtramos por empresaId para ver solo los admins de esta empresa
        db.child("users")
            .orderByChild("empresaId")
            .equalTo(empresaUid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    listaAdmins.clear()
                    for (child in snapshot.children) {
                        val user = child.getValue(User::class.java) ?: continue
                        // Solo mostramos admins (no conductores que también tienen empresaId)
                        if (user.role == User.ROL_ADMINISTRADOR) {
                            listaAdmins.add(user)
                        }
                    }
                    adapter.notifyDataSetChanged()
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@HomeEmpresaActivity,
                        "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun mostrarDialogoCrearAdmin() {
        val view = layoutInflater.inflate(R.layout.dialog_admin_empresa, null)

        val edtNombre = view.findViewById<EditText>(R.id.edtNombreAdmin)
        val edtEmail  = view.findViewById<EditText>(R.id.edtEmailAdmin)
        val edtPass   = view.findViewById<EditText>(R.id.edtPasswordAdmin)
        val edtPhone  = view.findViewById<EditText>(R.id.edtPhoneAdmin)

        AlertDialog.Builder(this)
            .setTitle("Crear administrador")
            .setView(view)
            .setPositiveButton("Crear") { _, _ ->
                val nombre = edtNombre.text.toString().trim()
                val email  = edtEmail.text.toString().trim()
                val pass   = edtPass.text.toString().trim()
                val phone  = edtPhone.text.toString().trim()

                if (nombre.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                    Toast.makeText(this,
                        "Nombre, email y contraseña son obligatorios",
                        Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                crearAdmin(nombre, email, pass, phone)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun crearAdmin(nombre: String, email: String, pass: String, phone: String) {
        mostrarCarga(true)

        // Usamos la Cloud Function "crearAdminEmpresa"
        // El admin se guarda con role = ROL_ADMINISTRADOR y empresaId = uid de esta empresa
        FunctionsHelper.call(
            functionName = "crearAdminEmpresa",
            data = mapOf(
                "nombre"    to nombre,
                "email"     to email,
                "password"  to pass,
                "phone"     to phone,
                "empresaId" to empresaUid
            ),
            onSuccess = {
                mainHandler.post {
                    mostrarCarga(false)
                    Toast.makeText(this, "Administrador creado", Toast.LENGTH_SHORT).show()
                }
            },
            onFailure = { error ->
                mainHandler.post {
                    mostrarCarga(false)
                    Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun confirmarEliminar(admin: User) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar administrador")
            .setMessage("¿Seguro que deseas eliminar a ${admin.name}?")
            .setPositiveButton("Eliminar") { _, _ ->
                mostrarCarga(true)
                FunctionsHelper.call(
                    functionName = "eliminarUsuario",
                    data = mapOf("uid" to (admin.uid ?: "")),
                    onSuccess = {
                        mainHandler.post {
                            mostrarCarga(false)
                            Toast.makeText(this, "Administrador eliminado", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onFailure = { error ->
                        mainHandler.post {
                            mostrarCarga(false)
                            Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarCarga(mostrar: Boolean) {
        progressBar.visibility = if (mostrar) View.VISIBLE else View.GONE
    }
}
