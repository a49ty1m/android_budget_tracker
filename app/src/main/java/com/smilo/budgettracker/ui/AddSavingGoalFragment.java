package com.smilo.budgettracker.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.smilo.budgettracker.R;
import com.smilo.budgettracker.db.AccountWithBalance;
import com.smilo.budgettracker.db.SavingEntity;
import java.util.ArrayList;
import java.util.List;

public class AddSavingGoalFragment extends Fragment {

    private BudgetViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_saving_goal, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(BudgetViewModel.class);

        EditText etGoalName = view.findViewById(R.id.et_goal_name);
        EditText etTargetAmount = view.findViewById(R.id.et_target_amount);
        EditText etCurrentAmount = view.findViewById(R.id.et_current_amount);
        EditText etEmoji = view.findViewById(R.id.et_emoji);
        AutoCompleteTextView actvAccount = view.findViewById(R.id.actv_saving_account);

        final List<AccountWithBalance>[] accountsWrapper = new List[1];
        final int[] selectedAccountId = {-1};

        viewModel.getAccountsWithBalance().observe(getViewLifecycleOwner(), accounts -> {
            if (accounts != null && !accounts.isEmpty()) {
                accountsWrapper[0] = accounts;
                List<String> accountNames = new ArrayList<>();
                for (AccountWithBalance acc : accounts) {
                    accountNames.add(acc.emoji + " (" + acc.userName + "-" + acc.databaseName + ")");
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

        view.findViewById(R.id.btn_cancel).setOnClickListener(v -> Navigation.findNavController(view).navigateUp());

        view.findViewById(R.id.btn_create).setOnClickListener(v -> {
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
                    Navigation.findNavController(view).navigateUp();
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(), "Invalid amount", Toast.LENGTH_SHORT).show();
                }
            } else if (selectedAccountId[0] == -1) {
                Toast.makeText(getContext(), "Please select an account", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
