package com.chirag.smart_spend.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.chirag.smart_spend.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileFragment : Fragment() {

    private lateinit var tvAvatar: TextView
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvMemberSince: TextView
    private lateinit var tvTransactionCount: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tvAvatar = view.findViewById(R.id.tvProfileAvatar)
        tvName = view.findViewById(R.id.tvProfileName)
        tvEmail = view.findViewById(R.id.tvProfileEmail)
        tvMemberSince = view.findViewById(R.id.tvMemberSince)
        tvTransactionCount = view.findViewById(R.id.tvTransactionCount)

        view.findViewById<Button>(R.id.btnResetPassword).setOnClickListener {
            sendPasswordReset()
        }
    }

    override fun onResume() {
        super.onResume()
        loadProfile()
    }

    private fun loadProfile() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        tvEmail.text = user.email ?: ""

        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                if (!isAdded) return@addOnSuccessListener
                val name = document.getString("name") ?: "User"
                tvName.text = name
                tvAvatar.text = name.take(1).uppercase()

                val createdAt = document.getLong("createdAt")
                if (createdAt != null) {
                    val format = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    tvMemberSince.text = format.format(Date(createdAt))
                }
            }

        db.collection("transactions")
            .whereEqualTo("userId", user.uid)
            .count()
            .get(AggregateSource.SERVER)
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                tvTransactionCount.text = snapshot.count.toString()
            }
    }

    private fun sendPasswordReset() {
        val email = FirebaseAuth.getInstance().currentUser?.email
        if (email.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "No email found for this account", Toast.LENGTH_SHORT).show()
            return
        }

        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
            .addOnSuccessListener {
                if (!isAdded) return@addOnSuccessListener
                Toast.makeText(requireContext(), "Password reset email sent to $email", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}