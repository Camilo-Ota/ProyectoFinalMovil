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
    private lateinit var btnAsignarBuses: Button
    private lateinit var progressBar: ProgressBar

    private val listaConductores = mutableListOf<User>()
    private lateinit var adapter: ConductorAdminAdapter
    private val mainHandler = Handler(Looper.getMainLooper())

    private var empresaId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_admin)
        RoleGuard.verificar(this, User.ROL_ADMINISTRADOR) { iniciarUI() }
    }

    private fun iniciarUI() {
        db = FirebaseDatabase.getInstance().reference

        rvConductores       = findViewById(R.id.rvConductores)
        btnAgregarConductor = findViewById(R.id.btnAgregarConductor)
        btnIrCrearRuta      = findViewById(R.id.btnIrCrearRuta)
        btnCerrarSesion     = findViewById(R.id.btnCerrarSesionAdmin)
        btnAsignarBuses     = findViewById(R.id.btnAsignarBuses)
        progressBar         = findViewById(R.id.progressBarAdmin)

        adapter = ConductorAdminAdapter(
            lista          = listaConductores,
            onEditar       = { mostrarDialogoEditar(it) },
            onEliminar     = { confirmarEliminar(it) },
            onVerHistorial = { conductor ->
                val intent = Intent(this, HistorialRecorridosActivity::class.java)
                intent.putExtra("conductorUid",    conductor.uid)
                intent.putExtra("conductorNombre", conductor.name)
                startActivity(intent)
            }
        )
        rvConductores.layoutManager = LinearLayoutManager(this)
        rvConductores.adapter = adapter

        btnAgregarConductor.setOnClickListener { mostrarDialogoCrear() }
        btnIrCrearRuta.setOnClickListener { startActivity(Intent(this, HomeAdminRutasActivity::class.java)) }
        btnAsignarBuses.setOnClickListener { startActivity(Intent(this, AsignarBusConductorActivity::class.java)) }
        btnCerrarSesion.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LogIn::class.java))
            finishAffinity()
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.child("users").child(uid).child("empresaId").get()
            .addOnSuccessListener { snap ->
                empresaId = snap.getValue(String::class.java)
                cargarConductores()
            }
            .addOnFailureListener { cargarConductores() }
    }

    private fun cargarConductores() {
        val query: Query = if (!empresaId.isNullOrEmpty()) {
            db.child("users").orderByChild("empresaId").equalTo(empresaId)
        } else {
            db.child("users").orderByChild("role").equalTo(User.ROL_CONDUCTOR)
        }

        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listaConductores.clear()
                for (child in snapshot.children) {
                    val user = child.getValue(User::class.java) ?: continue
                    if (user.role == User.ROL_CONDUCTOR) listaConductores.add(user)
                }
                adapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@HomeAdminActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // ── Crear: usa dialog_crear_conductor.xml (sin campo placa) ──────────────

    private fun mostrarDialogoCrear() {
        val view      = layoutInflater.inflate(R.layout.dialog_crear_conductor, null)
        val edtNombre = view.findViewById<EditText>(R.id.edtNombreConductor)
        val edtEmail  = view.findViewById<EditText>(R.id.edtEmailConductor)
        val edtPass   = view.findViewById<EditText>(R.id.edtPasswordConductor)
        val edtPhone  = view.findViewById<EditText>(R.id.edtPhoneConductor)

        AlertDialog.Builder(this)
            .setTitle("Crear conductor")
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

                mostrarCarga(true)
                val data = mutableMapOf<String, Any>(
                    "nombre" to nombre, "email" to email,
                    "password" to pass, "phone" to phone, "placa" to ""
                )
                if (!empresaId.isNullOrEmpty()) data["empresaId"] = empresaId!!

                FunctionsHelper.call(
                    functionName = "crearConductor",
                    data = data,
                    onSuccess = { mainHandler.post { mostrarCarga(false); Toast.makeText(this, "Conductor creado", Toast.LENGTH_SHORT).show() } },
                    onFailure = { error -> mainHandler.post { mostrarCarga(false); Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show() } }
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ── Editar: usa dialog_editar_conductor.xml (sin placa, con info del bus) ─

    private fun mostrarDialogoEditar(conductor: User) {
        val view        = layoutInflater.inflate(R.layout.dialog_editar_conductor, null)
        val tvBusInfo   = view.findViewById<TextView>(R.id.tvBusAsignadoInfo)
        val edtNombre   = view.findViewById<EditText>(R.id.edtNombreConductor)
        val edtEmail    = view.findViewById<EditText>(R.id.edtEmailConductor)
        val edtPass     = view.findViewById<EditText>(R.id.edtPasswordConductor)
        val edtPhone    = view.findViewById<EditText>(R.id.edtPhoneConductor)

        // Mostrar info del bus asignado (solo lectura)
        tvBusInfo.text = when {
            !conductor.busPlaca.isNullOrEmpty() && !conductor.busModelo.isNullOrEmpty() ->
                "🚌 Bus asignado: ${conductor.busPlaca} — ${conductor.busModelo}"
            !conductor.busPlaca.isNullOrEmpty() ->
                "🚌 Bus asignado: ${conductor.busPlaca}"
            !conductor.busAsignado.isNullOrEmpty() ->
                "🚌 Bus asignado (sin placa registrada)"
            else ->
                "🚌 Sin bus asignado"
        }

        edtNombre.setText(conductor.name)
        edtEmail.setText(conductor.email)
        edtPhone.setText(conductor.phone)

        AlertDialog.Builder(this)
            .setTitle("Editar conductor")
            .setView(view)
            .setPositiveButton("Guardar") { _, _ ->
                val uid    = conductor.uid ?: return@setPositiveButton
                val nombre = edtNombre.text.toString().trim()
                val phone  = edtPhone.text.toString().trim()
                val pass   = edtPass.text.toString().trim()

                if (nombre.isEmpty()) {
                    Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                db.child("users").child(uid).updateChildren(
                    mapOf("name" to nombre, "phone" to phone)
                )

                if (pass.isNotEmpty()) {
                    if (pass.length < 6) {
                        Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    mostrarCarga(true)
                    FunctionsHelper.call(
                        functionName = "actualizarPassword",
                        data = mapOf("uid" to uid, "password" to pass),
                        onSuccess = { mainHandler.post { mostrarCarga(false); Toast.makeText(this, "Contraseña actualizada", Toast.LENGTH_SHORT).show() } },
                        onFailure = { error -> mainHandler.post { mostrarCarga(false); Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show() } }
                    )
                } else {
                    Toast.makeText(this, "Conductor actualizado", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarEliminar(conductor: User) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar conductor")
            .setMessage("¿Seguro que deseas eliminar a ${conductor.name}?")
            .setPositiveButton("Eliminar") { _, _ ->
                mostrarCarga(true)
                FunctionsHelper.call(
                    functionName = "eliminarUsuario",
                    data = mapOf("uid" to (conductor.uid ?: "")),
                    onSuccess = { mainHandler.post { mostrarCarga(false); Toast.makeText(this, "Conductor eliminado", Toast.LENGTH_SHORT).show() } },
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
