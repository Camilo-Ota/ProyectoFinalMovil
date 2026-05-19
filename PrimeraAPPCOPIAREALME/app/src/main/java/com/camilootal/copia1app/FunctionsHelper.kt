package com.camilootal.copia1app

import android.content.Context
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Helper para llamar Cloud Functions Gen 2 enviando el token manualmente.
 * Reemplaza functions.getHttpsCallable() que no adjunta el token correctamente en Gen 2.
 */
object FunctionsHelper {

    private const val BASE_URL = "https://us-central1-appone-1-93d0e.cloudfunctions.net"

    fun call(
        functionName: String,
        data: Map<String, Any>,
        onSuccess: (JSONObject) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            onFailure("Sesión expirada. Por favor inicia sesión de nuevo.")
            return
        }

        // Obtener token fresco y luego hacer la llamada HTTP
        user.getIdToken(true)
            .addOnSuccessListener { tokenResult ->
                val token = tokenResult.token ?: run {
                    onFailure("No se pudo obtener el token de autenticación.")
                    return@addOnSuccessListener
                }
                // Ejecutar la llamada HTTP en un hilo separado
                Thread {
                    try {
                        val result = callHttp(functionName, data, token)
                        onSuccess(result)
                    } catch (e: Exception) {
                        onFailure(e.message ?: "Error desconocido")
                    }
                }.start()
            }
            .addOnFailureListener { e ->
                onFailure("Error de autenticación: ${e.message}")
            }
    }

    private fun callHttp(functionName: String, data: Map<String, Any>, token: String): JSONObject {
        val url = URL("$BASE_URL/$functionName")
        val connection = url.openConnection() as HttpURLConnection

        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.doOutput = true
        connection.connectTimeout = 15000
        connection.readTimeout = 15000

        // Cloud Functions Callable espera { "data": { ... } }
        val body = JSONObject()
        body.put("data", JSONObject(data))

        val writer = OutputStreamWriter(connection.outputStream)
        writer.write(body.toString())
        writer.flush()
        writer.close()

        val responseCode = connection.responseCode
        val responseStream = if (responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        }

        val response = responseStream.bufferedReader().readText()
        val json = JSONObject(response)

        if (responseCode !in 200..299) {
            val error = json.optJSONObject("error")
            val message = error?.optString("message") ?: "Error del servidor ($responseCode)"
            throw Exception(message)
        }

        // Gen 2 devuelve { "result": { ... } }
        return json.optJSONObject("result") ?: json
    }
}