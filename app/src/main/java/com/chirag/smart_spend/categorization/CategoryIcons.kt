package com.chirag.smart_spend.categorization

object CategoryIcons {

    val expense = listOf(
        "Food" to "🍔",
        "Transport" to "🚗",
        "Shopping" to "🛍️",
        "Bills" to "💡",
        "Groceries" to "🥦",
        "Entertainment" to "🎬",
        "Health" to "💊",
        "Education" to "📚",
        "Other" to "✨"
    )

    val income = listOf(
        "Salary" to "💼",
        "Freelance" to "💻",
        "Investment" to "📈",
        "Gift" to "🎁",
        "Other" to "✨"
    )

    fun iconFor(category: String): String? {
        return (expense + income)
            .firstOrNull { it.first.equals(category, ignoreCase = true) }
            ?.second
    }
}