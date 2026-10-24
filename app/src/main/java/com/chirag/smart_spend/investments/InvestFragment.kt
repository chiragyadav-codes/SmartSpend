package com.chirag.smart_spend.investments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.chirag.smart_spend.R
import com.chirag.smart_spend.reports.ReportRepository
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class InvestFragment : Fragment() {

    private val repository = ReportRepository()

    private lateinit var tvInvestMonth: TextView
    private lateinit var tvSavingsRate: TextView
    private lateinit var tvSavedDetail: TextView
    private lateinit var tvSuggestion: TextView
    private lateinit var progressSavings: LinearProgressIndicator

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_invest, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tvInvestMonth = view.findViewById(R.id.tvInvestMonth)
        tvSavingsRate = view.findViewById(R.id.tvSavingsRate)
        tvSavedDetail = view.findViewById(R.id.tvSavedDetail)
        tvSuggestion = view.findViewById(R.id.tvSuggestion)
        progressSavings = view.findViewById(R.id.progressSavings)
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        val calendar = Calendar.getInstance()
        tvInvestMonth.text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)

        repository.getTransactionsForMonth(
            year = calendar.get(Calendar.YEAR),
            month = calendar.get(Calendar.MONTH),
            onResult = { transactions ->
                if (!isAdded) return@getTransactionsForMonth

                val income = repository.getTotalIncome(transactions)
                val expense = repository.getTotalExpense(transactions)
                val saved = income - expense
                val rate = if (income > 0) (saved / income * 100).coerceIn(0.0, 100.0) else 0.0

                tvSavingsRate.text = "${rate.toInt()}%"
                progressSavings.setProgressCompat(rate.toInt(), true)

                tvSavedDetail.text = if (saved >= 0) {
                    "Saved ₹$saved of ₹$income income"
                } else {
                    "Overspent by ₹${-saved} this month"
                }

                tvSuggestion.text = InvestmentSuggestionEngine.getSuggestion(income, expense)
            },
            onError = { e ->
                if (!isAdded) return@getTransactionsForMonth
                Toast.makeText(requireContext(), "Failed to load: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }
}