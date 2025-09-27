package com.example.cashflowreportapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cashflowreportapp.api.ExchangeResponse
import com.example.cashflowreportapp.api.RetrofitClient
import com.example.cashflowreportapp.database.AppDatabase
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.*

class TransactionsFragment : Fragment() {
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var textBalance: TextView
    private lateinit var textBalanceUsd: TextView   // 👈 add this
    private lateinit var textBalanceChange: TextView

    private var totalIncome: Double = 0.0
    private var totalExpense: Double = 0.0
    private var lastBalance: Double = 0.0
    private var isFirstUpdate = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_transactions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textBalance = view.findViewById(R.id.text_balance)
        textBalanceUsd = view.findViewById(R.id.text_balance_usd) // 👈 initialize
        textBalanceChange = view.findViewById(R.id.text_balance_change)

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

        textBalance.text = formatter.format(balance)

        // Example: fake conversion rate 1 USD = 16000 IDR
        val usdRate = 16000.0
        val usdBalance = balance / usdRate
        textBalanceUsd.text = "$" + String.format("%.2f", usdBalance)  // 👈 no crash

        if (!isFirstUpdate) {
            updateBalanceChange(balance, formatter)
        } else {
            textBalanceChange.text = "No Change"
            textBalanceChange.setBackgroundResource(R.drawable.bg_gray_badge)
            isFirstUpdate = false
        }

        lastBalance = balance
    }

    private fun updateBalanceChange(currentBalance: Double, formatter: NumberFormat) {
        val difference = currentBalance - lastBalance

        if (difference > 0) {
            textBalanceChange.text = "+${formatter.format(difference)}"
            textBalanceChange.setBackgroundResource(R.drawable.bg_green_badge)
        } else if (difference < 0) {
            textBalanceChange.text = formatter.format(difference) // already negative
            textBalanceChange.setBackgroundResource(R.drawable.bg_red_badge)
        } else {
            textBalanceChange.text = "No Change"
            textBalanceChange.setBackgroundResource(R.drawable.bg_gray_badge)
        }
    }
}

