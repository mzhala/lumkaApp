package com.example.lumka_app.model

data class Transaction(
    var id: String = "",
    var amount: Double = 0.0,
    var category: String = "",
    var type: String = "", // "income" or "expense"
    var date: String = "",
    var uid: String = "",
    var url: String = ""
)
