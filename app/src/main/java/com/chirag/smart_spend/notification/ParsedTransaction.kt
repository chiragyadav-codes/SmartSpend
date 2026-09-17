package com.chirag.smart_spend.notification

data class ParsedTransaction(
    val amount: Double,
    val merchant: String,
    val type: String // "income" or "expense"
)