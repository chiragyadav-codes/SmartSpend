package com.chirag.smart_spend.transactions

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chirag.smart_spend.R
import com.chirag.smart_spend.home.ToolbarActionHandler
import com.chirag.smart_spend.transactions.model.Transaction
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import com.chirag.smart_spend.budget.HomeBudgetBar

class TransactionsFragment : Fragment(), ToolbarActionHandler {

    private var allTransactions: List<Transaction> = emptyList()
    private var periodStart = 0L
    private var periodEnd = 0L
    private var isCustomPeriod = false

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var tvPeriod: TextView
    private lateinit var tvResetPeriod: TextView
    private lateinit var tvSectionCount: TextView
    private lateinit var tvMonthIncome: TextView
    private lateinit var tvMonthExpense: TextView
    private lateinit var tvMonthSaved: TextView
    private lateinit var btnCalendar: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_transactions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.rvTransactions)
        tvEmptyState = view.findViewById(R.id.tvEmptyState)
        tvPeriod = view.findViewById(R.id.tvPeriod)
        tvResetPeriod = view.findViewById(R.id.tvResetPeriod)
        tvSectionCount = view.findViewById(R.id.tvSectionCount)
        tvMonthIncome = view.findViewById(R.id.tvMonthIncome)
        tvMonthExpense = view.findViewById(R.id.tvMonthExpense)
        tvMonthSaved = view.findViewById(R.id.tvMonthSaved)
        btnCalendar = view.findViewById(R.id.btnCalendar)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        resetToCurrentMonth()
        showSummary()

        btnCalendar.setOnClickListener { showPeriodPicker() }
        tvResetPeriod.setOnClickListener {
            resetToCurrentMonth()
            showSummary()
        }
    }

    override fun onResume() {
        super.onResume()
        loadTransactions()
    }

    override fun onToolbarAction(itemId: Int): Boolean {
        return when (itemId) {
            R.id.action_all_transactions -> {
                startActivity(Intent(requireContext(), AllTransactionsActivity::class.java))
                true
            }
            R.id.action_refresh -> {
                loadTransactions()
                true
            }
            else -> false
        }
    }

    private fun loadTransactions() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance().collection("transactions")
            .whereEqualTo("userId", userId)
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                if (!isAdded) return@addOnSuccessListener
                allTransactions = result.map { doc ->
                    doc.toObject(Transaction::class.java).also { it.id = doc.id }
                }
                showTransactions()
                showSummary()
                view?.let { HomeBudgetBar.update(it, allTransactions) }
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                Toast.makeText(requireContext(), "Failed to load: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showTransactions() {
        tvSectionCount.text = "${allTransactions.size} entries"

        if (allTransactions.isEmpty()) {
            tvEmptyState.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            tvEmptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }

        recyclerView.adapter = TransactionAdapter(
            transactions = allTransactions,
            onItemClick = { transaction ->
                val intent = Intent(requireContext(), AddTransactionActivity::class.java)
                intent.putExtra("transactionId", transaction.id)
                startActivity(intent)
            },
            onItemLongClick = { transaction ->
                showDeleteConfirmation(transaction)
            }
        )
    }

    private fun showSummary() {
        val inPeriod = allTransactions.filter { it.date in periodStart..periodEnd }
        val income = inPeriod.filter { it.type == "income" }.sumOf { it.amount }
        val expense = inPeriod.filter { it.type == "expense" }.sumOf { it.amount }
        val saved = income - expense

        tvPeriod.text = periodLabel()
        tvResetPeriod.visibility = if (isCustomPeriod) View.VISIBLE else View.GONE

        tvMonthIncome.text = "₹$income"
        tvMonthExpense.text = "₹$expense"
        tvMonthSaved.text = "₹$saved"

        val savedColor = if (saved >= 0) R.color.sm_income else R.color.sm_expense
        tvMonthSaved.setTextColor(ContextCompat.getColor(requireContext(), savedColor))
    }

    private fun periodLabel(): String {
        if (!isCustomPeriod) {
            return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(periodStart))
        }

        val format = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
        val start = format.format(Date(periodStart))
        val end = format.format(Date(periodEnd))
        return if (start == end) start else "$start – $end"
    }

    private fun resetToCurrentMonth() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        periodStart = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        periodEnd = calendar.timeInMillis

        isCustomPeriod = false
    }

    private fun showPeriodPicker() {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Select period")
            .build()

        picker.addOnPositiveButtonClickListener { selection ->
            val first = selection.first
            val second = selection.second
            if (first != null && second != null) {
                periodStart = utcDayToLocal(first, endOfDay = false)
                periodEnd = utcDayToLocal(second, endOfDay = true)
                isCustomPeriod = true
                showSummary()
            }
        }

        picker.show(childFragmentManager, "period_picker")
    }

    // The date picker returns each day as midnight in UTC, so convert it to
    // the same day in the phone's own time zone before comparing with dates.
    private fun utcDayToLocal(utcMillis: Long, endOfDay: Boolean): Long {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utc.timeInMillis = utcMillis

        val local = Calendar.getInstance()
        val year = utc.get(Calendar.YEAR)
        val month = utc.get(Calendar.MONTH)
        val day = utc.get(Calendar.DAY_OF_MONTH)

        if (endOfDay) {
            local.set(year, month, day, 23, 59, 59)
            local.set(Calendar.MILLISECOND, 999)
        } else {
            local.set(year, month, day, 0, 0, 0)
            local.set(Calendar.MILLISECOND, 0)
        }
        return local.timeInMillis
    }

    private fun showDeleteConfirmation(transaction: Transaction) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Transaction")
            .setMessage("Delete this ${transaction.category} entry?")
            .setPositiveButton("Delete") { _, _ ->
                FirebaseFirestore.getInstance().collection("transactions")
                    .document(transaction.id)
                    .delete()
                    .addOnSuccessListener {
                        if (!isAdded) return@addOnSuccessListener
                        Toast.makeText(requireContext(), "Deleted", Toast.LENGTH_SHORT).show()
                        loadTransactions()
                    }
                    .addOnFailureListener { e ->
                        if (!isAdded) return@addOnFailureListener
                        Toast.makeText(requireContext(), "Failed to delete: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}