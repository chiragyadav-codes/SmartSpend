package com.chirag.smart_spend.notification

class NotificationParser {

    fun parse(text: String): ParsedTransaction? {
        // Matches: "Rs.501 debited from A/c XX1234 to Swiggy on 17-09-26"
        // Also handles "credited" for income
        val regex = Regex(
            """Rs\.?\s?(\d+(?:\.\d+)?)\s+(debited|credited).*?to\s+([A-Za-z0-9 ]+?)\s+on""",
            RegexOption.IGNORE_CASE
        )

        val match = regex.find(text) ?: return null

        val amount = match.groupValues[1].toDoubleOrNull() ?: return null
        val action = match.groupValues[2].lowercase()
        val merchant = match.groupValues[3].trim()

        val type = if (action == "credited") "income" else "expense"

        return ParsedTransaction(
            amount = amount,
            merchant = merchant,
            type = type
        )
    }
}