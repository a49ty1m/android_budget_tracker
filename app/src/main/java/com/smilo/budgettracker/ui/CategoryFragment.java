package com.smilo.budgettracker.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import com.smilo.budgettracker.R;
import com.smilo.budgettracker.db.CategoryEntity;

public class CategoryFragment extends Fragment {

    private BudgetViewModel viewModel;
    private CategoryAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_categories, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView rvCategories = view.findViewById(R.id.rv_categories);
        View btnAdd = view.findViewById(R.id.btn_add_category_top);
        View toolbar = view.findViewById(R.id.toolbar_categories);
        com.google.android.material.tabs.TabLayout tabLayout = view.findViewById(R.id.tab_layout_categories);

        rvCategories.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CategoryAdapter();
        rvCategories.setAdapter(adapter);

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
                java.util.List<CategoryEntity> updatedList = adapter.getCategories();
                for (int i = 0; i < updatedList.size(); i++) {
                    updatedList.get(i).displayOrder = i;
                    viewModel.updateCategory(updatedList.get(i));
                }
            }
        });
        itemTouchHelper.attachToRecyclerView(rvCategories);

        viewModel = new ViewModelProvider(requireActivity()).get(BudgetViewModel.class);

        viewModel.getAllCategories().observe(getViewLifecycleOwner(), categories -> {
            updateFilteredList(tabLayout.getSelectedTabPosition());
        });

        tabLayout.addOnTabSelectedListener(new com.google.android.material.tabs.TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab tab) {
                updateFilteredList(tab.getPosition());
            }

            @Override
            public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
        });

        adapter.setOnCategoryClickListener(this::showEditCategoryDialog);

        if (btnAdd != null) {
            btnAdd.setOnClickListener(v -> showEditCategoryDialog(null));
        }

        if (toolbar instanceof androidx.appcompat.widget.Toolbar) {
            ((androidx.appcompat.widget.Toolbar) toolbar).setNavigationOnClickListener(v -> {
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            });
        }
    }

    private void updateFilteredList(int tabPosition) {
        String type;
        if (tabPosition == 0) type = "Expense";
        else type = "Income";

        if (viewModel.getAllCategories().getValue() != null) {
            java.util.List<CategoryEntity> filtered = new java.util.ArrayList<>();
            for (CategoryEntity category : viewModel.getAllCategories().getValue()) {
                if (type.equals(category.type)) {
                    filtered.add(category);
                }
            }
            adapter.setCategories(filtered);
        }
    }

    private void showEditCategoryDialog(@Nullable CategoryEntity category) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_category, null);
        EditText etName = dialogView.findViewById(R.id.et_category_name);
        EditText etEmoji = dialogView.findViewById(R.id.et_category_emoji);
        com.google.android.material.button.MaterialButtonToggleGroup toggleTypeGroup = dialogView.findViewById(R.id.toggle_category_type);

        if (category != null) {
            etName.setText(category.name);
            etEmoji.setText(category.emoji);
            if (toggleTypeGroup != null) {
                int checkId = R.id.btn_category_expense;
                if ("Income".equals(category.type)) checkId = R.id.btn_category_income;
                toggleTypeGroup.check(checkId);
            }
        } else {
            com.google.android.material.tabs.TabLayout tabLayout = getView().findViewById(R.id.tab_layout_categories);
            int currentTab = tabLayout != null ? tabLayout.getSelectedTabPosition() : 0;
            if (toggleTypeGroup != null) {
                int checkId = R.id.btn_category_expense;
                if (currentTab == 1) checkId = R.id.btn_category_income;
                toggleTypeGroup.check(checkId);
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.Theme_BudgetTracker)
                .setTitle(category == null ? "Add Category" : "Edit Category")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String emoji = etEmoji.getText().toString().trim();
                    String type = "Expense";
                    if (toggleTypeGroup != null) {
                        int checkedId = toggleTypeGroup.getCheckedButtonId();
                        if (checkedId == R.id.btn_category_income) type = "Income";
                        else type = "Expense";
                    } else {
                        com.google.android.material.tabs.TabLayout tabLayout = getView().findViewById(R.id.tab_layout_categories);
                        int currentTab = tabLayout != null ? tabLayout.getSelectedTabPosition() : 0;
                        if (currentTab == 1) type = "Income";
                        else type = "Expense";
                    }

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
            builder.setNeutralButton("Delete", (dialog, which) -> {
                viewModel.deleteCategory(category);
            });
        }
        
        builder.show();
    }
}
