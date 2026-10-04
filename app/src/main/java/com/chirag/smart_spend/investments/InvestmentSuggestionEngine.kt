package com.chirag.smart_spend.investments

object InvestmentSuggestionEngine {

    fun getSuggestion(totalIncome: Double, totalExpense: Double): String {
        val netSavings = totalIncome - totalExpense

        if (totalIncome <= 0.0) {
            return "Add some income entries to get personalized suggestions."
        }

        val savingsRate = netSavings / totalIncome

        return when {
            netSavings < 0 -> {
                "Your expenses exceeded your income this month by ₹${-netSavings}. " +
                        "Consider reviewing your spending categories to find areas to cut back."
            }
            savingsRate < 0.10 -> {
                "You saved ₹$netSavings this month, which is under 10% of your income. " +
                        "Try building a small emergency fund before investing."
            }
            savingsRate < 0.30 -> {
                "Nice! You saved ₹$netSavings this month (${(savingsRate * 100).toInt()}% of income). " +
                        "Consider starting a recurring SIP in a mutual fund to grow this steadily."
            }
            else -> {
                "Excellent! You saved ₹$netSavings this month (${(savingsRate * 100).toInt()}% of income). " +
                        "You're in a strong position to increase your SIP amount or explore diversified investments."
            }
        }
    }
}