package com.example.lumka_app.model

// MonthlyBudget data class
data class MonthlyBudget(
    val id: String = "",        // Unique ID (if stored in Firebase)
    val userId: String = "",    // User ID
    val monthYear: String = "", // Format: "YYYY-MM"
    val amount: Double = 0.0   // Total budget amount for the month
)
