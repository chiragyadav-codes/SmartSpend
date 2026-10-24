package com.chirag.smart_spend.reports

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chirag.smart_spend.R
import com.chirag.smart_spend.transactions.AddTransactionActivity
import com.chirag.smart_spend.transactions.TransactionAdapter
import com.github.mikephil.charting.charts.PieChart
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class DayPieFragment : Fragment() {

    private val repository = ReportRepository()
    private lateinit var viewModel: ChartsViewModel

    private lateinit var tvDay: TextView
    private lateinit var tvDayExpense: TextView
    private lateinit var tvDayIncome: TextView
    private lateinit var tvEmptyState: TextView
    private lateinit var pieChart: PieChart
    private lateinit var layoutDayLists: LinearLayout
    private lateinit var rvCategories: RecyclerView
    private lateinit var rvTransactions: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_day_pie, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireParentFragment())[ChartsViewModel::class.java]

        tvDay = view.findViewById(R.id.tvDay)
        tvDayExpense = view.findViewById(R.id.tvDayExpense)
        tvDayIncome = view.findViewById(R.id.tvDayIncome)
        tvEmptyState = view.findViewById(R.id.tvEmptyState)
        pieChart = view.findViewById(R.id.pieChart)
        layoutDayLists = view.findViewById(R.id.layoutDayLists)
        rvCategories = view.findViewById(R.id.rvDayCategories)
        rvTransactions = view.findViewById(R.id.rvDayTransactions)
        rvCategories.layoutManager = LinearLayoutManager(requireContext())
        rvTransactions.layoutManager = LinearLayoutManager(requireContext())

        view.findViewById<Button>(R.id.btnPrevDay).setOnClickListener {
            viewModel.selectedDay.add(Calendar.DAY_OF_MONTH, -1)
            render()
        }

        view.findViewById<Button>(R.id.btnNextDay).setOnClickListener {
            viewModel.selectedDay.add(Calendar.DAY_OF_MONTH, 1)
            render()
        }

        tvDay.setOnClickListener { showDayPicker() }

        viewModel.transactions.observe(viewLifecycleOwner) { render() }
    }

    private fun showDayPicker() {
        // The picker works in UTC, so build the current day as midnight UTC.
        val day = viewModel.selectedDay
        val utcStart = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utcStart.clear()
        utcStart.set(day.get(Calendar.YEAR), day.get(Calendar.MONTH), day.get(Calendar.DAY_OF_MONTH))

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select day")
            .setSelection(utcStart.timeInMillis)
            .build()

        picker.addOnPositiveButtonClickListener { utcMillis ->
            val picked = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            picked.timeInMillis = utcMillis
            viewModel.selectedDay.set(
                picked.get(Calendar.YEAR),
                picked.get(Calendar.MONTH),
                picked.get(Calendar.DAY_OF_MONTH)
            )
            render()
        }

        picker.show(childFragmentManager, "day_picker")
    }

    private fun render() {
        if (!isAdded) return

        val all = viewModel.transactions.value ?: emptyList()
        val day = viewModel.selectedDay

        tvDay.text = SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(day.time)

        val start = ReportDates.startOfDay(day)
        val end = ReportDates.endOfDay(day)
        val inDay = all.filter { it.date in start..end }

        val income = repository.getTotalIncome(inDay)
        val expense = repository.getTotalExpense(inDay)
        tvDayExpense.text = "Spent: ₹$expense"
        tvDayIncome.text = "Earned: ₹$income"

        val summaries = repository.groupByCategory(inDay)
        rvCategories.adapter = CategorySummaryAdapter(summaries)
        rvTransactions.adapter = TransactionAdapter(
            transactions = inDay,
            onItemClick = { transaction ->
                val intent = Intent(requireContext(), AddTransactionActivity::class.java)
                intent.putExtra("transactionId", transaction.id)
                startActivity(intent)
            },
            onItemLongClick = { }
        )

        layoutDayLists.visibility = if (inDay.isEmpty()) View.GONE else View.VISIBLE

        val expenseSummaries = summaries.filter { it.type == "expense" }
        if (expenseSummaries.isEmpty()) {
            pieChart.visibility = View.GONE
            tvEmptyState.text = if (inDay.isEmpty()) {
                "No transactions on this day."
            } else {
                "No expenses on this day."
            }
            tvEmptyState.visibility = View.VISIBLE
        } else {
            tvEmptyState.visibility = View.GONE
            pieChart.visibility = View.VISIBLE
            ChartHelper.showExpensePie(requireContext(), pieChart, expenseSummaries, "Spent\n₹$expense")
        }
    }
}