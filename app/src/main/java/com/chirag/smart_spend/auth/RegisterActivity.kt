package com.chirag.smart_spend.auth

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.chirag.smart_spend.R
import com.chirag.smart_spend.home.HomeActivity
import android.content.Intent

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvGoToLogin = findViewById<TextView>(R.id.tvGoToLogin)

        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { authResult ->
                    val userId = authResult.user?.uid ?: return@addOnSuccessListener

                    val userMap = hashMapOf(
                        "name" to name,
                        "email" to email,
                        "createdAt" to System.currentTimeMillis()
                    )

                    val db = FirebaseFirestore.getInstance()
                    db.collection("users").document(userId)
                        .set(userMap)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, HomeActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Saved auth but failed to save profile: ${e.message}", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, HomeActivity::class.java))
                            finish()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Registration failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        tvGoToLogin.setOnClickListener {
            finish() // goes back to LoginActivity
        }
    }
}