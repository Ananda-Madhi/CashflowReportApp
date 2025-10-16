package com.example.cashflowreportapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class AccountsFragment : Fragment() {

    private lateinit var listView: ListView
    private lateinit var inputAccount: EditText
    private lateinit var addButton: Button
    private lateinit var adapter: ArrayAdapter<String>
    private var accountList = globalAccounts

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_accounts, container, false)

        listView = view.findViewById(R.id.listViewAccounts)
        inputAccount = view.findViewById(R.id.editTextAccount)
        addButton = view.findViewById(R.id.buttonAddAccount)

        // Ensure default account exists
        if (!globalAccounts.contains("You")) {
            globalAccounts.add("You")
        }

        adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, accountList)
        listView.adapter = adapter

        // ✅ Add new account
        addButton.setOnClickListener {
            val newAccount = inputAccount.text.toString().trim()
            if (newAccount.isNotEmpty() && !accountList.contains(newAccount)) {
                accountList.add(newAccount)
                adapter.notifyDataSetChanged()
                inputAccount.text.clear()
            } else {
                Toast.makeText(requireContext(), "Account name cannot be empty or duplicate", Toast.LENGTH_SHORT).show()
            }
        }

        // ✅ Navigate to AccountTransactionsFragment when clicked
        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedAccount = accountList[position]
            val bundle = Bundle().apply {
                putString("account_name", selectedAccount)
            }

            // Navigate using Navigation Component
            findNavController().navigate(R.id.accountTransactionsFragment, bundle)
        }

        return view
    }

    companion object {
        var globalAccounts = arrayListOf<String>()
    }
}
