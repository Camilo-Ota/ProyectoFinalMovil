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

class HomeEmpresaActivity : AppCompatActivity() {

    private lateinit var db: DatabaseReference
    private lateinit var rvAdmins: RecyclerView
    private lateinit var btnAgregarAdmin: Button
    private lateinit var btnCerrarSesion: Button
    private lateinit var btnGestionarFlota: Button
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
        RoleGuard.verificar(this, User.ROL_EMPRESA) { iniciarUI() }
    }

    private fun iniciarUI() {
        db = FirebaseDatabase.getInstance().reference

        tvNombreEmpresa   = findViewById(R.id.tvNombreEmpresa)
        rvAdmins          = findViewById(R.id.rvAdminsEmpresa)
        btnAgregarAdmin   = findViewById(R.id.btnAgregarAdmin)
        btnCerrarSesion   = findViewById(R.id.btnCerrarSesionEmpresa)
        btnGestionarFlota = findViewById(R.id.btnGestionarFlota)
        progressBar       = findViewById(R.id.progressBarEmpresa)

        adapter = AdminEmpresaAdapter(
            lista      = listaAdmins,
            onEditar   = { mostrarDialogoEditar(it) },   // ← ahora sí se pasa
            onEliminar = { confirmarEliminar(it) }
        )
        rvAdmins.layoutManager = LinearLayoutManager(this)
        rvAdmins.adapter = adapter

        cargarNombreEmpresa()
        cargarAdmins()

        btnAgregarAdmin.setOnClickListener { mostrarDialogoCrearAdmin() }

        btnGestionarFlota.setOnClickListener {
            startActivity(Intent(this, GestionBusesActivity::class.java))
        }

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
        db.child("users")
            .orderByChild("empresaId")
            .equalTo(empresaUid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    listaAdmins.clear()
                    for (child in snapshot.children) {
                        val user = child.getValue(User::class.java) ?: continue
                        if (user.role == User.ROL_ADMINISTRADOR) listaAdmins.add(user)
                    }
                    adapter.notifyDataSetChanged()
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@HomeEmpresaActivity,
                        "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // ── Crear admin ───────────────────────────────────────────────────────────

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
                    Toast.makeText(this, "Nombre, email y contraseña son obligatorios",
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
        FunctionsHelper.call(
            functionName = "crearAdminEmpresa",
            data = mapOf("nombre" to nombre, "email" to email,
                "password" to pass, "phone" to phone, "empresaId" to empresaUid),
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

    // ── Editar admin ──────────────────────────────────────────────────────────

    private fun mostrarDialogoEditar(admin: User) {
        val view = layoutInflater.inflate(R.layout.dialog_admin_empresa, null)
        val edtNombre = view.findViewById<EditText>(R.id.edtNombreAdmin)
        val edtEmail  = view.findViewById<EditText>(R.id.edtEmailAdmin)
        val edtPass   = view.findViewById<EditText>(R.id.edtPasswordAdmin)
        val edtPhone  = view.findViewById<EditText>(R.id.edtPhoneAdmin)

        // Precargamos los datos actuales
        edtNombre.setText(admin.name)
        edtEmail.setText(admin.email)
        edtEmail.isEnabled = false          // el email no se puede cambiar
        edtPass.hint = "Nueva contraseña (dejar vacío para no cambiar)"
        edtPhone.setText(admin.phone)

        AlertDialog.Builder(this)
            .setTitle("Editar administrador")
            .setView(view)
            .setPositiveButton("Guardar") { _, _ ->
                val uid    = admin.uid ?: return@setPositiveButton
                val nombre = edtNombre.text.toString().trim()
                val pass   = edtPass.text.toString().trim()
                val phone  = edtPhone.text.toString().trim()

                if (nombre.isEmpty()) {
                    Toast.makeText(this, "El nombre no puede estar vacío",
                        Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Actualizar nombre y teléfono directamente en la DB
                val updates = mutableMapOf<String, Any>(
                    "name"  to nombre,
                    "phone" to phone
                )
                db.child("users").child(uid).updateChildren(updates)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Datos actualizados", Toast.LENGTH_SHORT).show()
                    }

                // Si escribió nueva contraseña, actualizarla vía Cloud Function
                if (pass.isNotEmpty()) {
                    if (pass.length < 6) {
                        Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres",
                            Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    mostrarCarga(true)
                    FunctionsHelper.call(
                        functionName = "actualizarPassword",
                        data = mapOf("uid" to uid, "password" to pass),
                        onSuccess = {
                            mainHandler.post {
                                mostrarCarga(false)
                                Toast.makeText(this, "Contraseña actualizada", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onFailure = { error ->
                            mainHandler.post {
                                mostrarCarga(false)
                                Toast.makeText(this, "Error al cambiar contraseña: $error",
                                    Toast.LENGTH_LONG).show()
                            }
                        }
                    )
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ── Eliminar admin ────────────────────────────────────────────────────────

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