package com.example.lumka_app

data class Incident(
    var id: String? = null,
    val userId: String? = null,
    val timestamp: Long? = null,        // epoch millis
    val title: String = "",
    val description: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    var address: String? = null,
    val likes: Int = 0,
    var distanceKm: Float? = null,
    var userName: String? = null,
    var userInitial: String? = null,
)