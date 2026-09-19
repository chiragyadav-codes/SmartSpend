package com.chirag.smart_spend.reports

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.chirag.smart_spend.transactions.model.Transaction
import java.util.Calendar

class ReportRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /**
     * Fetches all transactions for the logged-in user within the given month/year,
     * then calls onResult with the list. Pass month as 0-11 (Calendar.MONTH style).
     */
    fun getTransactionsForMonth(
        year: Int,
        month: Int,
        onResult: (List<Transaction>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            onError(Exception("Not logged in"))
            return
        }

        val startCal = Calendar.getInstance()
        startCal.set(year, month, 1, 0, 0, 0)
        val startMillis = startCal.timeInMillis

        val endCal = Calendar.getInstance()
        endCal.set(year, month, startCal.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
        val endMillis = endCal.timeInMillis

        db.collection("transactions")
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("date", startMillis)
            .whereLessThanOrEqualTo("date", endMillis)
            .get()
            .addOnSuccessListener { result ->
                val transactions = mutableListOf<Transaction>()
                for (document in result) {
                    val transaction = document.toObject(Transaction::class.java)
                    transaction.id = document.id
                    transactions.add(transaction)
                }
                onResult(transactions)
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }

    /**
     * Groups a list of transactions by category, summing amounts per category,
     * separately for income and expense.
     */
    fun groupByCategory(transactions: List<Transaction>): List<CategorySummary> {
        return transactions
            .groupBy { it.category to it.type }
            .map { (key, txns) ->
                val (category, type) = key
                CategorySummary(
                    category = category,
                    totalAmount = txns.sumOf { it.amount },
                    type = type
                )
            }
            .sortedByDescending { it.totalAmount }
    }

    fun getTotalIncome(transactions: List<Transaction>): Double {
        return transactions.filter { it.type == "income" }.sumOf { it.amount }
    }

    fun getTotalExpense(transactions: List<Transaction>): Double {
        return transactions.filter { it.type == "expense" }.sumOf { it.amount }
    }
}