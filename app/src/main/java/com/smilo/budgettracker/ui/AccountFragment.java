package com.smilo.budgettracker.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;
import androidx.navigation.Navigation;
import com.smilo.budgettracker.R;
import com.smilo.budgettracker.db.AccountWithBalance;
import com.smilo.budgettracker.db.SavingEntity;
import com.smilo.budgettracker.db.SavingWithAccount;
import com.smilo.budgettracker.db.UserAccountEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AccountFragment extends Fragment {

    private BudgetViewModel viewModel;
    private AccountAdapter accountAdapter;
    private SavingAdapter savingAdapter;
    private TextView tvTotalBalanceSummary, tvTotalSavingsSummary, tvSavingsHeader;
    private androidx.core.widget.NestedScrollView nestedScrollView;
    private ExtendedFloatingActionButton fabAddAccount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_accounts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvTotalBalanceSummary = view.findViewById(R.id.tv_total_balance_summary);
        tvTotalSavingsSummary = view.findViewById(R.id.tv_total_savings_summary);
        tvSavingsHeader = view.findViewById(R.id.tv_savings_header);
        nestedScrollView = view.findViewById(R.id.nested_scroll_view_accounts);
        fabAddAccount = view.findViewById(R.id.fab_add_account);
        RecyclerView rvAccounts = view.findViewById(R.id.rv_accounts);
        RecyclerView rvSavings = view.findViewById(R.id.rv_savings);

        rvAccounts.setLayoutManager(new LinearLayoutManager(requireContext()));
        accountAdapter = new AccountAdapter();
        rvAccounts.setAdapter(accountAdapter);
        accountAdapter.setOnAccountClickListener(account -> showEditAccountDialog(account));

        rvSavings.setLayoutManager(new LinearLayoutManager(requireContext()));
        savingAdapter = new SavingAdapter();
        rvSavings.setAdapter(savingAdapter);


        viewModel = new ViewModelProvider(requireActivity()).get(BudgetViewModel.class);

        viewModel.getAccountsWithBalance().observe(getViewLifecycleOwner(), accounts -> {
            accountAdapter.setAccounts(accounts);
        });

        viewModel.getAllSavings().observe(getViewLifecycleOwner(), savings -> {
            savingAdapter.setSavings(savings);
            double totalSavings = 0;
            if (savings != null) {
                for (SavingWithAccount s : savings) {
                    totalSavings += s.saving.currentAmount;
                }
            }
            tvTotalSavingsSummary.setText(String.format(Locale.getDefault(), "₹%.2f", totalSavings));
        });

        viewModel.getTotalBalance().observe(getViewLifecycleOwner(), balance -> {
            tvTotalBalanceSummary.setText(String.format(Locale.getDefault(), "₹%.2f", balance));
        });

        savingAdapter.setOnSavingClickListener(this::showEditSavingDialog);

        fabAddAccount.setOnClickListener(v -> showAddOptionsDialog());

        View btnAddSavingInline = view.findViewById(R.id.btn_add_saving_inline);
        if (btnAddSavingInline != null) {
            btnAddSavingInline.setOnClickListener(v -> showAddSavingDialog());
        }

        View btnAddAccountInline = view.findViewById(R.id.btn_add_account_inline);
        if (btnAddAccountInline != null) {
            btnAddAccountInline.setOnClickListener(v -> showAddAccountDialog());
        }

        if (getArguments() != null) {
            if (getArguments().getBoolean("scrollToSavings", false)) {
                view.post(() -> {
                    if (nestedScrollView != null && tvSavingsHeader != null) {
                        nestedScrollView.smoothScrollTo(0, tvSavingsHeader.getTop());
                    }
                });
            }
            if (getArguments().getBoolean("openAddSaving", false)) {
                showAddSavingDialog();
            }
        }
    }

    private void showAddOptionsDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_what_to_add, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.AppTheme)
                .setView(dialogView)
                .create();

        dialogView.findViewById(R.id.btn_add_account_option).setOnClickListener(v -> {
            dialog.dismiss();
            showAddAccountDialog();
        });

        dialogView.findViewById(R.id.btn_add_saving_option).setOnClickListener(v -> {
            dialog.dismiss();
            showAddSavingDialog();
        });

        dialog.show();
    }

    private void showAddAccountDialog() {
        Navigation.findNavController(requireView()).navigate(R.id.action_accountFragment_to_addAccountFragment);
    }

    private void showAddSavingDialog() {
        Navigation.findNavController(requireView()).navigate(R.id.action_accountFragment_to_addSavingGoalFragment);
    }

    private void showEditAccountDialog(AccountWithBalance account) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_account, null);
        EditText etUserName = dialogView.findViewById(R.id.et_account_name);
        EditText etDatabaseName = dialogView.findViewById(R.id.et_database_name);
        EditText etEmoji = dialogView.findViewById(R.id.et_account_emoji);

        etUserName.setText(account.userName);
        etDatabaseName.setText(account.databaseName);
        etEmoji.setText(account.emoji);

        new AlertDialog.Builder(requireContext(), R.style.AppTheme)
                .setTitle("Edit Account")
                .setView(dialogView)
                .setPositiveButton("Update", (dialog, which) -> {
                    String userName = etUserName.getText().toString().trim();
                    String dbName = etDatabaseName.getText().toString().trim();
                    String emoji = etEmoji.getText().toString().trim();

                    if (!userName.isEmpty() && !dbName.isEmpty()) {
                        if (emoji.isEmpty()) emoji = "💵";
                        UserAccountEntity entity = new UserAccountEntity(
                                userName,
                                dbName,
                                emoji,
                                System.currentTimeMillis(), // createdAt
                                System.currentTimeMillis() // updatedAt
                        );
                        entity.id = account.id;
                        viewModel.updateAccount(entity);
                    }
                })
                .setNegativeButton("Delete", (dialog, which) -> {
                    new AlertDialog.Builder(requireContext(), R.style.AppTheme)
                            .setTitle("Delete Account?")
                            .setMessage("Delete this account and all associated transactions?")
                            .setPositiveButton("Delete", (d, w) -> {
                                UserAccountEntity entity = new UserAccountEntity(account.userName, account.databaseName, account.emoji, System.currentTimeMillis(), System.currentTimeMillis());
                                entity.id = account.id;
                                viewModel.deleteAccount(entity);
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .setNeutralButton("Cancel", null)
                .show();
    }

    private void showEditSavingDialog(SavingEntity saving) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_saving, null);
        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_saving_title);
        EditText etGoalName = dialogView.findViewById(R.id.et_goal_name);
        EditText etTargetAmount = dialogView.findViewById(R.id.et_target_amount);
        EditText etCurrentAmount = dialogView.findViewById(R.id.et_current_amount);
        EditText etEmoji = dialogView.findViewById(R.id.et_emoji);
        AutoCompleteTextView actvAccount = dialogView.findViewById(R.id.actv_saving_account);
        TextInputLayout tilAccount = dialogView.findViewById(R.id.til_saving_account);

        tvTitle.setText("Edit Saving Goal");
        etGoalName.setText(saving.goalName);
        etTargetAmount.setText(String.valueOf(saving.targetAmount));
        etCurrentAmount.setText(String.valueOf(saving.currentAmount));
        etEmoji.setText(saving.emoji);
        
        // Disable account selection for existing savings to maintain data integrity
        tilAccount.setEnabled(false);
        viewModel.getAccountsWithBalance().observe(getViewLifecycleOwner(), accounts -> {
            if (accounts != null) {
                for (AccountWithBalance acc : accounts) {
                    if (acc.id == saving.userId) {
                        actvAccount.setText(String.format("(%s-%s)", acc.userName, acc.databaseName), false);
                        break;
                    }
                }
            }
        });

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
}
