package com.chirag.smart_spend.reports

import com.chirag.smart_spend.transactions.model.Transaction
import java.util.Locale
import kotlin.math.abs

data class CompareRow(
    val title: String,
    val current: Double,
    val previous: Double,
    val higherIsBetter: Boolean
)

data class Insight(
    val text: String,
    val good: Boolean?
)

object ComparisonReport {

    fun money(value: Double): String {
        return if (value % 1.0 == 0.0) {
            String.format(Locale.getDefault(), "%.0f", value)
        } else {
            String.format(Locale.getDefault(), "%.2f", value)
        }
    }

    fun categoryRows(current: List<Transaction>, previous: List<Transaction>): List<CompareRow> {
        val currentTotals = expenseByCategory(current)
        val previousTotals = expenseByCategory(previous)

        return (currentTotals.keys + previousTotals.keys)
            .map { category ->
                CompareRow(
                    title = category,
                    current = currentTotals[category] ?: 0.0,
                    previous = previousTotals[category] ?: 0.0,
                    higherIsBetter = false
                )
            }
            .sortedByDescending { abs(it.current - it.previous) }
    }

    fun insights(
        currentIncome: Double,
        currentExpense: Double,
        previousIncome: Double,
        previousExpense: Double,
        categoryRows: List<CompareRow>,
        previousLabel: String
    ): List<Insight> {
        val result = mutableListOf<Insight>()

        // Overall spending
        val spendingDiff = currentExpense - previousExpense
        if (currentExpense > 0 && previousExpense > 0) {
            val percent = percentText(spendingDiff, previousExpense)
            when {
                spendingDiff > 0 -> result.add(
                    Insight("Total spending is up $percent% (₹${money(spendingDiff)} more) than $previousLabel.", false)
                )
                spendingDiff < 0 -> result.add(
                    Insight("Total spending is down $percent% (₹${money(-spendingDiff)} less) than $previousLabel.", true)
                )
                else -> result.add(Insight("Total spending is the same as $previousLabel.", null))
            }
        } else if (currentExpense > 0) {
            result.add(Insight("You spent ₹${money(currentExpense)}. Nothing was recorded for $previousLabel.", null))
        } else if (previousExpense > 0) {
            result.add(Insight("No spending recorded here, compared with ₹${money(previousExpense)} in $previousLabel.", true))
        }

        // Savings
        val savedDiff = (currentIncome - currentExpense) - (previousIncome - previousExpense)
        if (abs(savedDiff) >= 1 && (currentIncome > 0 || previousIncome > 0)) {
            if (savedDiff > 0) {
                result.add(Insight("You saved ₹${money(savedDiff)} more than $previousLabel.", true))
            } else {
                result.add(Insight("You saved ₹${money(-savedDiff)} less than $previousLabel.", false))
            }
        }

        // Income
        val incomeDiff = currentIncome - previousIncome
        if (abs(incomeDiff) >= 1 && currentIncome > 0 && previousIncome > 0) {
            if (incomeDiff > 0) {
                result.add(Insight("Income is up ₹${money(incomeDiff)} compared with $previousLabel.", true))
            } else {
                result.add(Insight("Income is down ₹${money(-incomeDiff)} compared with $previousLabel.", false))
            }
        }

        // Categories: only changes that matter (at least 1 rupee and 5 percent)
        val significant = categoryRows.filter { row ->
            val diff = row.current - row.previous
            when {
                abs(diff) < 1.0 -> false
                row.previous == 0.0 || row.current == 0.0 -> true
                else -> abs(diff) / row.previous * 100 >= 5
            }
        }

        significant.filter { it.current > it.previous }.take(3).forEach {
            result.add(categoryInsight(it, previousLabel))
        }
        significant.filter { it.current < it.previous }.take(3).forEach {
            result.add(categoryInsight(it, previousLabel))
        }

        if (result.isEmpty()) {
            result.add(Insight("Nothing notable changed.", null))
        }

        return result
    }

    private fun categoryInsight(row: CompareRow, previousLabel: String): Insight {
        val diff = row.current - row.previous
        return when {
            row.previous == 0.0 -> Insight(
                "New spending on ${row.title}: ₹${money(row.current)} (nothing in $previousLabel).",
                null
            )
            row.current == 0.0 -> Insight(
                "No spending on ${row.title}, it was ₹${money(row.previous)} in $previousLabel.",
                true
            )
            diff > 0 -> Insight(
                "You spent ₹${money(diff)} more on ${row.title} (+${percentText(diff, row.previous)}%) than in $previousLabel.",
                false
            )
            else -> Insight(
                "You spent ₹${money(-diff)} less on ${row.title} (-${percentText(diff, row.previous)}%) than in $previousLabel.",
                true
            )
        }
    }

    private fun expenseByCategory(list: List<Transaction>): Map<String, Double> {
        return list
            .filter { it.type == "expense" }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    private fun percentText(part: Double, whole: Double): String {
        val percent = abs(part) / whole * 100
        return if (percent < 1) "<1" else percent.toInt().toString()
    }
}