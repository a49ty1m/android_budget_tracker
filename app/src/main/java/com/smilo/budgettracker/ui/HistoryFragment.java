package com.smilo.budgettracker.ui;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;
import com.smilo.budgettracker.R;
import com.smilo.budgettracker.db.TransactionEntity;
import com.smilo.budgettracker.db.TransactionWithAccount;
import android.graphics.Typeface;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import java.util.Calendar;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.util.Map;

public class HistoryFragment extends Fragment {

    private RecyclerView rvHistory;
    private TextView tvEmptyHistory;
    private LinearLayout llHistoryBreakdown;
    private LinearLayout llDynamicSummary;
    private ChipGroup cgFilters;
    private BudgetViewModel viewModel;
    private TransactionAdapter adapter;
    private List<TransactionWithAccount> allTransactions = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        rvHistory = view.findViewById(R.id.rv_history);
        tvEmptyHistory = view.findViewById(R.id.tv_empty_history);
        llHistoryBreakdown = view.findViewById(R.id.ll_history_breakdown);
        llDynamicSummary = view.findViewById(R.id.ll_dynamic_summary);
        cgFilters = view.findViewById(R.id.cg_filters);

        setupRecyclerView();
        setupViewModel();
        setupFilters();
        cgFilters.check(R.id.chip_this_month);  // Default to monthly summary
    }

    private void setupRecyclerView() {
        rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TransactionAdapter();
        rvHistory.setAdapter(adapter);
        
        adapter.setOnTransactionClickListener(this::showEditTransactionDialog);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(requireActivity()).get(BudgetViewModel.class);

        viewModel.getAllTransactions().observe(getViewLifecycleOwner(), transactions -> {
            if (transactions != null) {
                allTransactions = transactions;
                applyFilter(cgFilters.getCheckedChipId());
            }
        });
    }

    private void setupFilters() {
        cgFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                applyFilter(checkedIds.get(0));
            }
        });
    }

    private void applyFilter(int checkedId) {
        List<TransactionWithAccount> filteredList = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        
        if (checkedId == R.id.chip_this_week) {
            cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
            long weekStart = cal.getTimeInMillis();
            for (TransactionWithAccount t : allTransactions) if (t.transaction.createdAt >= weekStart) filteredList.add(t);
        } else if (checkedId == R.id.chip_this_month) {
            cal.set(Calendar.DAY_OF_MONTH, 1);
            long monthStart = cal.getTimeInMillis();
            for (TransactionWithAccount t : allTransactions) if (t.transaction.createdAt >= monthStart) filteredList.add(t);
        } else {
            filteredList.addAll(allTransactions);
        }

        updateUI(filteredList);
    }

    private void updateUI(List<TransactionWithAccount> transactions) {
        double totalExpense = 0;
        double totalIncome = 0;
        for (TransactionWithAccount t : transactions) {
            if ("Expense".equalsIgnoreCase(t.transaction.type)) {
                totalExpense += t.transaction.amount;
            } else if ("Income".equalsIgnoreCase(t.transaction.type)) {
                totalIncome += t.transaction.amount;
            }
        }
        double net = totalIncome - totalExpense;
        // Clear dynamic summary area
        llDynamicSummary.removeAllViews();

        float density = getResources().getDisplayMetrics().density;

        // Spent Card
        MaterialCardView cvSpent = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams paramsSpent = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        paramsSpent.setMargins(0, 0, 0, 8);
        cvSpent.setLayoutParams(paramsSpent);
        cvSpent.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.expense_glow));
        cvSpent.setRadius(12 * density);
        cvSpent.setCardElevation(2);
        TextView tvSpent = new TextView(requireContext());
        tvSpent.setText(getString(R.string.spent_format, "₹", totalExpense));
        tvSpent.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        tvSpent.setTextSize(22);
        tvSpent.setTypeface(null, Typeface.BOLD);
        tvSpent.setPadding(20, 16, 20, 16);
        cvSpent.addView(tvSpent);
        llDynamicSummary.addView(cvSpent);

        // Earned Card
        MaterialCardView cvEarned = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams paramsEarned = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        paramsEarned.setMargins(0, 0, 0, 8);
        cvEarned.setLayoutParams(paramsEarned);
        cvEarned.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.income_glow));
        cvEarned.setRadius(12 * density);
        cvEarned.setCardElevation(2);
        TextView tvEarned = new TextView(requireContext());
        tvEarned.setText(getString(R.string.earned_format, "₹", totalIncome));
        tvEarned.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        tvEarned.setTextSize(22);
        tvEarned.setTypeface(null, Typeface.BOLD);
        tvEarned.setPadding(20, 16, 20, 16);
        cvEarned.addView(tvEarned);
        llDynamicSummary.addView(cvEarned);

        // Net Card - Always yellow highlight
        MaterialCardView cvNet = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams paramsNet = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        paramsNet.setMargins(0, 0, 0, 8);
        cvNet.setLayoutParams(paramsNet);
        cvNet.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.net_yellow_glow));
        cvNet.setRadius(16 * density);
        cvNet.setCardElevation(6);
        TextView tvNet = new TextView(requireContext());
        String netPrefix = net >= 0 ? "+" : "-";
        tvNet.setText(getString(R.string.net_format, netPrefix, "₹", Math.abs(net)));
        tvNet.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_yellow));
        tvNet.setTextSize(26);
        tvNet.setTypeface(null, Typeface.BOLD_ITALIC);
        tvNet.setPadding(20, 20, 20, 20);
        cvNet.addView(tvNet);
        llDynamicSummary.addView(cvNet);

        if (transactions.isEmpty()) {
            tvEmptyHistory.setVisibility(View.VISIBLE);
            rvHistory.setVisibility(View.GONE);
            llHistoryBreakdown.removeAllViews();
        } else {
            tvEmptyHistory.setVisibility(View.GONE);
            rvHistory.setVisibility(View.VISIBLE);
            adapter.setTransactions(transactions);
            calculateBreakdown(transactions, totalExpense, totalIncome);
        }
    }

    private void calculateBreakdown(List<TransactionWithAccount> transactions, double totalExpense, double totalIncome) {
        llHistoryBreakdown.removeAllViews();
        double totalAmount = totalExpense + totalIncome;
        if (totalAmount == 0) return;

        Map<String, Double> categoryTotals = new HashMap<>();
        for (TransactionWithAccount t : transactions) {
            String key = t.transaction.type + ": " + t.transaction.category;
            categoryTotals.merge(key, t.transaction.amount, Double::sum);
        }

        List<Map.Entry<String, Double>> sortedList = new ArrayList<>(categoryTotals.entrySet());
        sortedList.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

        for (Map.Entry<String, Double> entry : sortedList) {
            double percentage = (entry.getValue() / totalAmount) * 100;
            addBreakdownBar(entry.getKey(), (int) percentage);
        }
    }

    private void addBreakdownBar(String category, int percentage) {
        View barView = LayoutInflater.from(requireContext()).inflate(R.layout.item_category_breakdown, llHistoryBreakdown, false);
        TextView tvLabel = barView.findViewById(R.id.tv_category_label);
        TextView tvPercent = barView.findViewById(R.id.tv_category_percentage);
        ProgressBar pbBar = barView.findViewById(R.id.pb_category_bar);

        tvLabel.setText(category);
        tvPercent.setText(getString(R.string.percentage_format, percentage));
        pbBar.setProgress(percentage);
        // Dynamic color by type
        int colorRes = category.startsWith("Expense") ? R.color.overspending : R.color.money_left;
        pbBar.setProgressTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), colorRes)));

        llHistoryBreakdown.addView(barView);
    }

    private void showEditTransactionDialog(TransactionEntity transaction) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_transaction, null);
        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        EditText etAmount = dialogView.findViewById(R.id.et_dialog_amount);
        EditText etNote = dialogView.findViewById(R.id.et_dialog_note);
        MaterialButton btnDate = dialogView.findViewById(R.id.btn_dialog_date);
        MaterialButton btnTime = dialogView.findViewById(R.id.btn_dialog_time);

        tvTitle.setText(getString(R.string.edit_transaction_title, transaction.type));
        etAmount.setText(String.format(Locale.getDefault(), "%.0f", transaction.amount));
        etNote.setText(transaction.note);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(transaction.createdAt);

        SimpleDateFormat dateSdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        SimpleDateFormat timeSdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());

        btnDate.setText(dateSdf.format(calendar.getTime()));
        btnTime.setText(timeSdf.format(calendar.getTime()));

        btnDate.setOnClickListener(v -> new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            btnDate.setText(dateSdf.format(calendar.getTime()));
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show());

        btnTime.setOnClickListener(v -> new TimePickerDialog(requireContext(), (view, hourOfDay, minute) -> {
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            calendar.set(Calendar.MINUTE, minute);
            btnTime.setText(timeSdf.format(calendar.getTime()));
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show());

        new AlertDialog.Builder(requireContext(), R.style.Theme_BudgetTracker)
                .setView(dialogView)
                .setPositiveButton(R.string.update, (dialog, which) -> {
                    String amountStr = etAmount.getText().toString().trim();
                    if (!amountStr.isEmpty()) {
                        try {
                            transaction.amount = Double.parseDouble(amountStr);
                            transaction.note = etNote.getText().toString().trim();
                            transaction.createdAt = calendar.getTimeInMillis();
                            viewModel.updateTransaction(transaction);
                            Toast.makeText(getContext(), R.string.entry_repaired, Toast.LENGTH_SHORT).show();
                        } catch (NumberFormatException e) {
                            Toast.makeText(getContext(), R.string.invalid_amount, Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton(R.string.delete, (dialog, which) -> new AlertDialog.Builder(requireContext(), R.style.Theme_BudgetTracker)
                        .setTitle(R.string.dialog_delete_title)
                        .setMessage(R.string.dialog_delete_message)
                        .setPositiveButton(R.string.delete, (d, w) -> {
                            viewModel.deleteTransaction(transaction);
                            Toast.makeText(getContext(), R.string.entry_deleted, Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show())
                .setNeutralButton(R.string.cancel, null)
                .show();
    }
}

