package com.chirag.smart_spend.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.chirag.smart_spend.transactions.model.Transaction

class NotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "SmartSpendListener"
    }

    private val parser = NotificationParser()
    private val recentlyProcessed = mutableSetOf<String>()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)

        val packageName = sbn.packageName
        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""

        Log.d(TAG, "Notification from: $packageName")
        Log.d(TAG, "Title: $title")
        Log.d(TAG, "Text: $text")

        val parsed = parser.parse(text)
        if (parsed == null) {
            Log.d(TAG, "PARSED FAILED -> no match for this text")
            return
        }

        Log.d(TAG, "PARSED SUCCESS -> Amount: ${parsed.amount}, Merchant: ${parsed.merchant}, Type: ${parsed.type}")

        // Simple duplicate guard: skip if we've already processed this exact text recently
        val dedupeKey = "$packageName|$text"
        if (recentlyProcessed.contains(dedupeKey)) {
            Log.d(TAG, "SKIPPED -> duplicate notification, already saved")
            return
        }
        recentlyProcessed.add(dedupeKey)

        saveToFirestore(parsed, packageName)
    }

    private fun saveToFirestore(parsed: ParsedTransaction, sourcePackage: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.d(TAG, "SKIPPED SAVE -> no user logged in")
            return
        }

        val transaction = Transaction(
            userId = userId,
            amount = parsed.amount,
            type = parsed.type,
            category = parsed.merchant, // will be refined by Module 6 (Auto-Categorization)
            note = "Auto-detected from $sourcePackage",
            date = System.currentTimeMillis(),
            source = "auto"
        )

        FirebaseFirestore.getInstance().collection("transactions")
            .add(transaction)
            .addOnSuccessListener {
                Log.d(TAG, "AUTO-SAVED to Firestore successfully")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "AUTO-SAVE FAILED: ${e.message}")
            }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification listener connected")
    }
}