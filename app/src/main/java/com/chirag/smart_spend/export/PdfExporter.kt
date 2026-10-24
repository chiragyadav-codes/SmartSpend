package com.chirag.smart_spend.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.chirag.smart_spend.reports.CategorySummary
import com.chirag.smart_spend.transactions.model.Transaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfExporter {

    private val pageWidth = 595
    private val pageHeight = 842
    private val margin = 40f
    private val bottomLimit = 800f

    fun exportMonthlyReport(
        context: Context,
        monthTitle: String,
        transactions: List<Transaction>,
        summaries: List<CategorySummary>,
        totalIncome: Double,
        totalExpense: Double,
        fileName: String
    ): Uri? {
        val document = PdfDocument()
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        val titlePaint = Paint().apply {
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            color = Color.BLACK
        }
        val headingPaint = Paint().apply {
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            color = Color.BLACK
        }
        val textPaint = Paint().apply {
            textSize = 11f
            color = Color.BLACK
        }
        val mutedPaint = Paint().apply {
            textSize = 10f
            color = Color.GRAY
        }
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }
        val rightIncome = Paint(textPaint).apply {
            color = Color.rgb(46, 125, 50)
            textAlign = Paint.Align.RIGHT
        }
        val rightExpense = Paint(textPaint).apply {
            color = Color.rgb(198, 40, 40)
            textAlign = Paint.Align.RIGHT
        }
        val rightMuted = Paint(mutedPaint).apply {
            textAlign = Paint.Align.RIGHT
        }
        val footerPaint = Paint(mutedPaint).apply {
            textAlign = Paint.Align.CENTER
        }

        var pageNumber = 1
        var page = document.startPage(
            PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        )
        var canvas = page.canvas
        var y = margin + 20f

        fun finishCurrentPage() {
            canvas.drawText("Page $pageNumber", pageWidth / 2f, pageHeight - 24f, footerPaint)
            document.finishPage(page)
        }

        fun ensureSpace(needed: Float) {
            if (y + needed > bottomLimit) {
                finishCurrentPage()
                pageNumber++
                page = document.startPage(
                    PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                )
                canvas = page.canvas
                y = margin + 20f
            }
        }

        val rightEdge = pageWidth - margin

        canvas.drawText("SmartSpend Report", margin, y, titlePaint)
        y += 20f
        canvas.drawText(monthTitle, margin, y, mutedPaint)
        y += 30f

        canvas.drawText("Summary", margin, y, headingPaint)
        y += 8f
        canvas.drawLine(margin, y, rightEdge, y, linePaint)
        y += 18f

        canvas.drawText("Income", margin, y, textPaint)
        canvas.drawText("₹${money(totalIncome)}", rightEdge, y, rightIncome)
        y += 16f
        canvas.drawText("Expense", margin, y, textPaint)
        canvas.drawText("₹${money(totalExpense)}", rightEdge, y, rightExpense)
        y += 16f

        val net = totalIncome - totalExpense
        canvas.drawText("Net savings", margin, y, textPaint)
        canvas.drawText("₹${money(net)}", rightEdge, y, if (net >= 0) rightIncome else rightExpense)
        y += 32f

        ensureSpace(50f)
        canvas.drawText("By category", margin, y, headingPaint)
        y += 8f
        canvas.drawLine(margin, y, rightEdge, y, linePaint)
        y += 18f

        for (summary in summaries) {
            ensureSpace(16f)
            canvas.drawText(summary.category, margin, y, textPaint)
            if (summary.type == "income") {
                canvas.drawText("+₹${money(summary.totalAmount)}", rightEdge, y, rightIncome)
            } else {
                canvas.drawText("-₹${money(summary.totalAmount)}", rightEdge, y, rightExpense)
            }
            y += 16f
        }
        y += 16f

        ensureSpace(70f)
        canvas.drawText("Transactions", margin, y, headingPaint)
        y += 8f
        canvas.drawLine(margin, y, rightEdge, y, linePaint)
        y += 18f

        canvas.drawText("Date", margin, y, mutedPaint)
        canvas.drawText("Category", 125f, y, mutedPaint)
        canvas.drawText("Note", 235f, y, mutedPaint)
        canvas.drawText("Amount", rightEdge, y, rightMuted)
        y += 16f

        for (transaction in transactions.sortedBy { it.date }) {
            ensureSpace(16f)
            canvas.drawText(dateFormat.format(Date(transaction.date)), margin, y, textPaint)
            canvas.drawText(transaction.category.take(16), 125f, y, textPaint)
            canvas.drawText(transaction.note.take(40), 235f, y, textPaint)
            if (transaction.type == "income") {
                canvas.drawText("+₹${money(transaction.amount)}", rightEdge, y, rightIncome)
            } else {
                canvas.drawText("-₹${money(transaction.amount)}", rightEdge, y, rightExpense)
            }
            y += 16f
        }

        finishCurrentPage()

        return try {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/")
                }
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                resolver.openOutputStream(it)?.use { out -> document.writeTo(out) }
            }
            uri
        } catch (e: Exception) {
            null
        } finally {
            document.close()
        }
    }

    private fun money(value: Double): String {
        return String.format(Locale.getDefault(), "%.2f", value)
    }
}