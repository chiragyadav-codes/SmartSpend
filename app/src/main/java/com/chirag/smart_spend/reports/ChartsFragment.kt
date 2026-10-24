package com.chirag.smart_spend.reports

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.chirag.smart_spend.R
import com.chirag.smart_spend.export.CsvExporter
import com.chirag.smart_spend.export.PdfExporter
import com.chirag.smart_spend.home.ToolbarActionHandler
import com.chirag.smart_spend.transactions.model.Transaction
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import java.text.SimpleDateFormat
import java.util.Locale

class ChartsFragment : Fragment(), ToolbarActionHandler {

    private val repository = ReportRepository()
    private val csvExporter = CsvExporter()
    private val pdfExporter = PdfExporter()
    private lateinit var viewModel: ChartsViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_charts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[ChartsViewModel::class.java]

        val tabs = view.findViewById<TabLayout>(R.id.chartsTabs)
        val pager = view.findViewById<ViewPager2>(R.id.chartsPager)
        pager.adapter = ChartsPagerAdapter(this)

        val titles = listOf("Month", "Day", "Trends", "Compare")
        TabLayoutMediator(tabs, pager) { tab, position ->
            tab.text = titles[position]
        }.attach()
    }

    override fun onResume() {
        super.onResume()
        viewModel.load()
    }

    override fun onToolbarAction(itemId: Int): Boolean {
        return when (itemId) {
            R.id.action_export_csv -> {
                exportCsv()
                true
            }
            R.id.action_export_pdf -> {
                exportPdf()
                true
            }
            else -> false
        }
    }

    private fun selectedMonthTransactions(): List<Transaction> {
        val all = viewModel.transactions.value ?: emptyList()
        val start = ReportDates.startOfMonth(viewModel.selectedMonth)
        val end = ReportDates.endOfMonth(viewModel.selectedMonth)
        return all.filter { it.date in start..end }
    }

    private fun exportCsv() {
        val transactions = selectedMonthTransactions()
        if (transactions.isEmpty()) {
            Toast.makeText(requireContext(), "No transactions to export", Toast.LENGTH_SHORT).show()
            return
        }

        val suffix = SimpleDateFormat("MMM_yyyy", Locale.getDefault()).format(viewModel.selectedMonth.time)
        val fileName = "SmartSpend_$suffix.csv"

        val uri = csvExporter.exportTransactions(requireContext(), transactions, fileName)

        if (uri != null) {
            Toast.makeText(requireContext(), "Exported to Downloads: $fileName", Toast.LENGTH_LONG).show()

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Share CSV via"))
        } else {
            Toast.makeText(requireContext(), "Export failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun exportPdf() {
        val transactions = selectedMonthTransactions()
        if (transactions.isEmpty()) {
            Toast.makeText(requireContext(), "No transactions to export", Toast.LENGTH_SHORT).show()
            return
        }

        val monthTitle = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(viewModel.selectedMonth.time)
        val suffix = SimpleDateFormat("MMM_yyyy", Locale.getDefault()).format(viewModel.selectedMonth.time)
        val fileName = "SmartSpend_$suffix.pdf"

        val uri = pdfExporter.exportMonthlyReport(
            context = requireContext(),
            monthTitle = monthTitle,
            transactions = transactions,
            summaries = repository.groupByCategory(transactions),
            totalIncome = repository.getTotalIncome(transactions),
            totalExpense = repository.getTotalExpense(transactions),
            fileName = fileName
        )

        if (uri != null) {
            Toast.makeText(requireContext(), "Exported to Downloads: $fileName", Toast.LENGTH_LONG).show()

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Share PDF via"))
        } else {
            Toast.makeText(requireContext(), "PDF export failed", Toast.LENGTH_SHORT).show()
        }
    }
}