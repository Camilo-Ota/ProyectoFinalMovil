package com.camilootal.copia1app

import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Helper para llamar Cloud Functions Gen 2 enviando el token manualmente.
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

        user.getIdToken(true)
            .addOnSuccessListener { tokenResult ->
                val token = tokenResult.token ?: run {
                    onFailure("No se pudo obtener el token de autenticación.")
                    return@addOnSuccessListener
                }
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

        val body = JSONObject()
        body.put("data", JSONObject(data))

        val writer = OutputStreamWriter(connection.outputStream)
        writer.write(body.toString())
        writer.flush()
        writer.close()

        val responseCode = connection.responseCode

        // ✅ FIX: Leer el stream correcto y protegerse contra null
        val responseStream = if (responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream ?: connection.inputStream
        }

        val response = responseStream?.bufferedReader()?.readText()
            ?: throw Exception("Sin respuesta del servidor (código $responseCode)")

        // ✅ FIX: Verificar que la respuesta es JSON antes de parsearla
        val trimmed = response.trim()
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            // El servidor devolvió HTML u otro contenido no-JSON
            throw Exception(
                when (responseCode) {
                    401 -> "No autorizado. Verifica que tu usuario tenga el rol correcto."
                    403 -> "Acceso denegado. Solo empresa_transporte puede crear administradores."
                    404 -> "La función '$functionName' no existe en el servidor."
                    500 -> "Error interno del servidor. Revisa los logs de Firebase."
                    else -> "Error del servidor ($responseCode). Respuesta inesperada."
                }
            )
        }

        val json = JSONObject(trimmed)

        if (responseCode !in 200..299) {
            val error = json.optJSONObject("error")
            val message = error?.optString("message") ?: "Error del servidor ($responseCode)"
            throw Exception(message)
        }

        return json.optJSONObject("result") ?: json
    }
}