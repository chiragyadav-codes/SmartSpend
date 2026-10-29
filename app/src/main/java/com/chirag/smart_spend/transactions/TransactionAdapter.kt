package com.chirag.smart_spend.transactions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.chirag.smart_spend.R
import com.chirag.smart_spend.categorization.CategoryIcons
import com.chirag.smart_spend.transactions.model.Transaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionAdapter(
    private val transactions: List<Transaction>,
    private val onItemClick: (Transaction) -> Unit,
    private val onItemLongClick: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvNote: TextView = itemView.findViewById(R.id.tvNote)
        val tvSource: TextView = itemView.findViewById(R.id.tvSource)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        val context = holder.itemView.context

        val icon = CategoryIcons.iconFor(transaction.category)
        holder.tvCategory.text = if (icon != null) "$icon  ${transaction.category}" else transaction.category

        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        holder.tvDate.text = dateFormat.format(Date(transaction.date))

        if (transaction.note.isBlank()) {
            holder.tvNote.visibility = View.GONE
        } else {
            holder.tvNote.visibility = View.VISIBLE
            holder.tvNote.text = transaction.note
        }

        val sourceText = if (transaction.source == "auto") "Auto-detected" else "Added manually"
        holder.tvSource.text = if (transaction.billImagePath.isNotEmpty()) {
            "$sourceText  ·  Bill attached"
        } else {
            sourceText
        }

        if (transaction.type == "income") {
            holder.tvAmount.text = "+${transaction.amount}"
            holder.tvAmount.setTextColor(ContextCompat.getColor(context, R.color.sm_income))
        } else {
            holder.tvAmount.text = "-${transaction.amount}"
            holder.tvAmount.setTextColor(ContextCompat.getColor(context, R.color.sm_expense))
        }

        holder.itemView.setOnClickListener {
            onItemClick(transaction)
        }

        holder.itemView.setOnLongClickListener {
            onItemLongClick(transaction)
            true
        }
    }

    override fun getItemCount(): Int = transactions.size
}