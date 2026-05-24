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

    // ── Guardar referencia para poder remover en onDestroy ────────────────────
    private var adminsListener: ValueEventListener? = null
    private var adminsQuery: Query? = null

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
            onEditar   = { mostrarDialogoEditar(it) },
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
        btnCerrarSesion.setOnClickListener { cerrarSesion() }
    }

    private fun cerrarSesion() {
        desconectarListeners()
        FirebaseAuth.getInstance().signOut()
        startActivity(Intent(this, LogIn::class.java))
        finishAffinity()
    }

    private fun cargarNombreEmpresa() {
        db.child("users").child(empresaUid).child("name").get()
            .addOnSuccessListener { snap ->
                tvNombreEmpresa.text = snap.getValue(String::class.java) ?: "Mi empresa"
            }
    }

    private fun cargarAdmins() {
        val query = db.child("users")
            .orderByChild("empresaId")
            .equalTo(empresaUid)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listaAdmins.clear()
                for (child in snapshot.children) {
                    val user = child.getValue(User::class.java) ?: continue
                    if (user.role == User.ROL_ADMINISTRADOR) listaAdmins.add(user)
                }
                adapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {
                if (FirebaseAuth.getInstance().currentUser != null) {
                    Toast.makeText(this@HomeEmpresaActivity,
                        "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        adminsQuery    = query
        adminsListener = listener
        query.addValueEventListener(listener)
    }

    private fun desconectarListeners() {
        adminsListener?.let { adminsQuery?.removeEventListener(it) }
        adminsListener = null
        adminsQuery    = null
    }

    override fun onDestroy() {
        super.onDestroy()
        desconectarListeners()
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
                    Toast.makeText(this, "Nombre, email y contraseña son obligatorios", Toast.LENGTH_SHORT).show()
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
            onSuccess = { mainHandler.post { mostrarCarga(false); Toast.makeText(this, "Administrador creado", Toast.LENGTH_SHORT).show() } },
            onFailure = { error -> mainHandler.post { mostrarCarga(false); Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show() } }
        )
    }

    private fun mostrarDialogoEditar(admin: User) {
        val view = layoutInflater.inflate(R.layout.dialog_admin_empresa, null)
        val edtNombre = view.findViewById<EditText>(R.id.edtNombreAdmin)
        val edtEmail  = view.findViewById<EditText>(R.id.edtEmailAdmin)
        val edtPass   = view.findViewById<EditText>(R.id.edtPasswordAdmin)
        val edtPhone  = view.findViewById<EditText>(R.id.edtPhoneAdmin)

        edtNombre.setText(admin.name)
        edtEmail.setText(admin.email)
        edtEmail.isEnabled = false
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
                if (nombre.isEmpty()) { Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                db.child("users").child(uid).updateChildren(mapOf("name" to nombre, "phone" to phone))
                    .addOnSuccessListener { Toast.makeText(this, "Datos actualizados", Toast.LENGTH_SHORT).show() }
                if (pass.isNotEmpty()) {
                    if (pass.length < 6) { Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                    mostrarCarga(true)
                    FunctionsHelper.call("actualizarPassword", mapOf("uid" to uid, "password" to pass),
                        onSuccess = { mainHandler.post { mostrarCarga(false); Toast.makeText(this, "Contraseña actualizada", Toast.LENGTH_SHORT).show() } },
                        onFailure = { error -> mainHandler.post { mostrarCarga(false); Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show() } }
                    )
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarEliminar(admin: User) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar administrador")
            .setMessage("¿Seguro que deseas eliminar a ${admin.name}?")
            .setPositiveButton("Eliminar") { _, _ ->
                mostrarCarga(true)
                FunctionsHelper.call("eliminarUsuario", mapOf("uid" to (admin.uid ?: "")),
                    onSuccess = { mainHandler.post { mostrarCarga(false); Toast.makeText(this, "Administrador eliminado", Toast.LENGTH_SHORT).show() } },
                    onFailure = { error -> mainHandler.post { mostrarCarga(false); Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show() } }
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarCarga(mostrar: Boolean) {
        progressBar.visibility = if (mostrar) View.VISIBLE else View.GONE
    }
}
