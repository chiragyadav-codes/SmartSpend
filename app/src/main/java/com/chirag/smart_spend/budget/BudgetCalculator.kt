package com.chirag.smart_spend.budget

import com.chirag.smart_spend.R
import com.chirag.smart_spend.reports.ComparisonReport
import com.chirag.smart_spend.reports.Insight
import com.chirag.smart_spend.reports.ReportDates
import com.chirag.smart_spend.transactions.model.Transaction
import java.util.Calendar
import kotlin.math.ceil

data class BudgetData(
    val monthlyBudget: Double?,
    val categoryBudgets: Map<String, Double>
)

data class CategoryProgress(
    val category: String,
    val limit: Double,
    val spent: Double
)

object BudgetCalculator {

    fun expenseTotal(list: List<Transaction>, start: Long, end: Long): Double {
        return list
            .filter { it.type == "expense" && it.date in start..end }
            .sumOf { it.amount }
    }

    fun expenseByCategory(list: List<Transaction>, start: Long, end: Long): Map<String, Double> {
        return list
            .filter { it.type == "expense" && it.date in start..end }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    // Average of the last three months that had spending, rounded up to the next 500.
    fun suggestedBudget(list: List<Transaction>, now: Calendar): Double? {
        val totals = mutableListOf<Double>()

        for (monthsAgo in 1..3) {
            val month = now.clone() as Calendar
            month.add(Calendar.MONTH, -monthsAgo)
            val total = expenseTotal(
                list,
                ReportDates.startOfMonth(month),
                ReportDates.endOfMonth(month)
            )
            if (total > 0) totals.add(total)
        }

        if (totals.isEmpty()) return null
        return ceil(totals.average() / 500.0) * 500.0
    }

    // Green until 75 percent, amber until the limit, red at or over it.
    fun statusColor(ratio: Double): Int {
        return when {
            ratio >= 1.0 -> R.color.sm_expense
            ratio >= 0.75 -> R.color.sm_warning
            else -> R.color.sm_income
        }
    }

    fun tips(
        spent: Double,
        monthlyBudget: Double?,
        dayOfMonth: Int,
        daysInMonth: Int,
        categories: List<CategoryProgress>
    ): List<Insight> {
        val result = mutableListOf<Insight>()
        val daysLeft = daysInMonth - dayOfMonth + 1

        if (monthlyBudget != null && monthlyBudget > 0) {
            val ratio = spent / monthlyBudget
            val percent = (ratio * 100).toInt()
            val evenPace = monthlyBudget * dayOfMonth / daysInMonth

            when {
                ratio >= 1.0 -> result.add(
                    Insight(
                        "You have used all of your monthly budget " +
                                "(₹${ComparisonReport.money(spent)} of ₹${ComparisonReport.money(monthlyBudget)}).",
                        false
                    )
                )
                ratio >= 0.8 -> result.add(
                    Insight(
                        "You have used $percent% of your monthly budget with $daysLeft days left. Slow down a little.",
                        false
                    )
                )
                spent <= evenPace -> result.add(
                    Insight(
                        "You are on track. ₹${ComparisonReport.money(spent)} spent so far, " +
                                "and an even pace would be ₹${ComparisonReport.money(evenPace)}.",
                        true
                    )
                )
                else -> result.add(
                    Insight(
                        "You are spending faster than an even pace: ₹${ComparisonReport.money(spent)} " +
                                "so far against ₹${ComparisonReport.money(evenPace)}.",
                        false
                    )
                )
            }
        }

        categories
            .filter { it.limit > 0 }
            .sortedByDescending { it.spent / it.limit }
            .take(3)
            .forEach { row ->
                val ratio = row.spent / row.limit
                when {
                    ratio >= 1.0 -> result.add(
                        Insight(
                            "${row.category} is over its budget by ₹${ComparisonReport.money(row.spent - row.limit)}.",
                            false
                        )
                    )
                    ratio >= 0.8 -> result.add(
                        Insight(
                            "${row.category} has used ${(ratio * 100).toInt()}% of its budget.",
                            false
                        )
                    )
                    else -> {}
                }
            }

        if (monthlyBudget == null && categories.isEmpty()) {
            result.add(Insight("Set a monthly budget to get tips here.", null))
        } else if (result.isEmpty()) {
            result.add(Insight("All your category budgets are on track.", true))
        }

        return result
    }
}