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

import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry

import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.components.XAxis

class MonthlySummaryActivity : AppCompatActivity() {

    private val repository = ReportRepository()
    private var currentCalendar = Calendar.getInstance()

    private lateinit var tvMonthYear: TextView
    private lateinit var tvTotalIncome: TextView
    private lateinit var tvTotalExpense: TextView
    private lateinit var tvNetSavings: TextView
    private lateinit var recyclerView: RecyclerView

    private lateinit var pieChart: PieChart
    private lateinit var barChart: BarChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_monthly_summary)

        tvMonthYear = findViewById(R.id.tvMonthYear)
        tvTotalIncome = findViewById(R.id.tvTotalIncome)
        tvTotalExpense = findViewById(R.id.tvTotalExpense)
        tvNetSavings = findViewById(R.id.tvNetSavings)
        recyclerView = findViewById(R.id.rvCategorySummary)
        pieChart = findViewById(R.id.pieChart)
        barChart = findViewById(R.id.barChart)
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
                updatePieChart(summaries)
                updateBarChart(totalIncome, totalExpense)
            },
            onError = { e ->
                Toast.makeText(this, "Failed to load: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun updatePieChart(summaries: List<CategorySummary>) {
        val expenseSummaries = summaries.filter { it.type == "expense" }

        if (expenseSummaries.isEmpty()) {
            pieChart.clear()
            return
        }

        val entries = expenseSummaries.map { PieEntry(it.totalAmount.toFloat(), it.category) }
        val dataSet = PieDataSet(entries, "Expenses by Category")
        dataSet.colors = listOf(
            android.graphics.Color.parseColor("#E57373"),
            android.graphics.Color.parseColor("#FFB74D"),
            android.graphics.Color.parseColor("#FFF176"),
            android.graphics.Color.parseColor("#81C784"),
            android.graphics.Color.parseColor("#64B5F6"),
            android.graphics.Color.parseColor("#BA68C8"),
            android.graphics.Color.parseColor("#4DB6AC")
        )

        val data = PieData(dataSet)
        pieChart.data = data
        pieChart.description.isEnabled = false
        pieChart.centerText = "Expenses"
        pieChart.invalidate() // refreshes the chart
    }

    private fun updateBarChart(totalIncome: Double, totalExpense: Double) {
        val entries = listOf(
            BarEntry(0f, totalIncome.toFloat()),
            BarEntry(1f, totalExpense.toFloat())
        )

        val dataSet = BarDataSet(entries, "Income vs Expense")
        dataSet.colors = listOf(
            android.graphics.Color.parseColor("#2E7D32"), // green for income
            android.graphics.Color.parseColor("#C62828")  // red for expense
        )

        val data = BarData(dataSet)
        data.barWidth = 0.5f

        barChart.data = data
        barChart.description.isEnabled = false
        barChart.legend.isEnabled = false

        val labels = listOf("Income", "Expense")
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        barChart.xAxis.granularity = 1f
        barChart.xAxis.setDrawGridLines(false)

        barChart.axisRight.isEnabled = false
        barChart.invalidate()
    }
}