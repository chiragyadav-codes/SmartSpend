package com.chirag.smart_spend.home

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.chirag.smart_spend.R
import com.chirag.smart_spend.auth.LoginActivity

class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        auth = FirebaseAuth.getInstance()
        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)

        val userId = auth.currentUser?.uid
        if (userId != null) {
            FirebaseFirestore.getInstance().collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    val name = document.getString("name") ?: "User"
                    tvWelcome.text = "Welcome, $name"
                }
        }

        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        findViewById<Button>(R.id.btnAddTransaction).setOnClickListener {
            startActivity(Intent(this, com.chirag.smart_spend.transactions.AddTransactionActivity::class.java))
        }

        findViewById<Button>(R.id.btnViewTransactions).setOnClickListener {
            startActivity(Intent(this, com.chirag.smart_spend.transactions.TransactionListActivity::class.java))
        }
    }
}