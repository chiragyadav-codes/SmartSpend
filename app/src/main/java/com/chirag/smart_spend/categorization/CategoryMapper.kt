package com.chirag.smart_spend.categorization

object CategoryMapper {

    private val categoryKeywords = mapOf(
        "Food" to listOf("swiggy", "zomato", "dominos", "pizza", "restaurant", "cafe", "starbucks", "mcdonald"),
        "Transport" to listOf("uber", "ola", "rapido", "petrol", "fuel", "metro", "irctc", "redbus"),
        "Shopping" to listOf("amazon", "flipkart", "myntra", "ajio", "meesho"),
        "Entertainment" to listOf("netflix", "spotify", "hotstar", "prime video", "bookmyshow", "pvr"),
        "Bills" to listOf("electricity", "recharge", "airtel", "jio", "vodafone", "broadband", "wifi"),
        "Groceries" to listOf("bigbasket", "blinkit", "zepto", "dmart", "grocery"),
        "Salary" to listOf("salary", "payroll")
    )

    fun categorize(merchant: String): String {
        val merchantLower = merchant.lowercase()

        for ((category, keywords) in categoryKeywords) {
            for (keyword in keywords) {
                if (merchantLower.contains(keyword)) {
                    return category
                }
            }
        }

        return "Uncategorized"
    }
}