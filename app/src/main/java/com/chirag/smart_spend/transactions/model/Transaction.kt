package com.chirag.smart_spend.transactions.model

data class Transaction(
    var id: String = "",
    val userId: String = "",
    val amount: Double = 0.0,
    val type: String = "",       // "income" or "expense"
    val category: String = "",
    val note: String = "",
    val date: Long = System.currentTimeMillis(),
    val source: String = "manual"
)