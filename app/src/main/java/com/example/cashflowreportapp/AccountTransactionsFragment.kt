package com.example.cashflowreportapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cashflowreportapp.database.AppDatabase
import com.example.cashflowreportapp.database.Transaction
import com.itextpdf.text.Document
import com.itextpdf.text.Paragraph
import com.itextpdf.text.pdf.PdfWriter
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.os.Environment

class AccountTransactionsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var textTotalIncome: TextView
    private lateinit var textTotalExpense: TextView
    private lateinit var textBalance: TextView
    private lateinit var textAccountName: TextView
    private lateinit var buttonExportPdf: Button
    private lateinit var backButton: ImageView // Tambahkan ini
    private lateinit var adapter: TransactionAdapter

    private var totalIncome = 0.0
    private var totalExpense = 0.0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_account_transactions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.recycler_view_account_transactions)
        textTotalIncome = view.findViewById(R.id.text_total_income)
        textTotalExpense = view.findViewById(R.id.text_total_expense)
        textBalance = view.findViewById(R.id.text_balance)
        textAccountName = view.findViewById(R.id.text_account_name)
        buttonExportPdf = view.findViewById(R.id.button_export_pdf)
        backButton = view.findViewById(R.id.iv_back) // Inisialisasi tombol kembali

        val accountName = arguments?.getString("account_name") ?: "Unknown Account"
        textAccountName.text = accountName

        // Logika untuk tombol kembali
        backButton.setOnClickListener {
            findNavController().popBackStack()
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = TransactionAdapter(
            onEditClick = { transaction ->
                Toast.makeText(context, "Edit for '${transaction.title}' clicked", Toast.LENGTH_SHORT).show()
            },
            onDeleteClick = { transaction ->
                Toast.makeText(context, "Delete for '${transaction.title}' clicked", Toast.LENGTH_SHORT).show()
            }
        )
        recyclerView.adapter = adapter

        val dao = AppDatabase.getDatabase(requireContext()).transactionDao()
        dao.getTransactionsByAccount(accountName).observe(viewLifecycleOwner) { transactions ->
            adapter.submitList(transactions)
            updateSummary(transactions)
        }

        buttonExportPdf.setOnClickListener {
            exportToPdf(accountName)
        }
    }

    private fun updateSummary(transactions: List<Transaction>) {
        totalIncome = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
        totalExpense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val balance = totalIncome - totalExpense

        val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("in-ID"))
        formatter.maximumFractionDigits = 0
        textTotalIncome.text = formatter.format(totalIncome)
        textTotalExpense.text = formatter.format(totalExpense)
        textBalance.text = formatter.format(balance)
    }

    private fun exportToPdf(accountName: String) {
        // ... (Fungsi exportToPdf tetap sama)
        lifecycleScope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getDatabase(requireContext()).transactionDao()
            val transactions = dao.getTransactionsByAccount(accountName).value ?: emptyList()

            if (transactions.isEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "No transactions to export", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val pdfDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "CashFlowReports")
            if (!pdfDir.exists()) pdfDir.mkdirs()

            val pdfFile = File(pdfDir, "${accountName}_transactions_${System.currentTimeMillis()}.pdf")

            try {
                val document = Document()
                PdfWriter.getInstance(document, FileOutputStream(pdfFile))
                document.open()
                document.add(Paragraph("Transactions for $accountName\n\n"))

                for (t in transactions) {
                    val formattedAmount = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("in-ID")).format(t.amount)
                    document.add(Paragraph("${t.title} (${t.type}): $formattedAmount"))
                }
                document.close()

                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Exported to ${pdfFile.path}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Failed to export PDF", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}