package com.example.cashflowreportapp


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cashflowreportapp.database.AppDatabase
import com.example.cashflowreportapp.database.Transaction
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import com.itextpdf.text.Document
import com.itextpdf.text.Paragraph
import com.itextpdf.text.pdf.PdfWriter



class AccountTransactionsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var textTotalIncome: TextView
    private lateinit var textTotalExpense: TextView
    private lateinit var textBalance: TextView
    private lateinit var textAccountName: TextView
    private lateinit var buttonExportPdf: Button
    private lateinit var adapter: TransactionAdapter

    private var totalIncome = 0.0
    private var totalExpense = 0.0

    companion object {
        private const val ARG_ACCOUNT = "account_name"

        fun newInstance(accountName: String): AccountTransactionsFragment {
            val fragment = AccountTransactionsFragment()
            val args = Bundle()
            args.putString(ARG_ACCOUNT, accountName)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_account_transactions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.findViewById(R.id.recycler_view_account_transactions)
        textTotalIncome = view.findViewById(R.id.text_total_income)
        textTotalExpense = view.findViewById(R.id.text_total_expense)
        textBalance = view.findViewById(R.id.text_balance)
        textAccountName = view.findViewById(R.id.text_account_name)
        buttonExportPdf = view.findViewById(R.id.button_export_pdf)

        val accountName = arguments?.getString(ARG_ACCOUNT) ?: "Unknown Account"
        textAccountName.text = "Account: $accountName"

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = TransactionAdapter(emptyList())
        recyclerView.adapter = adapter

        val dao = AppDatabase.getDatabase(requireContext()).transactionDao()
        dao.getTransactionsByAccount(accountName).observe(viewLifecycleOwner) { transactions ->
            adapter.updateData(transactions)
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

        val formatter = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        formatter.maximumFractionDigits = 0
        textTotalIncome.text = "Total Income: ${formatter.format(totalIncome)}"
        textTotalExpense.text = "Total Expense: ${formatter.format(totalExpense)}"
        textBalance.text = "Balance: ${formatter.format(balance)}"
    }

    private fun exportToPdf(accountName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getDatabase(requireContext()).transactionDao()
            val transactions = dao.getTransactionsByAccount(accountName).value ?: emptyList()

            val pdfDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "CashFlowReports")
            if (!pdfDir.exists()) pdfDir.mkdirs()

            val pdfFile = File(pdfDir, "${accountName}_transactions.pdf")

            val document = Document()
            PdfWriter.getInstance(document, FileOutputStream(pdfFile))
            document.open()
            document.add(Paragraph("Transactions for $accountName\n\n"))

            for (t in transactions) {
                document.add(Paragraph("${t.title} - ${t.type} - Rp ${t.amount}"))
            }

            document.close()

            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Exported to ${pdfFile.path}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
