package com.chirag.smart_spend.reports

import android.content.Context
import android.graphics.Color
import androidx.core.content.ContextCompat
import com.chirag.smart_spend.R
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.formatter.ValueFormatter

object ChartHelper {

    private val sliceColors = listOf(
        Color.parseColor("#E57373"),
        Color.parseColor("#FFB74D"),
        Color.parseColor("#FFF176"),
        Color.parseColor("#81C784"),
        Color.parseColor("#64B5F6"),
        Color.parseColor("#BA68C8"),
        Color.parseColor("#4DB6AC")
    )

    private val hideZeroFormatter = object : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            return if (value == 0f) "" else value.toInt().toString()
        }
    }

    fun showExpensePie(
        context: Context,
        chart: PieChart,
        expenses: List<CategorySummary>,
        centerText: String
    ) {
        if (expenses.isEmpty()) {
            chart.clear()
            return
        }

        val textColor = ContextCompat.getColor(context, R.color.sm_text_primary)

        val entries = expenses.map { PieEntry(it.totalAmount.toFloat(), it.category) }
        val dataSet = PieDataSet(entries, "")
        dataSet.colors = sliceColors
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 11f
        dataSet.valueFormatter = PercentFormatter(chart)

        chart.data = PieData(dataSet)
        chart.setUsePercentValues(true)
        chart.description.isEnabled = false
        chart.setDrawEntryLabels(false)
        chart.setHoleColor(Color.TRANSPARENT)
        chart.centerText = centerText
        chart.setCenterTextColor(textColor)
        chart.legend.textColor = textColor
        chart.isRotationEnabled = false
        chart.invalidate()
    }

    fun showGroupedBars(
        context: Context,
        chart: BarChart,
        labels: List<String>,
        incomeValues: List<Float>,
        expenseValues: List<Float>
    ) {
        val groupCount = labels.size
        if (groupCount == 0) {
            chart.clear()
            return
        }

        val textColor = ContextCompat.getColor(context, R.color.sm_text_primary)
        val gridColor = ContextCompat.getColor(context, R.color.sm_border)
        val showValues = groupCount <= 6

        val incomeEntries = incomeValues.mapIndexed { index, value -> BarEntry(index.toFloat(), value) }
        val expenseEntries = expenseValues.mapIndexed { index, value -> BarEntry(index.toFloat(), value) }

        val incomeSet = BarDataSet(incomeEntries, "Income")
        incomeSet.setColor(ContextCompat.getColor(context, R.color.sm_income))
        incomeSet.setDrawValues(showValues)
        incomeSet.valueTextColor = textColor
        incomeSet.valueTextSize = 9f
        incomeSet.valueFormatter = hideZeroFormatter

        val expenseSet = BarDataSet(expenseEntries, "Expense")
        expenseSet.setColor(ContextCompat.getColor(context, R.color.sm_expense))
        expenseSet.setDrawValues(showValues)
        expenseSet.valueTextColor = textColor
        expenseSet.valueTextSize = 9f
        expenseSet.valueFormatter = hideZeroFormatter

        val data = BarData(incomeSet, expenseSet)
        val groupSpace = 0.3f
        val barSpace = 0.05f
        data.barWidth = 0.3f

        chart.data = data

        val xAxis = chart.xAxis
        xAxis.axisMinimum = 0f
        xAxis.axisMaximum = groupCount.toFloat()
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setCenterAxisLabels(true)
        xAxis.setDrawGridLines(false)
        xAxis.setLabelCount(groupCount, false)
        xAxis.textColor = textColor
        xAxis.textSize = if (groupCount > 12) 9f else 11f

        chart.groupBars(0f, groupSpace, barSpace)

        chart.axisLeft.textColor = textColor
        chart.axisLeft.gridColor = gridColor
        chart.axisLeft.axisMinimum = 0f
        chart.axisRight.isEnabled = false
        chart.legend.textColor = textColor
        chart.description.isEnabled = false

        // Turn off dragging and zooming so swiping across the chart changes the page.
        chart.setScaleEnabled(false)
        chart.isDragEnabled = false
        chart.setPinchZoom(false)
        chart.setDoubleTapToZoomEnabled(false)
        chart.setNoDataText("")

        chart.invalidate()
    }
}