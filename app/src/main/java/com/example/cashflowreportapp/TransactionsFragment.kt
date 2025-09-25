package com.example.cashflowreportapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cashflowreportapp.database.AppDatabase
import java.text.NumberFormat
import java.util.*

class TransactionsFragment : Fragment() {
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var textTotalIncome: TextView
    private lateinit var textTotalExpense: TextView
    private lateinit var textBalance: TextView

    private var totalIncome: Double = 0.0
    private var totalExpense: Double = 0.0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_transactions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textTotalIncome = view.findViewById(R.id.text_total_income)
        textTotalExpense = view.findViewById(R.id.text_total_expense)
        textBalance = view.findViewById(R.id.text_balance)

        val recyclerView: RecyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        transactionAdapter = TransactionAdapter(emptyList())
        recyclerView.adapter = transactionAdapter

        val database = AppDatabase.getDatabase(requireContext())
        val transactionDao = database.transactionDao()

        transactionDao.getAllTransactions().observe(viewLifecycleOwner) { transactions ->
            transactionAdapter.updateData(transactions)
        }

        transactionDao.getTotalAmountByType("INCOME").observe(viewLifecycleOwner) { income ->
            totalIncome = income ?: 0.0
            updateBalanceUI()
        }

        transactionDao.getTotalAmountByType("EXPENSE").observe(viewLifecycleOwner) { expense ->
            totalExpense = expense ?: 0.0
            updateBalanceUI()
        }
    }

    private fun updateBalanceUI() {
        val balance = totalIncome - totalExpense

        val formatter = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        formatter.maximumFractionDigits = 0

        textTotalIncome.text = formatter.format(totalIncome)
        textTotalExpense.text = formatter.format(totalExpense)
        textBalance.text = formatter.format(balance)
    }
}