package com.chirag.smart_spend.transactions

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chirag.smart_spend.R
import com.chirag.smart_spend.transactions.model.Transaction
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class AllTransactionsActivity : AppCompatActivity() {

    private var allTransactions: List<Transaction> = emptyList()
    private var selectedTab = 0

    private lateinit var tabLayout: TabLayout
    private lateinit var tvSectionSummary: TextView
    private lateinit var tvEmptyState: TextView
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_transactions)

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        tabLayout = findViewById(R.id.tabLayout)
        tvSectionSummary = findViewById(R.id.tvSectionSummary)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        recyclerView = findViewById(R.id.rvAllTransactions)
        recyclerView.layoutManager = LinearLayoutManager(this)

        tabLayout.addTab(tabLayout.newTab().setText("All"))
        tabLayout.addTab(tabLayout.newTab().setText("Income"))
        tabLayout.addTab(tabLayout.newTab().setText("Expense"))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                selectedTab = tab?.position ?: 0
                showList()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}

            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    override fun onResume() {
        super.onResume()
        loadTransactions()
    }

    private fun loadTransactions() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance().collection("transactions")
            .whereEqualTo("userId", userId)
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                allTransactions = result.map { doc ->
                    doc.toObject(Transaction::class.java).also { it.id = doc.id }
                }
                showList()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showList() {
        val filtered = when (selectedTab) {
            1 -> allTransactions.filter { it.type == "income" }
            2 -> allTransactions.filter { it.type == "expense" }
            else -> allTransactions
        }

        val total = filtered.sumOf { it.amount }
        val count = filtered.size

        when (selectedTab) {
            1 -> {
                tvSectionSummary.text = "$count income entries · ₹$total"
                tvSectionSummary.setTextColor(ContextCompat.getColor(this, R.color.sm_income))
                tvEmptyState.text = "No income recorded yet."
            }
            2 -> {
                tvSectionSummary.text = "$count expense entries · ₹$total"
                tvSectionSummary.setTextColor(ContextCompat.getColor(this, R.color.sm_expense))
                tvEmptyState.text = "No expenses recorded yet."
            }
            else -> {
                tvSectionSummary.text = "$count transactions"
                tvSectionSummary.setTextColor(ContextCompat.getColor(this, R.color.sm_text_primary))
                tvEmptyState.text = "No transactions yet."
            }
        }

        if (filtered.isEmpty()) {
            tvEmptyState.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            tvEmptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }

        recyclerView.adapter = TransactionAdapter(
            transactions = filtered,
            onItemClick = { transaction ->
                val intent = Intent(this, AddTransactionActivity::class.java)
                intent.putExtra("transactionId", transaction.id)
                startActivity(intent)
            },
            onItemLongClick = { transaction ->
                showDeleteConfirmation(transaction)
            }
        )
    }

    private fun showDeleteConfirmation(transaction: Transaction) {
        AlertDialog.Builder(this)
            .setTitle("Delete Transaction")
            .setMessage("Delete this ${transaction.category} entry?")
            .setPositiveButton("Delete") { _, _ ->
                FirebaseFirestore.getInstance().collection("transactions")
                    .document(transaction.id)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
                        loadTransactions()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to delete: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}