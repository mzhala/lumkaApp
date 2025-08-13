package com.example.lumka_app.model

data class Category(
    val id: String = "",        // Unique identifier, e.g., Firebase key
    val userId: String = "",    // Reference to the user who owns this category
    val name: String = "",      // Name of the category (e.g., "Food")
    val iconUrl: String? = null,// Optional icon URL for visual representation
    val description: String? = null // Optional description of the category
)
