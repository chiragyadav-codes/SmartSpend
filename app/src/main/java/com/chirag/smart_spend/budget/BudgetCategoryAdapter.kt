package com.chirag.smart_spend.budget

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.chirag.smart_spend.R
import com.chirag.smart_spend.categorization.CategoryIcons
import com.chirag.smart_spend.reports.ComparisonReport
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlin.math.min

class BudgetCategoryAdapter(
    private val rows: List<CategoryProgress>,
    private val onClick: (CategoryProgress) -> Unit
) : RecyclerView.Adapter<BudgetCategoryAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCategory: TextView = itemView.findViewById(R.id.tvBudgetCategory)
        val tvAmounts: TextView = itemView.findViewById(R.id.tvBudgetAmounts)
        val tvStatus: TextView = itemView.findViewById(R.id.tvBudgetStatus)
        val progress: LinearProgressIndicator = itemView.findViewById(R.id.progressCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_budget_category, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val row = rows[position]
        val context = holder.itemView.context

        val icon = CategoryIcons.iconFor(row.category)
        holder.tvCategory.text = if (icon != null) "$icon  ${row.category}" else row.category

        holder.tvAmounts.text =
            "₹${ComparisonReport.money(row.spent)} / ₹${ComparisonReport.money(row.limit)}"

        val ratio = row.spent / row.limit
        val color = ContextCompat.getColor(context, BudgetCalculator.statusColor(ratio))

        holder.progress.setProgressCompat(min(100, (ratio * 100).toInt()), false)
        holder.progress.setIndicatorColor(color)

        holder.tvStatus.text = if (row.spent > row.limit) {
            "Over by ₹${ComparisonReport.money(row.spent - row.limit)}"
        } else {
            "₹${ComparisonReport.money(row.limit - row.spent)} left"
        }
        holder.tvStatus.setTextColor(color)

        holder.itemView.setOnClickListener { onClick(row) }
    }

    override fun getItemCount(): Int = rows.size
}