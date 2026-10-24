package com.chirag.smart_spend.reports

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.chirag.smart_spend.transactions.model.Transaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Calendar

class ChartsViewModel : ViewModel() {

    val transactions = MutableLiveData<List<Transaction>>(emptyList())
    val error = MutableLiveData<String?>()

    // The month shown on the Month page. Export uses it too.
    val selectedMonth: Calendar = Calendar.getInstance()

    // The day shown on the Day page.
    val selectedDay: Calendar = Calendar.getInstance()

    // Trends page: 0 = Day, 1 = Month, 2 = Year.
    var trendMode = 1
    val trendAnchor: Calendar = Calendar.getInstance()
    var trendCategory: String? = null

    // Compare page: 0 = month against the month before, 1 = year against the year before.
    var compareMode = 0
    val compareAnchor: Calendar = Calendar.getInstance()

    fun load() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance().collection("transactions")
            .whereEqualTo("userId", userId)
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                transactions.value = result.map { doc ->
                    doc.toObject(Transaction::class.java).also { it.id = doc.id }
                }
            }
            .addOnFailureListener { e ->
                error.value = e.message
            }
    }
}