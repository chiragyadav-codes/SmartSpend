package com.chirag.smart_spend.reports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.chirag.smart_spend.R
import com.chirag.smart_spend.transactions.model.Transaction
import com.github.mikephil.charting.charts.BarChart
import com.google.android.material.button.MaterialButtonToggleGroup
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TrendsFragment : Fragment() {

    private lateinit var viewModel: ChartsViewModel

    private lateinit var toggleMode: MaterialButtonToggleGroup
    private lateinit var btnPrev: Button
    private lateinit var btnNext: Button
    private lateinit var tvPeriod: TextView
    private lateinit var tvCategoryChooser: TextView
    private lateinit var tvSummary: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var barChart: BarChart

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_trends, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireParentFragment())[ChartsViewModel::class.java]

        toggleMode = view.findViewById(R.id.toggleMode)
        btnPrev = view.findViewById(R.id.btnTrendPrev)
        btnNext = view.findViewById(R.id.btnTrendNext)
        tvPeriod = view.findViewById(R.id.tvTrendPeriod)
        tvCategoryChooser = view.findViewById(R.id.tvCategoryChooser)
        tvSummary = view.findViewById(R.id.tvTrendSummary)
        tvEmpty = view.findViewById(R.id.tvTrendEmpty)
        barChart = view.findViewById(R.id.trendBarChart)

        val checkedId = when (viewModel.trendMode) {
            0 -> R.id.btnModeDay
            2 -> R.id.btnModeYear
            else -> R.id.btnModeMonth
        }
        toggleMode.check(checkedId)

        toggleMode.addOnButtonCheckedListener { _, id, isChecked ->
            if (isChecked) {
                viewModel.trendMode = when (id) {
                    R.id.btnModeDay -> 0
                    R.id.btnModeYear -> 2
                    else -> 1
                }
                render()
            }
        }

        btnPrev.setOnClickListener { moveAnchor(-1) }
        btnNext.setOnClickListener { moveAnchor(1) }
        tvCategoryChooser.setOnClickListener { showCategoryDialog() }

        viewModel.transactions.observe(viewLifecycleOwner) { render() }
    }

    private fun moveAnchor(step: Int) {
        when (viewModel.trendMode) {
            0 -> viewModel.trendAnchor.add(Calendar.MONTH, step)
            1 -> viewModel.trendAnchor.add(Calendar.YEAR, step)
        }
        render()
    }

    private fun showCategoryDialog() {
        val all = viewModel.transactions.value ?: emptyList()
        val categories = all.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()
        val options = (listOf("All categories") + categories).toTypedArray()

        val current = viewModel.trendCategory
        val checked = if (current == null) 0 else (categories.indexOf(current) + 1).coerceAtLeast(0)

        AlertDialog.Builder(requireContext())
            .setTitle("Category")
            .setSingleChoiceItems(options, checked) { dialog, which ->
                viewModel.trendCategory = if (which == 0) null else categories[which - 1]
                dialog.dismiss()
                render()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun render() {
        if (!isAdded) return

        val all = viewModel.transactions.value ?: emptyList()
        val category = viewModel.trendCategory
        val filtered = if (category == null) all else all.filter { it.category == category }

        tvCategoryChooser.text = "Category: ${category ?: "All"}  ▾"

        val mode = viewModel.trendMode
        val anchor = viewModel.trendAnchor

        val navVisibility = if (mode == 2) View.INVISIBLE else View.VISIBLE
        btnPrev.visibility = navVisibility
        btnNext.visibility = navVisibility

        val data = when (mode) {
            0 -> dayData(filtered, anchor)
            1 -> monthData(filtered, anchor)
            else -> yearData(filtered)
        }

        tvPeriod.text = data.title

        val totalIncome = data.income.sum()
        val totalExpense = data.expense.sum()
        tvSummary.text = "Income ₹$totalIncome   |   Expense ₹$totalExpense"

        val hasData = data.income.any { it != 0.0 } || data.expense.any { it != 0.0 }
        if (!hasData) {
            barChart.visibility = View.GONE
            tvEmpty.visibility = View.VISIBLE
        } else {
            tvEmpty.visibility = View.GONE
            barChart.visibility = View.VISIBLE
            ChartHelper.showGroupedBars(
                context = requireContext(),
                chart = barChart,
                labels = data.labels,
                incomeValues = data.income.map { it.toFloat() },
                expenseValues = data.expense.map { it.toFloat() }
            )
        }
    }

    // One group of bars for each day of the month shown.
    private fun dayData(list: List<Transaction>, anchor: Calendar): TrendData {
        val start = ReportDates.startOfMonth(anchor)
        val end = ReportDates.endOfMonth(anchor)
        val days = anchor.getActualMaximum(Calendar.DAY_OF_MONTH)
        val income = DoubleArray(days)
        val expense = DoubleArray(days)
        val cal = Calendar.getInstance()

        for (t in list) {
            if (t.date in start..end) {
                cal.timeInMillis = t.date
                val index = cal.get(Calendar.DAY_OF_MONTH) - 1
                if (t.type == "income") income[index] += t.amount else expense[index] += t.amount
            }
        }

        val labels = (1..days).map { day -> if (day == 1 || day % 5 == 0) day.toString() else "" }
        val title = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(anchor.time)
        return TrendData(labels, income.toList(), expense.toList(), title)
    }

    // One group of bars for each month of the year shown.
    private fun monthData(list: List<Transaction>, anchor: Calendar): TrendData {
        val year = anchor.get(Calendar.YEAR)
        val income = DoubleArray(12)
        val expense = DoubleArray(12)
        val cal = Calendar.getInstance()

        for (t in list) {
            cal.timeInMillis = t.date
            if (cal.get(Calendar.YEAR) == year) {
                val index = cal.get(Calendar.MONTH)
                if (t.type == "income") income[index] += t.amount else expense[index] += t.amount
            }
        }

        val labels = DateFormatSymbols(Locale.getDefault()).shortMonths.take(12)
        return TrendData(labels, income.toList(), expense.toList(), year.toString())
    }

    // One group of bars for each year that has data.
    private fun yearData(list: List<Transaction>): TrendData {
        if (list.isEmpty()) {
            return TrendData(emptyList(), emptyList(), emptyList(), "All years")
        }

        val cal = Calendar.getInstance()
        cal.timeInMillis = list.minOf { it.date }
        val firstYear = cal.get(Calendar.YEAR)
        cal.timeInMillis = list.maxOf { it.date }
        val lastYear = cal.get(Calendar.YEAR)

        val count = lastYear - firstYear + 1
        val income = DoubleArray(count)
        val expense = DoubleArray(count)

        for (t in list) {
            cal.timeInMillis = t.date
            val index = cal.get(Calendar.YEAR) - firstYear
            if (t.type == "income") income[index] += t.amount else expense[index] += t.amount
        }

        val labels = (firstYear..lastYear).map { it.toString() }
        return TrendData(labels, income.toList(), expense.toList(), "All years")
    }
}

private data class TrendData(
    val labels: List<String>,
    val income: List<Double>,
    val expense: List<Double>,
    val title: String
)