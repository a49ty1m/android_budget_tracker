package com.smilo.budgettracker.ui;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.smilo.budgettracker.R;
import com.smilo.budgettracker.db.TransactionEntity;
import com.smilo.budgettracker.db.TransactionWithAccount;
import java.util.Locale;

public class TransactionFragment extends Fragment {
    
    private EditText etAmount, etSource, etNote;
    private Spinner spType;
    private MaterialButton btnAddTransaction, btnDeleteData;
    private RecyclerView rvTransactions;
    private TextView tvTotalBalance, tvSummaryIncome, tvSummaryExpense, tvEmptyHistory;
    private BudgetViewModel viewModel;
    private TransactionAdapter adapter;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_transactions, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initializeViews(view);
        setupViewModel();
        setupTypeSpinner();
        setupRecyclerView();
    }
    
    private void initializeViews(View view) {
        etAmount = view.findViewById(R.id.et_amount);
        etSource = view.findViewById(R.id.et_source);
        etNote = view.findViewById(R.id.et_note);
        spType = view.findViewById(R.id.sp_type);
        btnAddTransaction = view.findViewById(R.id.btn_add_transaction);
        btnDeleteData = view.findViewById(R.id.btn_delete_data);
        rvTransactions = view.findViewById(R.id.rv_transactions);
        tvTotalBalance = view.findViewById(R.id.tv_total_balance);
        tvSummaryIncome = view.findViewById(R.id.tv_summary_income);
        tvSummaryExpense = view.findViewById(R.id.tv_summary_expense);
        tvEmptyHistory = view.findViewById(R.id.tv_empty_history);
        
        btnAddTransaction.setOnClickListener(v -> addTransaction());
        btnDeleteData.setOnClickListener(v -> showDeleteConfirmation());
    }
    
    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(BudgetViewModel.class);
        
        viewModel.getTotalBalance().observe(getViewLifecycleOwner(), balance -> 
            tvTotalBalance.setText(String.format(Locale.getDefault(), "₹%.2f", balance)));
            
        viewModel.getTotalIncome().observe(getViewLifecycleOwner(), income -> 
            tvSummaryIncome.setText(String.format(Locale.getDefault(), "%s ₹%.2f", getString(R.string.total_income), income)));
            
        viewModel.getTotalExpense().observe(getViewLifecycleOwner(), expense -> 
            tvSummaryExpense.setText(String.format(Locale.getDefault(), "%s ₹%.2f", getString(R.string.total_expense), expense)));
            
        viewModel.getAllTransactions().observe(getViewLifecycleOwner(), transactions -> {
            if (transactions == null || transactions.isEmpty()) {
                tvEmptyHistory.setVisibility(View.VISIBLE);
                rvTransactions.setVisibility(View.GONE);
            } else {
                tvEmptyHistory.setVisibility(View.GONE);
                rvTransactions.setVisibility(View.VISIBLE);
                adapter.setTransactions(transactions);
            }
        });
    }
    
    private void setupTypeSpinner() {
        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.transaction_types, android.R.layout.simple_spinner_item);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spType.setAdapter(typeAdapter);
        
        spType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String type = parent.getItemAtPosition(position).toString();
                if (type.contains("Expense")) {
                    btnAddTransaction.setText(R.string.add_expense);
                    btnAddTransaction.setBackgroundTintList(ColorStateList.valueOf(requireContext().getColor(R.color.expense_red)));
                } else if (type.contains("Income")) {
                    btnAddTransaction.setText(R.string.add_income);
                    btnAddTransaction.setBackgroundTintList(ColorStateList.valueOf(requireContext().getColor(R.color.income_green)));
                } else {
                    btnAddTransaction.setText(R.string.add_transaction);
                    btnAddTransaction.setBackgroundTintList(ColorStateList.valueOf(requireContext().getColor(R.color.accent_amber)));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }
    
    private void setupRecyclerView() {
        rvTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TransactionAdapter();
        rvTransactions.setAdapter(adapter);
    }
    
    private void addTransaction() {
        String amountStr = etAmount.getText().toString().trim();
        String typeFull = spType.getSelectedItem().toString();
        String type = typeFull.split(" ")[0]; // Get "Income" from "Income 💰"
        String source = etSource.getText().toString().trim();
        String note = etNote.getText().toString().trim();
        
        if (!amountStr.isEmpty()) {
            double amount = Double.parseDouble(amountStr);
            long now = System.currentTimeMillis();
            TransactionEntity transaction = new TransactionEntity(1, type, amount, source, "", note, now);
            viewModel.insertTransaction(transaction);
            
            Toast.makeText(requireContext(), R.string.money_move_captured, Toast.LENGTH_SHORT).show();
            
            etAmount.setText("");
            etSource.setText("");
            etNote.setText("");
        } else {
            Toast.makeText(requireContext(), R.string.enter_amount_hint, Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showDeleteConfirmation() {
        new AlertDialog.Builder(requireContext())
            .setTitle(R.string.nuclear_option)
            .setMessage(R.string.nuclear_desc)
            .setPositiveButton(R.string.nuclear_positive, (dialog, which) -> {
                deleteAllData();
                Toast.makeText(requireContext(), R.string.poof_gone, Toast.LENGTH_LONG).show();
            })
            .setNegativeButton(R.string.nuclear_negative, null)
            .show();
    }
    
    private void deleteAllData() {
        viewModel.getAllTransactions().observe(getViewLifecycleOwner(), transactions -> {
            if (transactions != null) {
                for (TransactionWithAccount t : transactions) {
                    viewModel.deleteTransaction(t.transaction);
                }
            }
        });
    }
}
