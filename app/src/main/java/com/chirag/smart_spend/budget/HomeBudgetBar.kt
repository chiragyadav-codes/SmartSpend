package com.chirag.smart_spend.budget

import android.content.Intent
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.chirag.smart_spend.R
import com.chirag.smart_spend.reports.ComparisonReport
import com.chirag.smart_spend.reports.ReportDates
import com.chirag.smart_spend.transactions.model.Transaction
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import kotlin.math.min

object HomeBudgetBar {

    fun update(root: View, transactions: List<Transaction>) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val context = root.context

        val card = root.findViewById<View>(R.id.cardBudget) ?: return
        val tvAmount = root.findViewById<TextView>(R.id.tvHomeBudgetAmount)
        val tvHint = root.findViewById<TextView>(R.id.tvHomeBudgetHint)
        val progress = root.findViewById<LinearProgressIndicator>(R.id.progressHomeBudget)

        card.setOnClickListener {
            context.startActivity(Intent(context, BudgetPlannerActivity::class.java))
        }

        FirebaseFirestore.getInstance().collection("users").document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (!root.isAttachedToWindow) return@addOnSuccessListener

                val limit = (document.get("monthlyBudget") as? Number)?.toDouble()

                if (limit == null || limit <= 0) {
                    tvAmount.text = "Not set"
                    tvHint.text = "Tap to set a monthly spending limit"
                    tvHint.setTextColor(ContextCompat.getColor(context, R.color.sm_text_secondary))
                    progress.visibility = View.GONE
                    return@addOnSuccessListener
                }

                val now = Calendar.getInstance()
                val spent = BudgetCalculator.expenseTotal(
                    transactions,
                    ReportDates.startOfMonth(now),
                    ReportDates.endOfMonth(now)
                )
                val ratio = spent / limit
                val color = ContextCompat.getColor(context, BudgetCalculator.statusColor(ratio))

                tvAmount.text = "₹${ComparisonReport.money(spent)} of ₹${ComparisonReport.money(limit)}"

                progress.visibility = View.VISIBLE
                progress.setProgressCompat(min(100, (ratio * 100).toInt()), true)
                progress.setIndicatorColor(color)

                val remaining = limit - spent
                tvHint.text = if (remaining >= 0) {
                    "₹${ComparisonReport.money(remaining)} left this month"
                } else {
                    "Over budget by ₹${ComparisonReport.money(-remaining)}"
                }
                tvHint.setTextColor(color)
            }
    }
}