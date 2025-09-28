package com.example.cashflowreportapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cashflowreportapp.database.AppDatabase
import com.example.cashflowreportapp.database.Transaction
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

class TransactionsFragment : Fragment() {

    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var textTotalIncome: TextView
    private lateinit var textTotalExpense: TextView
    private lateinit var textBalance: TextView
    private var totalIncome: Double = 0.0
    private var totalExpense: Double = 0.0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_transactions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textTotalIncome = view.findViewById(R.id.text_total_income)
        textTotalExpense = view.findViewById(R.id.text_total_expense)
        textBalance = view.findViewById(R.id.text_balance)

        val recyclerView: RecyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)

        val database = AppDatabase.getDatabase(requireContext())
        val transactionDao = database.transactionDao()

        transactionAdapter = TransactionAdapter(
            emptyList(),
            onEditClick = { transaction -> showEditDialog(transaction) },
            onDeleteClick = { transaction -> showDeleteDialog(transaction) }
        )
        recyclerView.adapter = transactionAdapter

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

        val itemTouchHelper = ItemTouchHelper(object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val transaction = transactionAdapter.getTransactionAt(position)

                if (direction == ItemTouchHelper.RIGHT) {
                    showEditDialog(transaction)
                    transactionAdapter.notifyItemChanged(position)
                } else if (direction == ItemTouchHelper.LEFT) {
                    showDeleteDialog(transaction, position)
                }
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun updateBalanceUI() {
        val balance = totalIncome - totalExpense
        val formatter = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        formatter.maximumFractionDigits = 0
        textTotalIncome.text = formatter.format(totalIncome)
        textTotalExpense.text = formatter.format(totalExpense)
        textBalance.text = formatter.format(balance)
    }

    private fun showAddDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.fragment_add_transacction, null)
        val titleInput = dialogView.findViewById<TextInputEditText>(R.id.input_title)
        val amountInput = dialogView.findViewById<TextInputEditText>(R.id.input_amount)
        val radioIncome = dialogView.findViewById<RadioButton>(R.id.radio_income)
        val radioExpense = dialogView.findViewById<RadioButton>(R.id.radio_expense)

        AlertDialog.Builder(requireContext())
            .setTitle("Tambah Transaksi")
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val newTransaction = Transaction(
                    title = titleInput.text.toString(),
                    amount = amountInput.text.toString().toDoubleOrNull() ?: 0.0,
                    type = if (radioIncome.isChecked) "INCOME" else "EXPENSE",
                    date = System.currentTimeMillis()
                )
                lifecycleScope.launch {
                    AppDatabase.getDatabase(requireContext()).transactionDao().insert(newTransaction)
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showEditDialog(transaction: Transaction) {
        val dialogVieww = LayoutInflater.from(requireContext())
            .inflate(R.layout.fragment_edit_transaction, null)
        val titleInput = dialogVieww.findViewById<TextInputEditText>(R.id.input_title)
        val amountInput = dialogVieww.findViewById<TextInputEditText>(R.id.input_amount)
        val radioIncome = dialogVieww.findViewById<RadioButton>(R.id.radio_income)
        val radioExpense = dialogVieww.findViewById<RadioButton>(R.id.radio_expense)

        titleInput.setText(transaction.title)
        amountInput.setText(transaction.amount.toString())
        if (transaction.type == "INCOME") radioIncome.isChecked = true else radioExpense.isChecked = true

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Transaksi")
            .setView(dialogVieww)
            .setPositiveButton("Simpan") { _, _ ->
                val updated = transaction.copy(
                    title = titleInput.text.toString(),
                    amount = amountInput.text.toString().toDoubleOrNull() ?: 0.0,
                    type = if (radioIncome.isChecked) "INCOME" else "EXPENSE",
                    date = System.currentTimeMillis()
                )
                lifecycleScope.launch {
                    AppDatabase.getDatabase(requireContext()).transactionDao().update(updated)
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showDeleteDialog(transaction: Transaction, position: Int? = null) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Transaksi")
            .setMessage("Yakin mau hapus transaksi \"${transaction.title}\"?")
            .setPositiveButton("Hapus") { _, _ ->
                lifecycleScope.launch {
                    AppDatabase.getDatabase(requireContext()).transactionDao().delete(transaction)
                }
            }
            .setNegativeButton("Batal") { _, _ ->
                position?.let { transactionAdapter.notifyItemChanged(it) }
            }
            .show()
    }
}
