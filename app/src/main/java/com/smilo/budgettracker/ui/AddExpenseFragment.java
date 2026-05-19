package com.smilo.budgettracker.ui;

import android.content.res.ColorStateList;
import android.view.ContextThemeWrapper;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import androidx.navigation.Navigation;
import com.smilo.budgettracker.R;
import com.smilo.budgettracker.db.AccountWithBalance;
import com.smilo.budgettracker.db.CategoryEntity;
import com.smilo.budgettracker.db.SavingEntity;
import com.smilo.budgettracker.db.SavingWithAccount;
import com.smilo.budgettracker.db.TransactionEntity;
import com.smilo.budgettracker.db.UserAccountEntity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Color;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.List;

public class AddExpenseFragment extends Fragment {

    private BudgetViewModel viewModel;
    private EditText etAmount, etNote;
    private TextView tvTitle, tvCurrencySymbol, tvCatLabel;
    private ChipGroup cgCategories;
    private MaterialButtonToggleGroup toggleType;
    private MaterialButton btnSave;
    private ImageButton btnEditCategories;
    private MaterialButton btnDate, btnTime;
    private Calendar selectedDateTime = Calendar.getInstance();
    private List<SavingWithAccount> currentSavings;
    private ChipGroup cg_accounts;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_expense, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(BudgetViewModel.class);

        tvTitle = view.findViewById(R.id.tv_add_title);
        tvCurrencySymbol = view.findViewById(R.id.tv_currency);
        tvCatLabel = view.findViewById(R.id.tv_cat_label);
        etAmount = view.findViewById(R.id.et_amount);
        etNote = view.findViewById(R.id.et_note);
        cgCategories = view.findViewById(R.id.cg_categories);
        cg_accounts = view.findViewById(R.id.cg_accounts);
        toggleType = view.findViewById(R.id.toggle_type);
        btnSave = view.findViewById(R.id.btn_save);
        btnEditCategories = view.findViewById(R.id.btn_edit_categories);
        btnDate = view.findViewById(R.id.btn_date);
        btnTime = view.findViewById(R.id.btn_time);

        // setupTypeToggle(); // Delaying this
        setupDateTimePickers();

        // Force "Spend" (Expense) to be the default state every time the screen opens
        toggleType.check(R.id.btn_type_expense);
        switchToExpenseUI();

        setupTypeToggle(); // Enable listener now

        viewModel.getAllSavings().observe(getViewLifecycleOwner(), savings -> {
            this.currentSavings = savings;
            if (toggleType.getCheckedButtonId() == R.id.btn_type_saving) {
                setupSavingsChips();
            }
        });

        btnSave.setOnClickListener(v -> saveTransaction());
        if (btnEditCategories != null) {
            btnEditCategories.setOnClickListener(v -> showManageCategoriesDialog());
        }
        viewModel.getAllUserAccounts().observe(getViewLifecycleOwner(), this::setupAccountChips);
        
        view.post(this::updateCheckedButtonStyle);
    }

    private void setupTypeToggle() {
        if (toggleType == null) return;
        toggleType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                // Ensure only one button is styled as checked at a time
                updateCheckedButtonStyle();
                if (checkedId == R.id.btn_type_income) {
                    switchToIncomeUI();
                } else if (checkedId == R.id.btn_type_saving) {
                    switchToSavingUI();
                } else {
                    switchToExpenseUI();
                }
            }
        });
    }

    private void updateCheckedButtonStyle() {
        if (!isAdded() || getContext() == null || toggleType == null) return;
        
        int checkedId = toggleType.getCheckedButtonId();
        int radius = (int) (26 * getResources().getDisplayMetrics().density);
        
        for (int i = 0; i < toggleType.getChildCount(); i++) {
            View child = toggleType.getChildAt(i);
            if (!(child instanceof MaterialButton)) continue;
            MaterialButton btn = (MaterialButton) child;
            
            // Re-apply absolute state every time
            btn.setRippleColor(null);
            btn.setElevation(0f);
            btn.setStrokeWidth(0);
            btn.setInsetTop(0);
            btn.setInsetBottom(0);
            btn.setPadding(0, 0, 0, 0);
            
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.RECTANGLE);
            shape.setCornerRadius(radius);
            
            if (btn.getId() == checkedId) {
                int colorRes = R.color.overspending;
                if (btn.getId() == R.id.btn_type_income) colorRes = R.color.money_left;
                else if (btn.getId() == R.id.btn_type_saving) colorRes = R.color.accent_blue;
                
                shape.setColor(ContextCompat.getColor(requireContext(), colorRes));
                btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.bg_dark));
                btn.setAlpha(1.0f);
            } else {
                shape.setColor(Color.TRANSPARENT);
                btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
                btn.setAlpha(1.0f);
            }
            
            btn.setBackground(shape);
        }
    }

    private void setupDateTimePickers() {
        updateDateLabel();
        updateTimeLabel();

        btnDate.setOnClickListener(v -> {
            new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
                selectedDateTime.set(Calendar.YEAR, year);
                selectedDateTime.set(Calendar.MONTH, month);
                selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateDateLabel();
            }, selectedDateTime.get(Calendar.YEAR), selectedDateTime.get(Calendar.MONTH), 
            selectedDateTime.get(Calendar.DAY_OF_MONTH)).show();
        });

        btnTime.setOnClickListener(v -> {
            new TimePickerDialog(requireContext(), (view, hourOfDay, minute) -> {
                selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                selectedDateTime.set(Calendar.MINUTE, minute);
                updateTimeLabel();
            }, selectedDateTime.get(Calendar.HOUR_OF_DAY), selectedDateTime.get(Calendar.MINUTE), false).show();
        });
    }

    private void updateDateLabel() {
        if (!isAdded() || btnDate == null) return;
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM", Locale.getDefault());
        Calendar now = Calendar.getInstance();
        boolean isToday = selectedDateTime.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) 
                && selectedDateTime.get(Calendar.YEAR) == now.get(Calendar.YEAR);
        
        String dateStr = sdf.format(selectedDateTime.getTime());
        btnDate.setText(isToday ? "Today, " + dateStr : dateStr);
    }

    private void updateTimeLabel() {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        btnTime.setText(sdf.format(selectedDateTime.getTime()));
    }

    private void switchToIncomeUI() {
        if (getContext() == null) return;
        tvTitle.setText("Received how much?");
        if (tvCatLabel != null) tvCatLabel.setText("Category");
        int green = ContextCompat.getColor(requireContext(), R.color.money_left);
        tvCurrencySymbol.setTextColor(green);
        btnSave.setBackgroundTintList(ColorStateList.valueOf(green));
        observeCategories("Income");
        btnEditCategories.setVisibility(View.VISIBLE);
    }

    private void switchToExpenseUI() {
        if (getContext() == null) return;
        tvTitle.setText("Spent how much?");
        if (tvCatLabel != null) tvCatLabel.setText("Category");
        int red = ContextCompat.getColor(requireContext(), R.color.overspending);
        tvCurrencySymbol.setTextColor(red);
        btnSave.setBackgroundTintList(ColorStateList.valueOf(red));
        observeCategories("Expense");
        btnEditCategories.setVisibility(View.VISIBLE);
    }

    private void switchToSavingUI() {
        if (getContext() == null) return;
        tvTitle.setText("Saving how much?");
        if (tvCatLabel != null) tvCatLabel.setText("Goal");
        int blue = ContextCompat.getColor(requireContext(), R.color.accent_blue);
        tvCurrencySymbol.setTextColor(blue);
        btnSave.setBackgroundTintList(ColorStateList.valueOf(blue));
        btnEditCategories.setVisibility(View.GONE);
        setupSavingsChips();
    }

    private void observeCategories(String type) {
        viewModel.getCategoriesByType(type).observe(getViewLifecycleOwner(), categories -> {
            if (categories != null && toggleType.getCheckedButtonId() != R.id.btn_type_saving) {
                setupCategories(categories);
            }
        });
    }

    private void setupCategories(List<CategoryEntity> categories) {
        cgCategories.removeAllViews();
        for (CategoryEntity category : categories) {
            Chip chip = new Chip(new ContextThemeWrapper(requireContext(), R.style.Widget_App_Chip));
            chip.setText(category.emoji + " " + category.name);
            chip.setCheckable(true);
            chip.setClickable(true);
            chip.setOnLongClickListener(v -> {
                showEditCategoryDialog(category, null);
                return true;
            });
            cgCategories.addView(chip);
        }

        Chip customChip = new Chip(requireContext());
        customChip.setText("+ Custom");
        customChip.setChipBackgroundColorResource(R.color.bg_surface);
        customChip.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_amber));
        customChip.setOnClickListener(v -> showAddCategoryDialog());
        cgCategories.addView(customChip);
    }

    private void setupSavingsChips() {
        cgCategories.removeAllViews();
        if (currentSavings != null) {
            for (SavingWithAccount item : currentSavings) {
                SavingEntity saving = item.saving;
                Chip chip = new Chip(new ContextThemeWrapper(requireContext(), R.style.Widget_App_Chip));
                String text = saving.emoji + " " + saving.goalName;
                chip.setText(text);
                chip.setTag(saving);
                chip.setCheckable(true);
                chip.setClickable(true);
                chip.setOnLongClickListener(v -> {
                    showEditSavingDialog(saving);
                    return true;
                });
                cgCategories.addView(chip);
            }
        }

        Chip addGoalChip = new Chip(requireContext());
        addGoalChip.setText("+ New Goal");
        addGoalChip.setChipBackgroundColorResource(R.color.bg_surface);
        addGoalChip.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_amber));
        addGoalChip.setOnClickListener(v -> showAddSavingDialog());
        cgCategories.addView(addGoalChip);
    }

    private void setupAccountChips(List<UserAccountEntity> accounts) {
        if (cg_accounts == null) return;
        cg_accounts.removeAllViews();
        
        if (accounts != null) {
            for (UserAccountEntity account : accounts) {
                Chip chip = new Chip(new ContextThemeWrapper(requireContext(), R.style.Widget_App_Chip));
                chip.setText(account.emoji + " " + account.userName + "-" + account.databaseName);
                chip.setTag(account.id);
                chip.setCheckable(true);
                chip.setClickable(true);
                chip.setOnLongClickListener(v -> {
                    showEditAccountDialog(account);
                    return true;
                });
                cg_accounts.addView(chip);
            }
        }

        Chip addChip = new Chip(requireContext());
        addChip.setText("+ New Wallet");
        addChip.setChipBackgroundColorResource(R.color.bg_surface);
        addChip.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_amber));
        addChip.setOnClickListener(v -> showAddAccountDialog());
        cg_accounts.addView(addChip);

        if (cg_accounts.getChildCount() > 1 && cg_accounts.getCheckedChipId() == View.NO_ID) {
            Chip firstChip = (Chip) cg_accounts.getChildAt(0);
            firstChip.setChecked(true);
        }
    }

    private void showAddSavingDialog() {
        Navigation.findNavController(requireView()).navigate(R.id.action_addExpenseFragment_to_addSavingGoalFragment);
    }

    private void showEditSavingDialog(SavingEntity saving) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_saving, null);
        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_saving_title);
        EditText etGoalName = dialogView.findViewById(R.id.et_goal_name);
        EditText etTargetAmount = dialogView.findViewById(R.id.et_target_amount);
        EditText etCurrentAmount = dialogView.findViewById(R.id.et_current_amount);
        EditText etEmoji = dialogView.findViewById(R.id.et_emoji);
        AutoCompleteTextView actvAccount = dialogView.findViewById(R.id.actv_saving_account);
        com.google.android.material.textfield.TextInputLayout tilAccount = dialogView.findViewById(R.id.til_saving_account);

        tvTitle.setText("Edit Saving Goal");
        etGoalName.setText(saving.goalName);
        etTargetAmount.setText(String.valueOf(saving.targetAmount));
        etCurrentAmount.setText(String.valueOf(saving.currentAmount));
        etEmoji.setText(saving.emoji);
        tilAccount.setEnabled(false);

        viewModel.getAccountsWithBalance().observe(getViewLifecycleOwner(), accounts -> {
            if (accounts != null) {
                for (AccountWithBalance acc : accounts) {
                    if (acc.id == saving.userId) {
                        actvAccount.setText(acc.emoji + " " + acc.databaseName, false);
                        break;
                    }
                }
            }
        });

        new AlertDialog.Builder(requireContext(), R.style.Theme_BudgetTracker)
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
                .setNegativeButton("Delete", (dialog, which) -> viewModel.deleteSaving(saving))
                .setNeutralButton("Cancel", null)
                .show();
    }

    private void showAddAccountDialog() {
        Navigation.findNavController(requireView()).navigate(R.id.action_addExpenseFragment_to_addAccountFragment);
    }

    private void showEditAccountDialog(UserAccountEntity account) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_account, null);
        EditText etUserName = dialogView.findViewById(R.id.et_account_name);
        EditText etDatabaseName = dialogView.findViewById(R.id.et_database_name);
        EditText etEmoji = dialogView.findViewById(R.id.et_account_emoji);

        etUserName.setText(account.userName);
        etDatabaseName.setText(account.databaseName);
        etEmoji.setText(account.emoji);

        new AlertDialog.Builder(requireContext(), R.style.Theme_BudgetTracker)
                .setTitle("Edit Account")
                .setView(dialogView)
                .setPositiveButton("Update", (dialog, which) -> {
                    String userName = etUserName.getText().toString().trim();
                    String dbName = etDatabaseName.getText().toString().trim();
                    String emoji = etEmoji.getText().toString().trim();
                    if (!userName.isEmpty() && !dbName.isEmpty()) {
                        account.userName = userName;
                        account.databaseName = dbName;
                        account.emoji = emoji.isEmpty() ? "💵" : emoji;
                        account.updatedAt = System.currentTimeMillis();
                        viewModel.updateAccount(account);
                    }
                })
                .setNegativeButton("Delete", (dialog, which) -> viewModel.deleteAccount(account))
                .setNeutralButton("Cancel", null)
                .show();
    }

    private void showAddCategoryDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_category, null);
        EditText etName = dialogView.findViewById(R.id.et_category_name);
        EditText etEmoji = dialogView.findViewById(R.id.et_category_emoji);
        MaterialButtonToggleGroup toggleTypeGroup = dialogView.findViewById(R.id.toggle_category_type);
        if (toggleType.getCheckedButtonId() == R.id.btn_type_income) toggleTypeGroup.check(R.id.btn_category_income);
        else toggleTypeGroup.check(R.id.btn_category_expense);

        new AlertDialog.Builder(requireContext(), R.style.Theme_BudgetTracker)
                .setTitle("Add Custom Category")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String emoji = etEmoji.getText().toString().trim();
                    if (!name.isEmpty()) {
                        String type = (toggleTypeGroup.getCheckedButtonId() == R.id.btn_category_income) ? "Income" : "Expense";
                        if (emoji.isEmpty()) emoji = "✨";
                        viewModel.insertCategory(new CategoryEntity(name, type, emoji));
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditCategoryDialog(@Nullable CategoryEntity category, @Nullable com.google.android.material.tabs.TabLayout tabLayout) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_category, null);
        EditText etName = dialogView.findViewById(R.id.et_category_name);
        EditText etEmoji = dialogView.findViewById(R.id.et_category_emoji);
        MaterialButtonToggleGroup toggleTypeGroup = dialogView.findViewById(R.id.toggle_category_type);

        if (category != null) {
            etName.setText(category.name);
            etEmoji.setText(category.emoji);
            toggleTypeGroup.check(category.type.equals("Income") ? R.id.btn_category_income : R.id.btn_category_expense);
        }

        new AlertDialog.Builder(requireContext(), R.style.Theme_BudgetTracker)
                .setTitle(category == null ? "Add Category" : "Edit Category")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String emoji = etEmoji.getText().toString().trim();
                    String type = (toggleTypeGroup.getCheckedButtonId() == R.id.btn_category_income) ? "Income" : "Expense";
                    if (!name.isEmpty()) {
                        if (emoji.isEmpty()) emoji = "✨";
                        if (category == null) viewModel.insertCategory(new CategoryEntity(name, type, emoji));
                        else {
                            category.name = name;
                            category.emoji = emoji;
                            category.type = type;
                            viewModel.updateCategory(category);
                        }
                    }
                })
                .setNegativeButton("Delete", (dialog, which) -> { if (category != null) viewModel.deleteCategory(category); })
                .setNeutralButton("Cancel", null)
                .show();
    }

    private void showManageCategoriesDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.fragment_categories, null);
        RecyclerView rv = dialogView.findViewById(R.id.rv_categories);
        View fab = dialogView.findViewById(R.id.btn_add_category_top);
        com.google.android.material.tabs.TabLayout tabLayout = dialogView.findViewById(R.id.tab_layout_categories);
        if (dialogView.findViewById(R.id.tv_categories_title) != null) dialogView.findViewById(R.id.tv_categories_title).setVisibility(View.GONE);

        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        CategoryAdapter adapter = new CategoryAdapter();
        rv.setAdapter(adapter);

        int typeId = toggleType.getCheckedButtonId();
        String currentType = (typeId == R.id.btn_type_income) ? "Income" : "Expense";
        if (tabLayout != null) {
            tabLayout.getTabAt(currentType.equals("Income") ? 1 : 0).select();
            tabLayout.addOnTabSelectedListener(new com.google.android.material.tabs.TabLayout.OnTabSelectedListener() {
                @Override public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab tab) { updateFilteredCategories(adapter, tab.getPosition()); }
                @Override public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
                @Override public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
            });
        }
        updateFilteredCategories(adapter, currentType.equals("Income") ? 1 : 0);

        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.Theme_BudgetTracker).setView(dialogView).create();
        if (dialogView.findViewById(R.id.toolbar_categories) != null) ((Toolbar)dialogView.findViewById(R.id.toolbar_categories)).setNavigationOnClickListener(v -> dialog.dismiss());
        adapter.setOnCategoryClickListener(c -> showEditCategoryDialog(c, tabLayout));
        if (fab != null) fab.setOnClickListener(v -> showAddCategoryDialog());
        dialog.show();
    }

    private void updateFilteredCategories(CategoryAdapter adapter, int tabPosition) {
        String type = (tabPosition == 0) ? "Expense" : "Income";
        viewModel.getCategoriesByType(type).observe(getViewLifecycleOwner(), adapter::setCategories);
    }

    private void saveTransaction() {
        String amountStr = etAmount.getText().toString().trim();
        if (amountStr.isEmpty() || Double.parseDouble(amountStr) <= 0) {
            Toast.makeText(getContext(), "Please enter a valid amount", Toast.LENGTH_SHORT).show();
            return;
        }
        double amount = Double.parseDouble(amountStr);
        int selectedChipId = cgCategories.getCheckedChipId();
        int accountChipId = cg_accounts.getCheckedChipId();
        if (selectedChipId == View.NO_ID || accountChipId == View.NO_ID) {
            Toast.makeText(getContext(), "Select account and category/goal", Toast.LENGTH_SHORT).show();
            return;
        }

        Chip accChip = cg_accounts.findViewById(accountChipId);
        int selectedAccountId = (Integer) accChip.getTag();
        Chip selectedChip = cgCategories.findViewById(selectedChipId);
        int typeId = toggleType.getCheckedButtonId();

        if (typeId == R.id.btn_type_saving) {
            SavingEntity saving = (SavingEntity) selectedChip.getTag();
            if (saving != null) {
                saving.currentAmount += amount;
                viewModel.updateSaving(saving);
                // Record as Saving type so it shows clearly in history
                TransactionEntity t = new TransactionEntity(selectedAccountId, "Saving", amount, "Saving", "🏦 " + saving.goalName, etNote.getText().toString(), selectedDateTime.getTimeInMillis());
                viewModel.insertTransaction(t);
                Navigation.findNavController(requireView()).popBackStack();
            }
        } else {
            String category = selectedChip.getText().toString();
            String type = (typeId == R.id.btn_type_income) ? "Income" : "Expense";
            TransactionEntity transaction = new TransactionEntity(selectedAccountId, type, amount, "Default", category, etNote.getText().toString(), selectedDateTime.getTimeInMillis());
            viewModel.insertTransaction(transaction);
            Navigation.findNavController(requireView()).popBackStack();
        }
    }
}
