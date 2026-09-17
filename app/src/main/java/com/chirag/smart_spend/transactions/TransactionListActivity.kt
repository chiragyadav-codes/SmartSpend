package com.chirag.smart_spend.transactions

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.chirag.smart_spend.R
import com.chirag.smart_spend.transactions.model.Transaction

class TransactionListActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction_list)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        recyclerView = findViewById(R.id.rvTransactions)
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadTransactions()
    }

    override fun onResume() {
        super.onResume()
        loadTransactions() // refresh list whenever we come back to this screen
    }

    private fun loadTransactions() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "You must be logged in", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("transactions")
            .whereEqualTo("userId", userId)
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val transactionList = mutableListOf<Transaction>()
                for (document in result) {
                    val transaction = document.toObject(Transaction::class.java)
                    transaction.id = document.id
                    transactionList.add(transaction)
                }

                recyclerView.adapter = TransactionAdapter(
                    transactions = transactionList,
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
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDeleteConfirmation(transaction: Transaction) {
        AlertDialog.Builder(this)
            .setTitle("Delete Transaction")
            .setMessage("Delete this ${transaction.category} entry?")
            .setPositiveButton("Delete") { _, _ ->
                db.collection("transactions").document(transaction.id)
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