package com.chirag.smart_spend.reports

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chirag.smart_spend.R

class CategorySummaryAdapter(
    private val summaries: List<CategorySummary>
) : RecyclerView.Adapter<CategorySummaryAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCategoryName: TextView = itemView.findViewById(R.id.tvCategoryName)
        val tvCategoryAmount: TextView = itemView.findViewById(R.id.tvCategoryAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_summary, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val summary = summaries[position]
        holder.tvCategoryName.text = summary.category

        if (summary.type == "income") {
            holder.tvCategoryAmount.text = "+${summary.totalAmount}"
            holder.tvCategoryAmount.setTextColor(Color.parseColor("#2E7D32"))
        } else {
            holder.tvCategoryAmount.text = "-${summary.totalAmount}"
            holder.tvCategoryAmount.setTextColor(Color.parseColor("#C62828"))
        }
    }

    override fun getItemCount(): Int = summaries.size
}