package com.chirag.smart_spend.budget

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.chirag.smart_spend.R
import com.chirag.smart_spend.reports.ComparisonReport
import com.chirag.smart_spend.reports.ReportRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object BudgetAlertChecker {

    private const val CHANNEL_ID = "budget_alerts"
    private const val PREFS_NAME = "smartspend_prefs"
    private const val KEY_ENABLED = "budget_alerts_enabled"

    fun isEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, true)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
    }

    // Looks at this month's spending against the saved budgets and sends
    // a notification if a budget has just reached 80% or 100%.
    fun check(context: Context) {
        val appContext = context.applicationContext
        if (!isEnabled(appContext)) return
        if (!NotificationManagerCompat.from(appContext).areNotificationsEnabled()) return

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance().collection("users").document(uid)
            .get()
            .addOnSuccessListener { document ->
                val monthly = (document.get("monthlyBudget") as? Number)?.toDouble()

                val categories = HashMap<String, Double>()
                (document.get("categoryBudgets") as? Map<*, *>)?.forEach { (key, value) ->
                    val limit = (value as? Number)?.toDouble()
                    if (key is String && limit != null) {
                        categories[key] = limit
                    }
                }

                if (monthly == null && categories.isEmpty()) return@addOnSuccessListener

                val now = Calendar.getInstance()
                ReportRepository().getTransactionsForMonth(
                    year = now.get(Calendar.YEAR),
                    month = now.get(Calendar.MONTH),
                    onResult = { transactions ->
                        val expenses = transactions.filter { it.type == "expense" }
                        val spent = expenses.sumOf { it.amount }
                        val byCategory = expenses
                            .groupBy { it.category }
                            .mapValues { entry -> entry.value.sumOf { it.amount } }

                        if (monthly != null) {
                            alertIfNeeded(appContext, uid, now, "monthly", "monthly budget", spent, monthly)
                        }
                        for ((category, limit) in categories) {
                            alertIfNeeded(
                                appContext, uid, now,
                                "cat_$category", "$category budget",
                                byCategory[category] ?: 0.0, limit
                            )
                        }
                    },
                    onError = { }
                )
            }
    }

    private fun alertIfNeeded(
        context: Context,
        uid: String,
        now: Calendar,
        scope: String,
        label: String,
        spent: Double,
        limit: Double
    ) {
        if (limit <= 0) return

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val monthKey = SimpleDateFormat("yyyyMM", Locale.US).format(now.time)
        val key = "budget_alert_${uid}_${monthKey}_$scope"

        val ratio = spent / limit

        // Back under 80% (for example the budget was raised): allow alerts again.
        if (ratio < 0.8) {
            if (prefs.contains(key)) prefs.edit().remove(key).apply()
            return
        }

        val level = if (ratio >= 1.0) 100 else 80
        if (level <= prefs.getInt(key, 0)) return
        prefs.edit().putInt(key, level).apply()

        val title = if (level == 100) "Budget exceeded" else "Budget alert"
        val text = if (level == 100) {
            "You have crossed your $label: ₹${ComparisonReport.money(spent)} spent of " +
                    "₹${ComparisonReport.money(limit)}."
        } else {
            "You have used ${(ratio * 100).toInt()}% of your $label " +
                    "(₹${ComparisonReport.money(spent)} of ₹${ComparisonReport.money(limit)}). " +
                    "₹${ComparisonReport.money(limit - spent)} left."
        }

        send(context, scope.hashCode(), title, text)
    }

    private fun send(context: Context, id: Int, title: String, text: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Budget alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = "Warnings when you reach 80% and 100% of a budget"
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(context, BudgetPlannerActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP

        val pending = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_budget)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        manager.notify(id, notification)
    }
}