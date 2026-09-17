package com.chirag.smart_spend.transactions

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.chirag.smart_spend.R
import com.chirag.smart_spend.transactions.model.Transaction

class AddTransactionActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var editingTransactionId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val rgType = findViewById<RadioGroup>(R.id.rgType)
        val etAmount = findViewById<EditText>(R.id.etAmount)
        val etCategory = findViewById<EditText>(R.id.etCategory)
        val etNote = findViewById<EditText>(R.id.etNote)
        val btnSave = findViewById<Button>(R.id.btnSaveTransaction)

        editingTransactionId = intent.getStringExtra("transactionId")

        if (editingTransactionId != null) {
            btnSave.text = "Update Transaction"
            db.collection("transactions").document(editingTransactionId!!)
                .get()
                .addOnSuccessListener { document ->
                    val transaction = document.toObject(Transaction::class.java)
                    if (transaction != null) {
                        etAmount.setText(transaction.amount.toString())
                        etCategory.setText(transaction.category)
                        etNote.setText(transaction.note)
                        if (transaction.type == "income") {
                            rgType.check(R.id.rbIncome)
                        } else {
                            rgType.check(R.id.rbExpense)
                        }
                    }
                }
        }

        btnSave.setOnClickListener {
            val amountText = etAmount.text.toString().trim()
            val category = etCategory.text.toString().trim()
            val note = etNote.text.toString().trim()

            if (amountText.isEmpty() || category.isEmpty()) {
                Toast.makeText(this, "Please enter amount and category", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountText.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(this, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val type = if (rgType.checkedRadioButtonId == R.id.rbIncome) "income" else "expense"
            val userId = auth.currentUser?.uid

            if (userId == null) {
                Toast.makeText(this, "You must be logged in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (editingTransactionId != null) {
                // UPDATE existing transaction
                val updates = hashMapOf<String, Any>(
                    "amount" to amount,
                    "type" to type,
                    "category" to category,
                    "note" to note
                )
                db.collection("transactions").document(editingTransactionId!!)
                    .update(updates)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Transaction updated!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to update: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                // CREATE new transaction
                val transaction = Transaction(
                    userId = userId,
                    amount = amount,
                    type = type,
                    category = category,
                    note = note,
                    date = System.currentTimeMillis(),
                    source = "manual"
                )
                db.collection("transactions")
                    .add(transaction)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Transaction saved!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }
}