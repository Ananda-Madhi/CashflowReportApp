package com.example.cashflowreportapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cashflowreportapp.database.AppDatabase
import com.example.cashflowreportapp.database.Transaction
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.util.*
import android.util.Log

class TransactionsFragment : Fragment() {

    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var textTotalIncome: TextView
    private lateinit var textTotalExpense: TextView
    private lateinit var textBalance: TextView
    private lateinit var currencySpinner: Spinner
    private lateinit var convertedBalance: TextView

    private var totalIncome: Double = 0.0
    private var totalExpense: Double = 0.0

    private val currencies = arrayOf(
        "USD", "EUR", "JPY", "GBP", "AUD",
        "SGD", "CNY", "KRW", "MYR", "THB"
    )

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
        currencySpinner = view.findViewById(R.id.currencySpinner)
        convertedBalance = view.findViewById(R.id.convertedBalance)

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

        // Observe daftar transaksi
        transactionDao.getAllTransactions().observe(viewLifecycleOwner) { transactions ->
            transactionAdapter.updateData(transactions)
        }

        // Observe total income
        transactionDao.getTotalAmountByType("INCOME").observe(viewLifecycleOwner) { income ->
            totalIncome = income ?: 0.0
            updateBalanceUI()
        }

        // Observe total expense
        transactionDao.getTotalAmountByType("EXPENSE").observe(viewLifecycleOwner) { expense ->
            totalExpense = expense ?: 0.0
            updateBalanceUI()
        }

        // Swipe edit & delete
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

        // Spinner currency
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, currencies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        currencySpinner.adapter = adapter

        currencySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateConvertedBalance()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    private fun updateConvertedBalance() {
        val selectedCurrency = currencySpinner.selectedItem?.toString() ?: return
        val balance = totalIncome - totalExpense

        lifecycleScope.launch {
            val rate = fetchExchangeRate("IDR", selectedCurrency)
            if (rate > 0) {
                val converted = balance * rate
                val formatter = NumberFormat.getCurrencyInstance(getLocaleForCurrency(selectedCurrency))
                convertedBalance.text = "Balance: ${formatter.format(converted)}"
            } else {
                convertedBalance.text = "Error fetching rate"
            }
        }
    }
    private fun updateBalanceUI() {
        val balance = totalIncome - totalExpense
        val formatter = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        formatter.maximumFractionDigits = 0
        textTotalIncome.text = formatter.format(totalIncome)
        textTotalExpense.text = formatter.format(totalExpense)
        textBalance.text = formatter.format(balance)

        // update balance konversi juga
        updateConvertedBalance()
    }



    private suspend fun fetchExchangeRate(from: String, to: String): Double {
        return withContext(Dispatchers.IO) {
            try {
                val apiUrl = "https://open.er-api.com/v6/latest/$from"
                val url = URL(apiUrl)
                val connection = url.openConnection() as HttpURLConnection

                connection.inputStream.bufferedReader().use { reader ->
                    val result = reader.readText()
                    val json = JSONObject(result)

                    // cek dulu status respons
                    if (json.optString("result") == "success") {
                        val rates = json.getJSONObject("rates")
                        return@withContext rates.optDouble(to, 0.0)
                    } else {
                        return@withContext 0.0
                    }
                }
            } catch (e: Exception) {
                Log.e("ExchangeRate", "Error fetching exchange rate", e)
                0.0
            }
        }
    }


    private fun getLocaleForCurrency(currency: String): Locale {
        return when (currency) {
            "USD" -> Locale.US
            "EUR" -> Locale.GERMANY
            "JPY" -> Locale.JAPAN
            "GBP" -> Locale.UK
            "AUD" -> Locale("en", "AU")
            "SGD" -> Locale("en", "SG")
            "CNY" -> Locale.CHINA
            "KRW" -> Locale.KOREA
            "MYR" -> Locale("ms", "MY")
            "THB" -> Locale("th", "TH")
            else -> Locale.US
        }
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
