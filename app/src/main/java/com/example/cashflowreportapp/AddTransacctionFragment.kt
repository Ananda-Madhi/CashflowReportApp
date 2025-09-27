package com.example.cashflowreportapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.cashflowreportapp.database.AppDatabase
import com.example.cashflowreportapp.database.Transaction
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class AddTransactionFragment : Fragment() {

    // 👇 batas maksimal transaksi
    private val MAX_LIMIT = 100_000_000.0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_add_transacction, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val titleInput: TextInputEditText = view.findViewById(R.id.input_title)
        val amountInput: TextInputEditText = view.findViewById(R.id.input_amount)
        val typeRadioGroup: RadioGroup = view.findViewById(R.id.radio_group_type)
        val saveButton: Button = view.findViewById(R.id.button_save)

        val database = AppDatabase.getDatabase(requireContext())

        saveButton.setOnClickListener {
            val title = titleInput.text.toString()
            val amount = amountInput.text.toString().toDoubleOrNull()

            if (title.isEmpty() || amount == null) {
                Toast.makeText(context, "Judul dan Jumlah tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ✅ cek batas maksimal
            if (amount > MAX_LIMIT) {
                Toast.makeText(
                    context,
                    "Jumlah maksimal transaksi adalah Rp 100.000.000",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            val selectedTypeId = typeRadioGroup.checkedRadioButtonId
            val type = if (selectedTypeId == R.id.radio_income) "INCOME" else "EXPENSE"

            val transaction = Transaction(
                title = title,
                amount = amount,
                date = System.currentTimeMillis(),
                type = type
            )

            lifecycleScope.launch {
                database.transactionDao().insert(transaction)
                Toast.makeText(context, "Transaksi berhasil disimpan", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp() // Kembali ke layar sebelumnya
            }
        }
    }
}
