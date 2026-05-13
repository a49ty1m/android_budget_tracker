package com.smilo.budgettracker.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.smilo.budgettracker.R;
import com.smilo.budgettracker.db.UserAccountEntity;

public class AddAccountFragment extends Fragment {

    private BudgetViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_account, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(BudgetViewModel.class);

        EditText etUserName = view.findViewById(R.id.et_user_name);
        EditText etDatabaseName = view.findViewById(R.id.et_database_name);
        EditText etEmoji = view.findViewById(R.id.et_account_emoji);

        view.findViewById(R.id.btn_cancel).setOnClickListener(v -> Navigation.findNavController(view).navigateUp());

        view.findViewById(R.id.btn_create).setOnClickListener(v -> {
            String name = etUserName.getText().toString().trim();
            String db = etDatabaseName.getText().toString().trim();
            String emoji = etEmoji.getText().toString().trim();
            
            if (!name.isEmpty() && !db.isEmpty()) {
                if (emoji.isEmpty()) emoji = "💵";
                long currentTime = System.currentTimeMillis();
                viewModel.insertAccount(new UserAccountEntity(name, db, emoji, currentTime, currentTime));
                Navigation.findNavController(view).navigateUp();
            }
        });
    }
}
