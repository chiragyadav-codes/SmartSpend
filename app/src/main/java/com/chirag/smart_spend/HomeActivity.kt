package com.chirag.smart_spend.home

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.chirag.smart_spend.R
import com.chirag.smart_spend.auth.LoginActivity
import com.chirag.smart_spend.reports.ReportRepository
import com.chirag.smart_spend.reports.MonthlySummaryActivity
import com.chirag.smart_spend.transactions.AddTransactionActivity
import com.chirag.smart_spend.transactions.TransactionListActivity
import java.util.Calendar

class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val reportRepository = ReportRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        auth = FirebaseAuth.getInstance()
        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)

        val userId = auth.currentUser?.uid
        if (userId != null) {
            FirebaseFirestore.getInstance().collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    val name = document.getString("name") ?: "User"
                    tvWelcome.text = "Welcome, $name"
                }
        }

        loadThisMonthSummary()

        findViewById<TextView>(R.id.btnLogout).setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        findViewById<androidx.cardview.widget.CardView>(R.id.cardAddTransaction).setOnClickListener {
            startActivity(Intent(this, AddTransactionActivity::class.java))
        }

        findViewById<androidx.cardview.widget.CardView>(R.id.cardViewTransactions).setOnClickListener {
            startActivity(Intent(this, TransactionListActivity::class.java))
        }

        findViewById<androidx.cardview.widget.CardView>(R.id.cardMonthlySummary).setOnClickListener {
            startActivity(Intent(this, MonthlySummaryActivity::class.java))
        }

        findViewById<androidx.cardview.widget.CardView>(R.id.cardNotificationAccess).setOnClickListener {
            startActivity(Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }

    private fun loadThisMonthSummary() {
        val calendar = Calendar.getInstance()
        reportRepository.getTransactionsForMonth(
            year = calendar.get(Calendar.YEAR),
            month = calendar.get(Calendar.MONTH),
            onResult = { transactions ->
                val income = reportRepository.getTotalIncome(transactions)
                val expense = reportRepository.getTotalExpense(transactions)
                val savings = income - expense

                findViewById<TextView>(R.id.tvHomeIncome).text = "₹$income"
                findViewById<TextView>(R.id.tvHomeExpense).text = "₹$expense"

                val tvSavings = findViewById<TextView>(R.id.tvHomeSavings)
                tvSavings.text = "₹$savings"
                tvSavings.setTextColor(
                    if (savings >= 0) android.graphics.Color.parseColor("#2E7D32") // green
                    else android.graphics.Color.parseColor("#C62828") // red
                )
            },
            onError = { }
        )
    }

    override fun onResume() {
        super.onResume()
        loadThisMonthSummary() // refresh when coming back from other screens
    }
}