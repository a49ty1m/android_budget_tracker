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
        String[] options = {"Add Account", "Add Saving Goal"};
        new AlertDialog.Builder(requireContext(), R.style.AppTheme)
                .setTitle("What to add?")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showAddAccountDialog();
                    } else {
                        showAddSavingDialog();
                    }
                })
                .show();
    }

    private void showAddAccountDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_account, null);
        EditText etUserName = dialogView.findViewById(R.id.et_user_name);
        EditText etDatabaseName = dialogView.findViewById(R.id.et_database_name);

        new AlertDialog.Builder(requireContext(), R.style.AppTheme)
                .setTitle("New Account")
                .setView(dialogView)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = etUserName.getText().toString().trim();
                    String db = etDatabaseName.getText().toString().trim();
                    if (!name.isEmpty() && !db.isEmpty()) {
                        long currentTime = System.currentTimeMillis();
                        viewModel.insertAccount(new UserAccountEntity(name, db, currentTime, currentTime));
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAddSavingDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_saving, null);
        EditText etGoalName = dialogView.findViewById(R.id.et_goal_name);
        EditText etTargetAmount = dialogView.findViewById(R.id.et_target_amount);
        EditText etCurrentAmount = dialogView.findViewById(R.id.et_current_amount);
        EditText etEmoji = dialogView.findViewById(R.id.et_emoji);
        AutoCompleteTextView actvAccount = dialogView.findViewById(R.id.actv_saving_account);

        final List<AccountWithBalance>[] accountsWrapper = new List[1];
        final int[] selectedAccountId = {-1};

        viewModel.getAccountsWithBalance().observe(getViewLifecycleOwner(), accounts -> {
            if (accounts != null && !accounts.isEmpty()) {
                accountsWrapper[0] = accounts;
                List<String> accountNames = new ArrayList<>();
                for (AccountWithBalance acc : accounts) {
                    accountNames.add(String.format("(%s-%s)", acc.userName, acc.databaseName));
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_dropdown_item_1line, accountNames);
                actvAccount.setAdapter(adapter);
                
                // Default to first account
                actvAccount.setText(accountNames.get(0), false);
                selectedAccountId[0] = accounts.get(0).id;
            }
        });

        actvAccount.setOnItemClickListener((parent, view1, position, id) -> {
            if (accountsWrapper[0] != null) {
                selectedAccountId[0] = accountsWrapper[0].get(position).id;
            }
        });

        new AlertDialog.Builder(requireContext(), R.style.AppTheme)
                .setTitle("New Saving Goal")
                .setView(dialogView)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = etGoalName.getText().toString().trim();
                    String targetStr = etTargetAmount.getText().toString().trim();
                    String currentStr = etCurrentAmount.getText().toString().trim();
                    String emoji = etEmoji.getText().toString().trim();

                    if (!name.isEmpty() && !targetStr.isEmpty() && selectedAccountId[0] != -1) {
                        try {
                            double target = Double.parseDouble(targetStr);
                            double current = currentStr.isEmpty() ? 0 : Double.parseDouble(currentStr);
                            if (emoji.isEmpty()) emoji = "🎯";
                            
                            viewModel.insertSaving(new SavingEntity(selectedAccountId[0], name, target, current, emoji, System.currentTimeMillis()));
                        } catch (NumberFormatException e) {
                            Toast.makeText(getContext(), "Invalid amount", Toast.LENGTH_SHORT).show();
                        }
                    } else if (selectedAccountId[0] == -1) {
                        Toast.makeText(getContext(), "Please select an account", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditAccountDialog(AccountWithBalance account) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_account, null);
        EditText etUserName = dialogView.findViewById(R.id.et_account_name);
        EditText etDatabaseName = dialogView.findViewById(R.id.et_database_name);

        etUserName.setText(account.userName);
        etDatabaseName.setText(account.databaseName);

        new AlertDialog.Builder(requireContext(), R.style.AppTheme)
                .setTitle("Edit Account")
                .setView(dialogView)
                .setPositiveButton("Update", (dialog, which) -> {
                    String userName = etUserName.getText().toString().trim();
                    String dbName = etDatabaseName.getText().toString().trim();

                    if (!userName.isEmpty() && !dbName.isEmpty()) {
                        UserAccountEntity entity = new UserAccountEntity(
                                userName,
                                dbName,
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
                                UserAccountEntity entity = new UserAccountEntity(account.userName, account.databaseName, System.currentTimeMillis(), System.currentTimeMillis());
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
