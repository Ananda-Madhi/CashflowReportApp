package com.example.cashflowreportapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.android.material.textfield.TextInputEditText
import androidx.lifecycle.lifecycleScope
import com.example.cashflowreportapp.database.AppDatabase
import com.example.cashflowreportapp.database.Transaction
import kotlinx.coroutines.launch

class AddTransactionFragment : Fragment() {

    private lateinit var spinnerAccount: Spinner

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_transacction, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val inputTitle = view.findViewById<TextInputEditText>(R.id.input_title)
        val inputAmount = view.findViewById<TextInputEditText>(R.id.input_amount)
        val radioIncome = view.findViewById<RadioButton>(R.id.radio_income)
        val radioExpense = view.findViewById<RadioButton>(R.id.radio_expense)
        val buttonSave = view.findViewById<Button>(R.id.button_save)

        // ✅ Setup spinner for account selection
        spinnerAccount = view.findViewById(R.id.spinnerAccount)
        val accountAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            AccountsFragment.globalAccounts
        )
        spinnerAccount.adapter = accountAdapter

        buttonSave.setOnClickListener {
            val title = inputTitle.text.toString()
            val amount = inputAmount.text.toString().toDoubleOrNull() ?: 0.0
            val type = if (radioIncome.isChecked) "INCOME" else "EXPENSE"
            val date = System.currentTimeMillis()
            val account = spinnerAccount.selectedItem?.toString() ?: ""  // ✅ FIXED

            if (title.isNotBlank() && amount > 0 && account.isNotBlank()) {
                val transaction = Transaction(0, title, amount, type, date, account)
                lifecycleScope.launch {
                    AppDatabase.getDatabase(requireContext()).transactionDao().insert(transaction)
                    activity?.onBackPressed()
                }
            } else {
                Toast.makeText(requireContext(), "Please fill all fields!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
