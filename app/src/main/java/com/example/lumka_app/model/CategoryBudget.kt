package com.example.lumka_app.model

// CategoryBudget data class
data class CategoryBudget(
    val id: String = "",   // Unique ID
    val userId: String = "",    // User ID
    val monthYear: String = "", // Format: "YYYY-MM"
    val category: String = "",  // Category name (e.g., Food, Travel)
    val amount: Double = 0.0   // Budget allocated for this category
)
