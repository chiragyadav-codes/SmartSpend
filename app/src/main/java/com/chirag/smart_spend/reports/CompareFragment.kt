package com.chirag.smart_spend.reports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chirag.smart_spend.R
import com.google.android.material.button.MaterialButtonToggleGroup
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CompareFragment : Fragment() {

    private val repository = ReportRepository()
    private lateinit var viewModel: ChartsViewModel

    private lateinit var toggle: MaterialButtonToggleGroup
    private lateinit var tvTitle: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var layoutReport: LinearLayout
    private lateinit var layoutInsights: LinearLayout
    private lateinit var rvSummary: RecyclerView
    private lateinit var rvCategories: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_compare, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireParentFragment())[ChartsViewModel::class.java]

        toggle = view.findViewById(R.id.toggleCompare)
        tvTitle = view.findViewById(R.id.tvCompareTitle)
        tvEmpty = view.findViewById(R.id.tvCompareEmpty)
        layoutReport = view.findViewById(R.id.layoutReport)
        layoutInsights = view.findViewById(R.id.layoutInsights)
        rvSummary = view.findViewById(R.id.rvCompareSummary)
        rvCategories = view.findViewById(R.id.rvCompareCategories)
        rvSummary.layoutManager = LinearLayoutManager(requireContext())
        rvCategories.layoutManager = LinearLayoutManager(requireContext())

        toggle.check(if (viewModel.compareMode == 1) R.id.btnCompareYear else R.id.btnCompareMonth)
        toggle.addOnButtonCheckedListener { _, id, isChecked ->
            if (isChecked) {
                viewModel.compareMode = if (id == R.id.btnCompareYear) 1 else 0
                render()
            }
        }

        view.findViewById<Button>(R.id.btnComparePrev).setOnClickListener { moveAnchor(-1) }
        view.findViewById<Button>(R.id.btnCompareNext).setOnClickListener { moveAnchor(1) }

        viewModel.transactions.observe(viewLifecycleOwner) { render() }
    }

    private fun moveAnchor(step: Int) {
        val field = if (viewModel.compareMode == 1) Calendar.YEAR else Calendar.MONTH
        viewModel.compareAnchor.add(field, step)
        render()
    }

    private fun render() {
        if (!isAdded) return

        val all = viewModel.transactions.value ?: emptyList()
        val yearMode = viewModel.compareMode == 1

        val current = viewModel.compareAnchor
        val previous = current.clone() as Calendar
        previous.add(if (yearMode) Calendar.YEAR else Calendar.MONTH, -1)

        val currentStart = if (yearMode) ReportDates.startOfYear(current) else ReportDates.startOfMonth(current)
        val currentEnd = if (yearMode) ReportDates.endOfYear(current) else ReportDates.endOfMonth(current)
        val previousStart = if (yearMode) ReportDates.startOfYear(previous) else ReportDates.startOfMonth(previous)
        val previousEnd = if (yearMode) ReportDates.endOfYear(previous) else ReportDates.endOfMonth(previous)

        val currentList = all.filter { it.date in currentStart..currentEnd }
        val previousList = all.filter { it.date in previousStart..previousEnd }

        val longFormat = SimpleDateFormat(if (yearMode) "yyyy" else "MMMM yyyy", Locale.getDefault())
        val shortFormat = SimpleDateFormat(if (yearMode) "yyyy" else "MMM yyyy", Locale.getDefault())
        val currentLabel = shortFormat.format(current.time)
        val previousLabel = shortFormat.format(previous.time)

        tvTitle.text = "${longFormat.format(current.time)} vs ${longFormat.format(previous.time)}"

        if (currentList.isEmpty() && previousList.isEmpty()) {
            layoutReport.visibility = View.GONE
            tvEmpty.visibility = View.VISIBLE
            return
        }

        tvEmpty.visibility = View.GONE
        layoutReport.visibility = View.VISIBLE

        val currentIncome = repository.getTotalIncome(currentList)
        val currentExpense = repository.getTotalExpense(currentList)
        val previousIncome = repository.getTotalIncome(previousList)
        val previousExpense = repository.getTotalExpense(previousList)

        val summaryRows = listOf(
            CompareRow("Income", currentIncome, previousIncome, true),
            CompareRow("Expense", currentExpense, previousExpense, false),
            CompareRow("Saved", currentIncome - currentExpense, previousIncome - previousExpense, true)
        )
        rvSummary.adapter = CompareAdapter(summaryRows, currentLabel, previousLabel)

        val categoryRows = ComparisonReport.categoryRows(currentList, previousList)
        rvCategories.adapter = CompareAdapter(categoryRows, currentLabel, previousLabel)

        val insights = ComparisonReport.insights(
            currentIncome = currentIncome,
            currentExpense = currentExpense,
            previousIncome = previousIncome,
            previousExpense = previousExpense,
            categoryRows = categoryRows,
            previousLabel = previousLabel
        )
        showInsights(insights)
    }

    private fun showInsights(insights: List<Insight>) {
        layoutInsights.removeAllViews()
        val padding = (6 * resources.displayMetrics.density).toInt()

        for (insight in insights) {
            val textView = TextView(requireContext())
            textView.text = "•  ${insight.text}"
            textView.textSize = 14f
            textView.setPadding(0, padding, 0, padding)

            val colorRes = when (insight.good) {
                true -> R.color.sm_income
                false -> R.color.sm_expense
                null -> R.color.sm_text_primary
            }
            textView.setTextColor(ContextCompat.getColor(requireContext(), colorRes))
            layoutInsights.addView(textView)
        }
    }
}