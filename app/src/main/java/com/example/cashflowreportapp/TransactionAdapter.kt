package com.example.cashflowreportapp

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cashflowreportapp.database.Transaction
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(
    private var transactions: List<Transaction>,
    private val onEditClick: (Transaction) -> Unit,
    private val onDeleteClick: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    fun getTransactionAt(position: Int): Transaction {
        return transactions[position]
    }

    fun updateData(newTransactions: List<Transaction>) {
        this.transactions = newTransactions
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.title.text = transaction.title

        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        holder.date.text = sdf.format(Date(transaction.date))

        val formattedAmount = String.format(Locale.GERMANY, "Rp %,.0f", transaction.amount)
        holder.amount.text = formattedAmount

        if (transaction.type == "EXPENSE") {
            holder.amount.setTextColor(Color.RED)
        } else {
            holder.amount.setTextColor(Color.GREEN)
        }

        holder.btnEdit.setOnClickListener { onEditClick(transaction) }
        holder.btnDelete.setOnClickListener { onDeleteClick(transaction) }
    }

    override fun getItemCount() = transactions.size

    class TransactionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.transaction_title)
        val date: TextView = view.findViewById(R.id.transaction_date)
        val amount: TextView = view.findViewById(R.id.transaction_amount)
        val btnEdit: ImageButton = view.findViewById(R.id.btn_edit)
        val btnDelete: ImageButton = view.findViewById(R.id.btn_delete)
    }
}
