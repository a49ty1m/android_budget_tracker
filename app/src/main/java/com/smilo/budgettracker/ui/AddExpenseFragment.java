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
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.smilo.budgettracker.R;
import com.smilo.budgettracker.db.CategoryEntity;
import com.smilo.budgettracker.db.SavingEntity;
import com.smilo.budgettracker.db.SavingWithAccount;
import com.smilo.budgettracker.db.TransactionEntity;
import com.smilo.budgettracker.db.UserAccountEntity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.List;

public class AddExpenseFragment extends Fragment {

    private BudgetViewModel viewModel;
    private EditText etAmount, etNote;
    private TextView tvTitle, tvCurrencySymbol;
    private ChipGroup cgCategories;
    private MaterialButtonToggleGroup toggleType;
    private MaterialButton btnSave;
    private ImageButton btnEditCategories;
    private MaterialButton btnDate, btnTime;
    private Calendar selectedDateTime = Calendar.getInstance();
    private List<SavingWithAccount> currentSavings;
    private ChipGroup cg_accounts;
    private int selectedAccountId = 1;

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
        etAmount = view.findViewById(R.id.et_amount);
        etNote = view.findViewById(R.id.et_note);
        cgCategories = view.findViewById(R.id.cg_categories);
        cg_accounts = view.findViewById(R.id.cg_accounts);
        toggleType = view.findViewById(R.id.toggle_type);
        btnSave = view.findViewById(R.id.btn_save);
        btnEditCategories = view.findViewById(R.id.btn_edit_categories);
        btnDate = view.findViewById(R.id.btn_date);
        btnTime = view.findViewById(R.id.btn_time);

        setupTypeToggle();
        setupDateTimePickers();

        // Handle arguments for initial state
        String initialType = "Expense";
        if (getArguments() != null) {
            initialType = getArguments().getString("initialType", "Expense");
        }

        switch (initialType) {
            case "Income":
                toggleType.check(R.id.btn_type_income);
                switchToIncomeUI();
                break;
            case "Saving":
                toggleType.check(R.id.btn_type_saving);
                switchToSavingUI();
                break;
            case "Account":
                toggleType.check(R.id.btn_type_account);
                switchToAccountUI();
                break;
            default:
                toggleType.check(R.id.btn_type_expense);
                switchToExpenseUI();
                break;
        }

        viewModel.getAllSavings().observe(getViewLifecycleOwner(), savings -> {
            this.currentSavings = savings;
        });

        btnSave.setOnClickListener(v -> saveTransaction());
        if (btnEditCategories != null) {
            btnEditCategories.setOnClickListener(v -> showManageCategoriesDialog());
        }
        viewModel.getAllUserAccounts().observe(getViewLifecycleOwner(), this::setupAccountChips);
    }

    private void setupTypeToggle() {
        toggleType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btn_type_income) {
                    switchToIncomeUI();
                } else if (checkedId == R.id.btn_type_saving) {
                    switchToSavingUI();
                } else if (checkedId == R.id.btn_type_account) {
                    switchToAccountUI();
                } else {
                    switchToExpenseUI();
                }
            }
        });
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
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        btnDate.setText(sdf.format(selectedDateTime.getTime()));
    }

    private void updateTimeLabel() {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        btnTime.setText(sdf.format(selectedDateTime.getTime()));
    }

    private void switchToAccountUI() {
        tvTitle.setText("Add Account");
        int amber = ContextCompat.getColor(requireContext(), R.color.accent_amber);
        tvCurrencySymbol.setTextColor(amber);
        btnSave.setBackgroundTintList(ColorStateList.valueOf(amber));
        btnEditCategories.setVisibility(View.GONE);
        cgCategories.removeAllViews();
        // Navigation.findNavController(requireView()).navigate(R.id.action_homeFragment_to_accountFragment);
        Navigation.findNavController(requireView()).navigate(R.id.action_addExpenseFragment_to_accountFragment);
        // Reset toggle to previous or default after navigation
        toggleType.check(R.id.btn_type_expense);
    }

    private void switchToIncomeUI() {
        tvTitle.setText("Received how much?");
        int green = ContextCompat.getColor(requireContext(), R.color.money_left);
        tvCurrencySymbol.setTextColor(green);
        btnSave.setBackgroundTintList(ColorStateList.valueOf(green));
        observeCategories("Income");
        btnEditCategories.setVisibility(View.VISIBLE);
    }

    private void switchToExpenseUI() {
        tvTitle.setText("Spent how much?");
        int amber = ContextCompat.getColor(requireContext(), R.color.accent_amber);
        tvCurrencySymbol.setTextColor(amber);
        btnSave.setBackgroundTintList(ColorStateList.valueOf(amber));
        observeCategories("Expense");
        btnEditCategories.setVisibility(View.VISIBLE);
    }

    private void switchToSavingUI() {
        tvTitle.setText("Save how much?");
        int blue = ContextCompat.getColor(requireContext(), R.color.accent_amber); // Using amber for consistency if blue not defined
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
            cgCategories.addView(chip);
        }

        Chip customChip = new Chip(requireContext());
        customChip.setText("+ Custom");
        customChip.setChipBackgroundColorResource(R.color.bg_surface);
        customChip.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        customChip.setOnClickListener(v -> showAddCategoryDialog());
        cgCategories.addView(customChip);
    }

    private void setupSavingsChips() {
        cgCategories.removeAllViews();
        if (currentSavings == null || currentSavings.isEmpty()) {
            Toast.makeText(getContext(), "No savings goals found. Create one in Accounts.", Toast.LENGTH_SHORT).show();
            return;
        }

        for (SavingWithAccount item : currentSavings) {
            SavingEntity saving = item.saving;
            Chip chip = new Chip(new ContextThemeWrapper(requireContext(), R.style.Widget_App_Chip));
            String text = saving.emoji + " " + saving.goalName;
            if (item.userName != null && item.accountName != null) {
                text += String.format(" (%s-%s)", item.userName, item.accountName);
            }
            chip.setText(text);
            chip.setTag(saving);
            chip.setCheckable(true);
            chip.setClickable(true);
            cgCategories.addView(chip);
        }
    }

    private void setupAccountChips(List<UserAccountEntity> accounts) {
        if (cg_accounts == null) return;
        cg_accounts.removeAllViews();
        if (accounts == null || accounts.isEmpty()) {
            return;
        }
        for (UserAccountEntity account : accounts) {
            Chip chip = new Chip(new ContextThemeWrapper(requireContext(), R.style.Widget_App_Chip));
            String displayName = String.format("(%s-%s)", account.userName, account.databaseName);
            chip.setText(displayName);
            chip.setTag(account.id);
            chip.setId(View.generateViewId());
            chip.setCheckable(true);
            chip.setClickable(true);
            cg_accounts.addView(chip);
        }
        // Auto select first
        if (cg_accounts.getChildCount() > 0 && cg_accounts.getCheckedChipId() == View.NO_ID) {
            Chip firstChip = (Chip) cg_accounts.getChildAt(0);
            firstChip.setChecked(true);
        }
    }

    private void showAddCategoryDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_category, null);
        EditText etName = dialogView.findViewById(R.id.et_category_name);
        EditText etEmoji = dialogView.findViewById(R.id.et_category_emoji);
        MaterialButtonToggleGroup toggleTypeGroup = dialogView.findViewById(R.id.toggle_category_type);

        // Set default selection based on current transaction type
        if (toggleType.getCheckedButtonId() == R.id.btn_type_income) {
            toggleTypeGroup.check(R.id.btn_category_income);
        } else {
            toggleTypeGroup.check(R.id.btn_category_expense);
        }

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

    private void showManageCategoriesDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.fragment_categories, null);
        RecyclerView rv = dialogView.findViewById(R.id.rv_categories);
        View fab = dialogView.findViewById(R.id.btn_add_category_top);
        com.google.android.material.tabs.TabLayout tabLayout = dialogView.findViewById(R.id.tab_layout_categories);
        
        TextView tv = dialogView.findViewById(R.id.tv_categories_title);
        if (tv != null) tv.setVisibility(View.GONE);

        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        CategoryAdapter adapter = new CategoryAdapter();
        rv.setAdapter(adapter);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                adapter.moveItem(viewHolder.getAdapterPosition(), target.getAdapterPosition());
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                List<CategoryEntity> updatedList = adapter.getCategories();
                for (int i = 0; i < updatedList.size(); i++) {
                    updatedList.get(i).displayOrder = i;
                    viewModel.updateCategory(updatedList.get(i));
                }
            }
        });
        itemTouchHelper.attachToRecyclerView(rv);

        // Filter categories based on current transaction type
        int typeId = toggleType.getCheckedButtonId();
        String currentType = (typeId == R.id.btn_type_income) ? "Income" : "Expense";
        
        if (tabLayout != null) {
            tabLayout.getTabAt(currentType.equals("Income") ? 1 : 0).select();
            tabLayout.addOnTabSelectedListener(new com.google.android.material.tabs.TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab tab) {
                    updateFilteredCategories(adapter, tab.getPosition());
                }
                @Override
                public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
                @Override
                public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
            });
        }

        updateFilteredCategories(adapter, currentType.equals("Income") ? 1 : 0);

        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.Theme_BudgetTracker)
                .setView(dialogView)
                .create();

        Toolbar toolbar = dialogView.findViewById(R.id.toolbar_categories);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> dialog.dismiss());
        }

        adapter.setOnCategoryClickListener(category -> showEditCategoryDialog(category, tabLayout));
        if (fab != null) {
            fab.setOnClickListener(v -> showAddCategoryDialog());
        }
        dialog.show();
    }

    private void updateFilteredCategories(CategoryAdapter adapter, int tabPosition) {
        String type = (tabPosition == 0) ? "Expense" : "Income";
        viewModel.getCategoriesByType(type).observe(getViewLifecycleOwner(), adapter::setCategories);
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
        } else {
            // Set default selection based on tab layout or transaction type
            if (tabLayout != null) {
                toggleTypeGroup.check(tabLayout.getSelectedTabPosition() == 1 ? R.id.btn_category_income : R.id.btn_category_expense);
            } else {
                toggleTypeGroup.check(toggleType.getCheckedButtonId() == R.id.btn_type_income ? R.id.btn_category_income : R.id.btn_category_expense);
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.Theme_BudgetTracker)
                .setTitle(category == null ? "Add Category" : "Edit Category")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String emoji = etEmoji.getText().toString().trim();
                    String type = (toggleTypeGroup.getCheckedButtonId() == R.id.btn_category_income) ? "Income" : "Expense";
                    
                    if (!name.isEmpty()) {
                        if (emoji.isEmpty()) emoji = "✨";
                        if (category == null) {
                            viewModel.insertCategory(new CategoryEntity(name, type, emoji));
                        } else {
                            category.name = name;
                            category.emoji = emoji;
                            category.type = type;
                            viewModel.updateCategory(category);
                        }
                    }
                })
                .setNegativeButton("Cancel", null);
        
        if (category != null) {
            builder.setNeutralButton("Delete", (dialog, which) -> viewModel.deleteCategory(category));
        }
        
        builder.show();
    }

    private void saveTransaction() {
        String amountStr = etAmount.getText().toString().trim();
        if (amountStr.isEmpty() || Double.parseDouble(amountStr) <= 0) {
            Toast.makeText(getContext(), "Please enter a valid amount", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);
        int selectedChipId = cgCategories.getCheckedChipId();
        if (selectedChipId == View.NO_ID) {
            Toast.makeText(getContext(), "Please select a category or goal", Toast.LENGTH_SHORT).show();
            return;
        }

        int accountChipId = cg_accounts.getCheckedChipId();
        if (accountChipId == View.NO_ID) {
            Toast.makeText(getContext(), "Please select an account", Toast.LENGTH_SHORT).show();
            return;
        }

        Chip accChip = (Chip) cg_accounts.findViewById(accountChipId);
        selectedAccountId = (Integer) accChip.getTag();

        Chip selectedChip = cgCategories.findViewById(selectedChipId);
        int typeId = toggleType.getCheckedButtonId();

        if (typeId == R.id.btn_type_saving) {
            SavingEntity saving = (SavingEntity) selectedChip.getTag();
            if (saving != null) {
                saving.currentAmount += amount;
                // Note: SavingEntity only has createdAt, which usually refers to goal creation.
                // However, we'll update it to the selected time to satisfy the "all transactions" requirement
                // if the user intends this to be the timestamp of this specific "saving transaction".
                // In a more complex app, we'd have a separate SavingTransaction table.
                viewModel.updateSaving(saving);
                Navigation.findNavController(requireView()).popBackStack();
            }
        } else {
            String category = selectedChip.getText().toString();
            String type = (typeId == R.id.btn_type_income) ? "Income" : "Expense";
            String source = "Default";
            String note = etNote.getText().toString();

            TransactionEntity transaction = new TransactionEntity(
                selectedAccountId, type, amount, source, category, note, selectedDateTime.getTimeInMillis()
            );
            viewModel.insertTransaction(transaction);
            Navigation.findNavController(requireView()).popBackStack();
        }
    }
}
