package com.example.cashflowreportapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cashflowreportapp.database.AppDatabase
import com.example.cashflowreportapp.R

class TransactionsFragment : Fragment() {
    private lateinit var transactionAdapter: TransactionAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_transactions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView: RecyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)

        transactionAdapter = TransactionAdapter(emptyList())
        recyclerView.adapter = transactionAdapter

        val database = AppDatabase.getDatabase(requireContext())
        database.transactionDao().getAllTransactions().observe(viewLifecycleOwner) { transactions ->
            transactionAdapter.updateData(transactions)
        }
    }
}