package com.smilo.budgettracker.ui;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
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
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.smilo.budgettracker.R;
import com.smilo.budgettracker.db.SavingEntity;
import com.smilo.budgettracker.db.SavingWithAccount;
import com.smilo.budgettracker.db.TransactionEntity;
import com.smilo.budgettracker.db.TransactionWithAccount;
import com.smilo.budgettracker.db.UserAccountEntity;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeFragment extends Fragment {

    private BudgetViewModel viewModel;
    private TextView tvWelcomeBack, tvRemainingBalance, tvSpentSoFar, tvDailyLimit, tvNoDataChart, tvMySaving, tvDailyLimitLabel;
    private LinearProgressIndicator pbBudgetUsage;
    private RecyclerView rvRecentTransactions, rvHomeSavings;
    private TransactionAdapter adapter;
    private SavingAdapter savingAdapter;
    private ImageButton btnSettings;
    private View btnQuickExpense, btnQuickIncome, btnQuickActive, tvViewAll, tvViewAllSavings;
    private LinearLayout llBreakdownContainer;
    private UserAccountEntity currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvWelcomeBack = view.findViewById(R.id.tv_welcome_back);
        tvRemainingBalance = view.findViewById(R.id.tv_remaining_balance);
        tvSpentSoFar = view.findViewById(R.id.tv_spent_so_far);
        tvDailyLimit = view.findViewById(R.id.tv_daily_limit);
        tvMySaving = view.findViewById(R.id.tv_home_total_saving);

        // Make the Savings stats box clickable to go to Savings deep-dive
        View savingsCard = (View) tvMySaving.getParent().getParent();
        if (savingsCard instanceof com.google.android.material.card.MaterialCardView) {
            savingsCard.setOnClickListener(v -> {
                Bundle bundle = new Bundle();
                bundle.putBoolean("scrollToSavings", true);
                Navigation.findNavController(v).navigate(R.id.action_homeFragment_to_accountFragment, bundle);
            });
        }

        tvNoDataChart = view.findViewById(R.id.tv_no_data_chart);
        pbBudgetUsage = view.findViewById(R.id.pb_budget_usage);
        rvRecentTransactions = view.findViewById(R.id.rv_recent_transactions);
        rvHomeSavings = view.findViewById(R.id.rv_home_savings);
        btnSettings = view.findViewById(R.id.btn_settings);
        btnQuickExpense = view.findViewById(R.id.btn_quick_expense);
        btnQuickIncome = view.findViewById(R.id.btn_quick_income);
        btnQuickActive = view.findViewById(R.id.btn_quick_active);
        llBreakdownContainer = view.findViewById(R.id.ll_breakdown_container);
        tvViewAll = view.findViewById(R.id.tv_view_all);
        tvViewAllSavings = view.findViewById(R.id.tv_view_all_savings);

        setupRecyclerView();
        setupViewModel();
        setupClickListeners();
    }

    private void setupRecyclerView() {
        rvRecentTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TransactionAdapter();
        rvRecentTransactions.setAdapter(adapter);
        
        adapter.setOnTransactionClickListener(this::showEditTransactionDialog);

        rvHomeSavings.setLayoutManager(new LinearLayoutManager(requireContext()));
        savingAdapter = new SavingAdapter();
        rvHomeSavings.setAdapter(savingAdapter);
        
        // Enable editing from HomeFragment
        savingAdapter.setOnSavingClickListener(this::showEditSavingDialog);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(requireActivity()).get(BudgetViewModel.class);
        
        viewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                currentUser = user;
                tvWelcomeBack.setText(String.format("Hey %s 👋", user.userName));
            }
        });

        viewModel.getTotalBalance().observe(getViewLifecycleOwner(), balance -> {
            String balanceStr = String.format(Locale.getDefault(), "₹%.0f", balance);
            tvRemainingBalance.setText(balanceStr);
            updateBudgetUI(viewModel.getTotalIncome().getValue() != null ? viewModel.getTotalIncome().getValue() : 0,
                    viewModel.getTotalExpense().getValue() != null ? viewModel.getTotalExpense().getValue() : 0,
                    balance);
        });

        viewModel.getTotalIncome().observe(getViewLifecycleOwner(), income -> {
            updateBudgetUI(income,
                    viewModel.getTotalExpense().getValue() != null ? viewModel.getTotalExpense().getValue() : 0,
                    viewModel.getTotalBalance().getValue() != null ? viewModel.getTotalBalance().getValue() : 0);
        });

        viewModel.getTotalExpense().observe(getViewLifecycleOwner(), expense -> {
            tvSpentSoFar.setText(String.format(Locale.getDefault(), "₹%.0f", expense));
            updateBudgetUI(viewModel.getTotalIncome().getValue() != null ? viewModel.getTotalIncome().getValue() : 0,
                    expense,
                    viewModel.getTotalBalance().getValue() != null ? viewModel.getTotalBalance().getValue() : 0);
        });
        
        viewModel.getAllTransactions().observe(getViewLifecycleOwner(), transactions -> {
            if (transactions != null && !transactions.isEmpty()) {
                adapter.setTransactions(transactions.subList(0, Math.min(transactions.size(), 5)));
                calculateBreakdown(transactions);
            } else {
                adapter.setTransactions(null);
                tvNoDataChart.setVisibility(View.VISIBLE);
                clearBreakdown();
            }
        });

        viewModel.getAllSavings().observe(getViewLifecycleOwner(), savings -> {
            double totalSavingsAmount = 0;
            if (savings != null && !savings.isEmpty()) {
                savingAdapter.setSavings(savings.subList(0, Math.min(savings.size(), 2)));
                rvHomeSavings.setVisibility(View.VISIBLE);
                for (SavingWithAccount s : savings) {
                    totalSavingsAmount += s.saving.currentAmount;
                }
                calculateSavingsBreakdown(savings);
            } else {
                rvHomeSavings.setVisibility(View.GONE);
                clearSavingsBreakdown();
            }
            tvMySaving.setText(String.format(Locale.getDefault(), "₹%.0f", totalSavingsAmount));
        });
    }

    private void calculateSavingsBreakdown(List<SavingWithAccount> savings) {
        clearSavingsBreakdown();
        
        // Add a header for Savings Insights if needed, or just append to the container
        // For now, let's just append them to the existing llBreakdownContainer
        
        for (SavingWithAccount s : savings) {
            double progress = (s.saving.currentAmount / s.saving.targetAmount) * 100;
            addSavingsBreakdownBar(s.saving.goalName + " " + s.saving.emoji, (int) progress);
        }
    }

    private void clearSavingsBreakdown() {
        // Logic to clear only savings bars if they are mixed
        // For simplicity, let's assume we want to show both Expense and Savings insights
    }

    private void addSavingsBreakdownBar(String label, int percentage) {
        View barView = LayoutInflater.from(requireContext()).inflate(R.layout.item_category_breakdown, llBreakdownContainer, false);
        TextView tvLabel = barView.findViewById(R.id.tv_category_label);
        TextView tvPercent = barView.findViewById(R.id.tv_category_percentage);
        ProgressBar pbBar = barView.findViewById(R.id.pb_category_bar);

        tvLabel.setText(label);
        tvPercent.setText(percentage + "%");
        pbBar.setProgress(Math.min(percentage, 100));
        pbBar.setProgressTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.money_left)));

        llBreakdownContainer.addView(barView);
    }

    private void updateBudgetUI(double budget, double spent, double balance) {
        // Since budget is derived from income, if income is zero, progress is zero
        if (budget <= 0) {
            pbBudgetUsage.setProgress(0);
            tvDailyLimit.setText("₹0");
            return;
        }
        
        int progress = (int) ((spent / budget) * 100);
        pbBudgetUsage.setProgress(Math.min(progress, 100));
        
        if (spent > budget) {
            pbBudgetUsage.setIndicatorColor(ContextCompat.getColor(requireContext(), R.color.overspending));
        } else if (progress >= 85) {
            pbBudgetUsage.setIndicatorColor(ContextCompat.getColor(requireContext(), R.color.warning_orange));
        } else {
            pbBudgetUsage.setIndicatorColor(ContextCompat.getColor(requireContext(), R.color.accent_amber));
        }

        // Calculate Daily Safe Expense Limit based on available loot (balance)
        Calendar cal = Calendar.getInstance();
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
        int daysLeft = daysInMonth - dayOfMonth + 1;
        double dailyLimit = Math.max(0, balance / daysLeft);
        tvDailyLimit.setText(String.format(Locale.getDefault(), "₹%.0f", dailyLimit));
    }

    private void calculateBreakdown(List<TransactionWithAccount> transactions) {
        clearBreakdown();
        Map<String, Double> categoryTotals = new HashMap<>();
        double totalExpense = 0;

        for (TransactionWithAccount t : transactions) {
            if ("Expense".equalsIgnoreCase(t.transaction.type)) {
                categoryTotals.put(t.transaction.category, categoryTotals.getOrDefault(t.transaction.category, 0.0) + t.transaction.amount);
                totalExpense += t.transaction.amount;
            }
        }

        if (totalExpense == 0) {
            tvNoDataChart.setVisibility(View.VISIBLE);
            return;
        }

        tvNoDataChart.setVisibility(View.GONE);
        List<Map.Entry<String, Double>> sortedList = new ArrayList<>(categoryTotals.entrySet());
        Collections.sort(sortedList, (e1, e2) -> e2.getValue().compareTo(e1.getValue()));

        int count = 0;
        for (Map.Entry<String, Double> entry : sortedList) {
            if (count >= 3) break;
            double percentage = (entry.getValue() / totalExpense) * 100;
            addBreakdownBar(entry.getKey(), (int) percentage);
            count++;
        }
    }

    private void clearBreakdown() {
        int childCount = llBreakdownContainer.getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            View v = llBreakdownContainer.getChildAt(i);
            if (v.getId() != R.id.tv_no_data_chart && !(v instanceof TextView && ((TextView)v).getText().toString().contains("Insights"))) {
                llBreakdownContainer.removeViewAt(i);
            }
        }
    }

    private void addBreakdownBar(String category, int percentage) {
        View barView = LayoutInflater.from(requireContext()).inflate(R.layout.item_category_breakdown, llBreakdownContainer, false);
        TextView tvLabel = barView.findViewById(R.id.tv_category_label);
        TextView tvPercent = barView.findViewById(R.id.tv_category_percentage);
        ProgressBar pbBar = barView.findViewById(R.id.pb_category_bar);

        tvLabel.setText(category);
        tvPercent.setText(percentage + "%");
        pbBar.setProgress(percentage);
        pbBar.setProgressTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.accent_amber)));

        llBreakdownContainer.addView(barView);
    }

    private void setupClickListeners() {
        btnSettings.setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_homeFragment_to_settingsFragment)
        );
        
        btnQuickExpense.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("initialType", "Expense");
            Navigation.findNavController(v).navigate(R.id.action_homeFragment_to_addExpenseFragment, bundle);
        });

        btnQuickIncome.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("initialType", "Income");
            Navigation.findNavController(v).navigate(R.id.action_homeFragment_to_addExpenseFragment, bundle);
        });

        if (btnQuickActive != null) {
            btnQuickActive.setOnClickListener(v -> {
                Bundle bundle = new Bundle();
                bundle.putString("initialType", "Saving");
                Navigation.findNavController(v).navigate(R.id.action_homeFragment_to_addExpenseFragment, bundle);
            });
        }

        tvViewAll.setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.historyFragment)
        );

        tvViewAllSavings.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putBoolean("scrollToSavings", true);
            Navigation.findNavController(v).navigate(R.id.action_homeFragment_to_accountFragment, bundle);
        });
    }

    private void showEditSavingDialog(SavingEntity saving) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_saving, null);
        EditText etGoalName = dialogView.findViewById(R.id.et_goal_name);
        EditText etTargetAmount = dialogView.findViewById(R.id.et_target_amount);
        EditText etCurrentAmount = dialogView.findViewById(R.id.et_current_amount);
        EditText etEmoji = dialogView.findViewById(R.id.et_emoji);

        etGoalName.setText(saving.goalName);
        etTargetAmount.setText(String.valueOf(saving.targetAmount));
        etCurrentAmount.setText(String.valueOf(saving.currentAmount));
        etEmoji.setText(saving.emoji);

        new AlertDialog.Builder(requireContext(), R.style.AppTheme)
                .setTitle("Edit Saving Goal")
                .setView(dialogView)
                .setPositiveButton("Update", (dialog, which) -> {
                    String name = etGoalName.getText().toString().trim();
                    String targetStr = etTargetAmount.getText().toString().trim();
                    String currentStr = etCurrentAmount.getText().toString().trim();
                    String emoji = etEmoji.getText().toString().trim();

                    if (!name.isEmpty() && !targetStr.isEmpty()) {
                        try {
                            saving.goalName = name;
                            saving.targetAmount = Double.parseDouble(targetStr);
                            saving.currentAmount = currentStr.isEmpty() ? 0 : Double.parseDouble(currentStr);
                            saving.emoji = emoji.isEmpty() ? "🎯" : emoji;
                            
                            viewModel.updateSaving(saving);
                        } catch (NumberFormatException e) {
                            Toast.makeText(getContext(), "Invalid amount", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Delete", (dialog, which) -> {
                    new AlertDialog.Builder(requireContext(), R.style.AppTheme)
                            .setTitle("Delete Goal?")
                            .setMessage("Are you sure you want to delete this saving goal?")
                            .setPositiveButton("Delete", (d, w) -> viewModel.deleteSaving(saving))
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .setNeutralButton("Cancel", null)
                .show();
    }

    private void showEditTransactionDialog(TransactionEntity transaction) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_transaction, null);
        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        EditText etAmount = dialogView.findViewById(R.id.et_dialog_amount);
        EditText etNote = dialogView.findViewById(R.id.et_dialog_note);
        MaterialButton btnDate = dialogView.findViewById(R.id.btn_dialog_date);
        MaterialButton btnTime = dialogView.findViewById(R.id.btn_dialog_time);

        tvTitle.setText("Edit " + transaction.type);
        etAmount.setText(String.valueOf(transaction.amount));
        etNote.setText(transaction.note);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(transaction.createdAt);

        SimpleDateFormat dateSdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        SimpleDateFormat timeSdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());

        btnDate.setText(dateSdf.format(calendar.getTime()));
        btnTime.setText(timeSdf.format(calendar.getTime()));

        btnDate.setOnClickListener(v -> {
            new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                btnDate.setText(dateSdf.format(calendar.getTime()));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        btnTime.setOnClickListener(v -> {
            new TimePickerDialog(requireContext(), (view, hourOfDay, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
                btnTime.setText(timeSdf.format(calendar.getTime()));
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show();
        });

        new AlertDialog.Builder(requireContext(), R.style.AppTheme)
                .setView(dialogView)
                .setPositiveButton("Update", (dialog, which) -> {
                    String amountStr = etAmount.getText().toString().trim();
                    if (!amountStr.isEmpty()) {
                        try {
                            transaction.amount = Double.parseDouble(amountStr);
                            transaction.note = etNote.getText().toString().trim();
                            transaction.createdAt = calendar.getTimeInMillis();
                            viewModel.updateTransaction(transaction);
                        } catch (NumberFormatException e) {
                            Toast.makeText(getContext(), "Invalid amount", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Delete", (dialog, which) -> {
                    new AlertDialog.Builder(requireContext(), R.style.AppTheme)
                            .setTitle("Delete Move?")
                            .setMessage("This action cannot be undone. Ready to wipe this record?")
                            .setPositiveButton("Wipe It", (d, w) -> viewModel.deleteTransaction(transaction))
                            .setNegativeButton("Keep It", null)
                            .show();
                })
                .setNeutralButton("Cancel", null)
                .show();
    }
}
