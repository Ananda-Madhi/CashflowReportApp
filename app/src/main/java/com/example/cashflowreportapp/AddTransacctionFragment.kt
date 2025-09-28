package com.example.cashflowreportapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.RadioButton
import android.widget.Spinner
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.cashflowreportapp.database.AppDatabase
import com.example.cashflowreportapp.database.Transaction
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class AddTransactionFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_add_transacction, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val inputTitle = view.findViewById<TextInputEditText>(R.id.input_title)
        val inputAmount = view.findViewById<TextInputEditText>(R.id.input_amount)
        val radioIncome = view.findViewById<RadioButton>(R.id.radio_income)
        val buttonSave = view.findViewById<Button>(R.id.button_save)
        val spinnerCurrency = view.findViewById<Spinner>(R.id.spinner_currency) // <-- TAMBAHAN

        // Setup Spinner Adapter
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.transaction_currencies,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCurrency.adapter = adapter
        }

        buttonSave.setOnClickListener {
            val title = inputTitle.text.toString()
            val amount = inputAmount.text.toString().toDoubleOrNull()
            val type = if (radioIncome.isChecked) "INCOME" else "EXPENSE"
            val date = System.currentTimeMillis()
            val currency = spinnerCurrency.selectedItem.toString() // <-- TAMBAHAN

            if (title.isNotBlank() && amount != null && amount > 0) {
                val transaction = Transaction(
                    title = title,
                    amount = amount,
                    type = type,
                    date = date,
                    currency = currency // <-- TAMBAHAN
                )
                lifecycleScope.launch {
                    AppDatabase.getDatabase(requireContext()).transactionDao().insert(transaction)
                    // Kembali ke halaman sebelumnya setelah berhasil menyimpan
                    findNavController().navigateUp()
                }
            } else {
                Toast.makeText(context, "Judul dan Jumlah harus diisi dengan benar", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
