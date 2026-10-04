package com.chirag.smart_spend.export

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.chirag.smart_spend.transactions.model.Transaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CsvExporter {

    fun exportTransactions(context: Context, transactions: List<Transaction>, fileName: String): Uri? {
        val resolver = context.contentResolver
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        val csvContent = StringBuilder()
        csvContent.append("Date,Type,Category,Amount,Note,Source\n")

        for (transaction in transactions) {
            val date = dateFormat.format(Date(transaction.date))
            val note = transaction.note.replace(",", ";") // avoid breaking CSV columns
            csvContent.append("$date,${transaction.type},${transaction.category},${transaction.amount},$note,${transaction.source}\n")
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/")
            }
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                outputStream.write(csvContent.toString().toByteArray())
            }
        }

        return uri
    }
}