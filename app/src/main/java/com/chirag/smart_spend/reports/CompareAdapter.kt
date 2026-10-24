package com.chirag.smart_spend.reports

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.chirag.smart_spend.R
import java.util.Locale
import kotlin.math.abs

class CompareAdapter(
    private val rows: List<CompareRow>,
    private val currentLabel: String,
    private val previousLabel: String
) : RecyclerView.Adapter<CompareAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvRowTitle)
        val tvValues: TextView = itemView.findViewById(R.id.tvRowValues)
        val tvChange: TextView = itemView.findViewById(R.id.tvRowChange)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_compare, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val row = rows[position]
        val context = holder.itemView.context

        holder.tvTitle.text = row.title
        holder.tvValues.text =
            "$currentLabel: ₹${ComparisonReport.money(row.current)}   " +
                    "$previousLabel: ₹${ComparisonReport.money(row.previous)}"

        val diff = row.current - row.previous
        val sign = when {
            diff > 0 -> "+"
            diff < 0 -> "-"
            else -> ""
        }
        val percentText = when {
            row.previous == 0.0 && row.current == 0.0 -> "0%"
            row.previous == 0.0 -> "new"
            else -> String.format(Locale.getDefault(), "%+.0f%%", diff / row.previous * 100)
        }
        holder.tvChange.text = "${sign}₹${ComparisonReport.money(abs(diff))}\n$percentText"

        val colorRes = when {
            diff == 0.0 -> R.color.sm_text_secondary
            (diff > 0) == row.higherIsBetter -> R.color.sm_income
            else -> R.color.sm_expense
        }
        holder.tvChange.setTextColor(ContextCompat.getColor(context, colorRes))
    }

    override fun getItemCount(): Int = rows.size
}