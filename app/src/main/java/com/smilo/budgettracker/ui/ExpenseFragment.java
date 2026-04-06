package com.smilo.budgettracker.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.button.MaterialButton;
import com.smilo.budgettracker.R;
import com.smilo.budgettracker.db.TransactionEntity;

public class ExpenseFragment extends Fragment {

    private EditText etAmount, etSource, etNote;
    private MaterialButton btnAddExpense;
    private BudgetViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_expense, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(BudgetViewModel.class);

        etAmount = view.findViewById(R.id.et_amount);
        etSource = view.findViewById(R.id.et_source);
        etNote = view.findViewById(R.id.et_note);
        btnAddExpense = view.findViewById(R.id.btn_add_expense);

        btnAddExpense.setOnClickListener(v -> addExpense());
    }

    private void addExpense() {
        String amountStr = etAmount.getText().toString().trim();
        String source = etSource.getText().toString().trim();
        String note = etNote.getText().toString().trim();

        if (amountStr.isEmpty()) {
            etAmount.setError("Required");
            return;
        }

        double amount = Double.parseDouble(amountStr);
        long now = System.currentTimeMillis();
        // Using user ID 1 as default for now
        TransactionEntity transaction = new TransactionEntity(1, "Expense", amount, source, "", note, now);
        viewModel.insertTransaction(transaction);

        etAmount.setText("");
        etSource.setText("");
        etNote.setText("");
        Toast.makeText(getContext(), "Expense added successfully", Toast.LENGTH_SHORT).show();
    }
}
