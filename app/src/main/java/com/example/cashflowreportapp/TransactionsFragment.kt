package com.example.cashflowreportapp

import android.os.Bundle
import android.util.Log
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
import java.math.BigDecimal
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.util.*

class TransactionsFragment : Fragment() {

    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var textTotalIncome: TextView
    private lateinit var textTotalExpense: TextView
    private lateinit var textBalance: TextView
    private lateinit var currencySpinner: Spinner
    private lateinit var convertedBalance: TextView
    private lateinit var allTransactions: List<Transaction>

    private var totalIncomeInBaseCurrency: Double = 0.0
    private var totalExpenseInBaseCurrency: Double = 0.0
    private val baseCurrency = "IDR"

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

        transactionAdapter = TransactionAdapter(
            emptyList(),
            onEditClick = { transaction -> showEditDialog(transaction) },
            onDeleteClick = { transaction -> showDeleteDialog(transaction) }
        )
        recyclerView.adapter = transactionAdapter

        val database = AppDatabase.getDatabase(requireContext())
        val transactionDao = database.transactionDao()

        transactionDao.getAllTransactions().observe(viewLifecycleOwner) { transactions ->
            allTransactions = transactions
            transactionAdapter.updateData(transactions)
            calculateBalance()
        }

        setupSwipeToDelete(recyclerView)
        setupCurrencySpinner()
    }

    private fun setupCurrencySpinner() {
        val adapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.transaction_currencies,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        currencySpinner.adapter = adapter

        currencySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateConvertedBalance()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun calculateBalance() {
        lifecycleScope.launch {
            var income = 0.0
            var expense = 0.0

            val rateCache = mutableMapOf<String, Double>()

            for (transaction in allTransactions) {
                val rate = if (transaction.currency == baseCurrency) {
                    1.0
                } else {
                    if (rateCache.containsKey(transaction.currency)) {
                        rateCache[transaction.currency]!!
                    } else {
                        val fetchedRate = fetchExchangeRate(transaction.currency, baseCurrency)
                        rateCache[transaction.currency] = fetchedRate
                        fetchedRate
                    }
                }

                val amountInBase = transaction.amount * rate
                if (transaction.type == "INCOME") {
                    income += amountInBase
                } else {
                    expense += amountInBase
                }
            }

            totalIncomeInBaseCurrency = income
            totalExpenseInBaseCurrency = expense

            withContext(Dispatchers.Main) {
                updateBalanceUI()
            }
        }
    }

    private fun updateBalanceUI() {
        val balance = totalIncomeInBaseCurrency - totalExpenseInBaseCurrency
        val formatter = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        formatter.maximumFractionDigits = 0

        textTotalIncome.text = formatter.format(totalIncomeInBaseCurrency)
        textTotalExpense.text = formatter.format(totalExpenseInBaseCurrency)
        textBalance.text = formatter.format(balance)

        updateConvertedBalance()
    }

    private fun updateConvertedBalance() {
        val selectedCurrency = currencySpinner.selectedItem.toString()
        val balanceInBase = totalIncomeInBaseCurrency - totalExpenseInBaseCurrency

        if (selectedCurrency == baseCurrency) {
            convertedBalance.visibility = View.GONE
            return
        }
        convertedBalance.visibility = View.VISIBLE

        lifecycleScope.launch {
            val rate = fetchExchangeRate(baseCurrency, selectedCurrency)
            if (rate > 0) {
                val converted = balanceInBase * rate
                val formatter = NumberFormat.getCurrencyInstance(getLocaleForCurrency(selectedCurrency))
                convertedBalance.text = "Balance: ${formatter.format(converted)}"
            } else {
                convertedBalance.text = "Error fetching rate for $selectedCurrency"
            }
        }
    }

    private suspend fun fetchExchangeRate(from: String, to: String): Double {
        return withContext(Dispatchers.IO) {
            try {
                val apiUrl = "https://open.er-api.com/v6/latest/$from"
                val url = URL(apiUrl)
                val connection = url.openConnection() as HttpURLConnection
                val result = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(result)

                if (json.optString("result") == "success") {
                    json.getJSONObject("rates").optDouble(to, 0.0)
                } else {
                    0.0
                }
            } catch (e: Exception) {
                Log.e("ExchangeRate", "Error fetching exchange rate from $from to $to", e)
                0.0
            }
        }
    }

    private fun showEditDialog(transaction: Transaction) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.fragment_edit_transaction, null)
        val titleInput = dialogView.findViewById<TextInputEditText>(R.id.input_title)
        val amountInput = dialogView.findViewById<TextInputEditText>(R.id.input_amount)
        val radioIncome = dialogView.findViewById<RadioButton>(R.id.radio_income)
        val radioExpense = dialogView.findViewById<RadioButton>(R.id.radio_expense)
        val spinnerCurrency = dialogView.findViewById<Spinner>(R.id.spinner_currency)

        val adapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.transaction_currencies,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCurrency.adapter = adapter

        titleInput.setText(transaction.title)
        amountInput.setText(BigDecimal(transaction.amount).toPlainString())

        if (transaction.type == "INCOME") radioIncome.isChecked = true else radioExpense.isChecked = true
        val currencyPosition = adapter.getPosition(transaction.currency)
        spinnerCurrency.setSelection(currencyPosition)

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Transaksi")
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val updated = transaction.copy(
                    title = titleInput.text.toString(),
                    amount = amountInput.text.toString().toDoubleOrNull() ?: transaction.amount,
                    type = if (radioIncome.isChecked) "INCOME" else "EXPENSE",
                    currency = spinnerCurrency.selectedItem.toString()
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
            .setMessage("Hapus Transaksi? \"${transaction.title}\"?")
            .setPositiveButton("Hapus") { _, _ ->
                lifecycleScope.launch {
                    AppDatabase.getDatabase(requireContext()).transactionDao().delete(transaction)
                }
            }
            .setNegativeButton("Batal") { dialog, _ ->
                position?.let { transactionAdapter.notifyItemChanged(it) }
                dialog.dismiss()
            }
            .setOnCancelListener {
                position?.let { transactionAdapter.notifyItemChanged(it) }
            }
            .show()
    }

    private fun setupSwipeToDelete(recyclerView: RecyclerView) {
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val transaction = transactionAdapter.getTransactionAt(position)
                if (direction == ItemTouchHelper.LEFT) {
                    showDeleteDialog(transaction, position)
                } else {
                    showEditDialog(transaction)
                    transactionAdapter.notifyItemChanged(position)
                }
            }
        }).attachToRecyclerView(recyclerView)
    }

    private fun getLocaleForCurrency(currency: String): Locale {
        return when (currency) {
            "IDR" -> Locale("in", "ID")
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
}
