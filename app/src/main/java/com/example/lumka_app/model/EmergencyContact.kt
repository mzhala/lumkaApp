package com.example.lumka_app

data class EmergencyContact(
    var id: String? = null,
    val userId: String? = null,      // the owner of the contact list
    val name: String = "",
    val phoneNumber: String = "",
    val isMain: Boolean = false,      // true if this is the default contact
    val timestamp: Long = System.currentTimeMillis()
)
