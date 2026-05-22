package com.camilootal.copia1app

// Modelo de datos para una publicación del canal de conductores.
// Se guarda en Firebase Realtime Database bajo la ruta: "canal_conductores/{id}"
data class Publicacion(
    var id: String = "",
    var autorId: String = "",          // UID del conductor (Firebase Auth)
    var autorNombre: String = "",      // Nombre del conductor para mostrar
    var texto: String = "",            // Contenido de la publicación
    var categoria: String = "General", // "Alerta" | "Trancón" | "Info" | "General"
    var timestamp: Long = 0L           // System.currentTimeMillis() al publicar
)