package com.example.lumka_app

data class Incident(
    val id: String? = null,
    val userId: String? = null,
    val timestamp: Long? = null,        // epoch millis
    val title: String = "",
    val description: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val likes: Int = 0
)
