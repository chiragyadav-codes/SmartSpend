package com.chirag.smart_spend.reports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chirag.smart_spend.R
import com.github.mikephil.charting.charts.PieChart
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MonthPieFragment : Fragment() {

    private val repository = ReportRepository()
    private lateinit var viewModel: ChartsViewModel

    private lateinit var tvMonthYear: TextView
    private lateinit var tvTotalIncome: TextView
    private lateinit var tvTotalExpense: TextView
    private lateinit var tvNetSavings: TextView
    private lateinit var tvEmptyState: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var pieChart: PieChart

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_month_pie, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireParentFragment())[ChartsViewModel::class.java]

        tvMonthYear = view.findViewById(R.id.tvMonthYear)
        tvTotalIncome = view.findViewById(R.id.tvTotalIncome)
        tvTotalExpense = view.findViewById(R.id.tvTotalExpense)
        tvNetSavings = view.findViewById(R.id.tvNetSavings)
        tvEmptyState = view.findViewById(R.id.tvEmptyState)
        recyclerView = view.findViewById(R.id.rvCategorySummary)
        pieChart = view.findViewById(R.id.pieChart)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        view.findViewById<Button>(R.id.btnPrevMonth).setOnClickListener {
            viewModel.selectedMonth.add(Calendar.MONTH, -1)
            render()
        }

        view.findViewById<Button>(R.id.btnNextMonth).setOnClickListener {
            viewModel.selectedMonth.add(Calendar.MONTH, 1)
            render()
        }

        viewModel.transactions.observe(viewLifecycleOwner) { render() }

        viewModel.error.observe(viewLifecycleOwner) { message ->
            if (message != null) {
                Toast.makeText(requireContext(), "Failed to load: $message", Toast.LENGTH_SHORT).show()
                viewModel.error.value = null
            }
        }
    }

    private fun render() {
        if (!isAdded) return

        val all = viewModel.transactions.value ?: emptyList()
        val month = viewModel.selectedMonth

        tvMonthYear.text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(month.time)

        val start = ReportDates.startOfMonth(month)
        val end = ReportDates.endOfMonth(month)
        val inMonth = all.filter { it.date in start..end }

        val income = repository.getTotalIncome(inMonth)
        val expense = repository.getTotalExpense(inMonth)
        val net = income - expense

        tvTotalIncome.text = "Income: ₹$income"
        tvTotalExpense.text = "Expense: ₹$expense"
        tvNetSavings.text = "Net Savings: ₹$net"

        val summaries = repository.groupByCategory(inMonth)
        recyclerView.adapter = CategorySummaryAdapter(summaries)

        val expenseSummaries = summaries.filter { it.type == "expense" }
        if (expenseSummaries.isEmpty()) {
            pieChart.visibility = View.GONE
            tvEmptyState.text = if (inMonth.isEmpty()) {
                "No transactions this month yet."
            } else {
                "No expenses this month."
            }
            tvEmptyState.visibility = View.VISIBLE
        } else {
            tvEmptyState.visibility = View.GONE
            pieChart.visibility = View.VISIBLE
            ChartHelper.showExpensePie(requireContext(), pieChart, expenseSummaries, "Expenses\n₹$expense")
        }
    }
}