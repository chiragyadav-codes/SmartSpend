package com.chirag.smart_spend.reports

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chirag.smart_spend.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MonthlySummaryActivity : AppCompatActivity() {

    private val repository = ReportRepository()
    private var currentCalendar = Calendar.getInstance()

    private lateinit var tvMonthYear: TextView
    private lateinit var tvTotalIncome: TextView
    private lateinit var tvTotalExpense: TextView
    private lateinit var tvNetSavings: TextView
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_monthly_summary)

        tvMonthYear = findViewById(R.id.tvMonthYear)
        tvTotalIncome = findViewById(R.id.tvTotalIncome)
        tvTotalExpense = findViewById(R.id.tvTotalExpense)
        tvNetSavings = findViewById(R.id.tvNetSavings)
        recyclerView = findViewById(R.id.rvCategorySummary)
        recyclerView.layoutManager = LinearLayoutManager(this)

        findViewById<Button>(R.id.btnPrevMonth).setOnClickListener {
            currentCalendar.add(Calendar.MONTH, -1)
            loadMonthData()
        }

        findViewById<Button>(R.id.btnNextMonth).setOnClickListener {
            currentCalendar.add(Calendar.MONTH, 1)
            loadMonthData()
        }

        loadMonthData()
    }

    private fun loadMonthData() {
        val year = currentCalendar.get(Calendar.YEAR)
        val month = currentCalendar.get(Calendar.MONTH)

        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        tvMonthYear.text = monthFormat.format(currentCalendar.time)

        repository.getTransactionsForMonth(
            year = year,
            month = month,
            onResult = { transactions ->
                val totalIncome = repository.getTotalIncome(transactions)
                val totalExpense = repository.getTotalExpense(transactions)
                val netSavings = totalIncome - totalExpense

                tvTotalIncome.text = "Income: ₹$totalIncome"
                tvTotalExpense.text = "Expense: ₹$totalExpense"
                tvNetSavings.text = "Net Savings: ₹$netSavings"

                val summaries = repository.groupByCategory(transactions)
                recyclerView.adapter = CategorySummaryAdapter(summaries)
            },
            onError = { e ->
                Toast.makeText(this, "Failed to load: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }
}