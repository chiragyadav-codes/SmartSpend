package com.chirag.smart_spend.reports

data class CategorySummary(
    val category: String,
    val totalAmount: Double,
    val type: String // "income" or "expense"
)