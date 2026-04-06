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
import com.smilo.budgettracker.R;
import com.smilo.budgettracker.db.UserAccountEntity;

public class SettingsFragment extends Fragment {

    private BudgetViewModel viewModel;
    private EditText etProfileName;
    private UserAccountEntity currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etProfileName = view.findViewById(R.id.et_profile_name);
        View btnSave = view.findViewById(R.id.btn_save_settings);
        View btnReset = view.findViewById(R.id.btn_reset_data);

        viewModel = new ViewModelProvider(requireActivity()).get(BudgetViewModel.class);

        viewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                currentUser = user;
                etProfileName.setText(user.userName);
            }
        });

        btnSave.setOnClickListener(v -> {
            if (currentUser != null) {
                String newName = etProfileName.getText().toString().trim();

                if (newName.isEmpty()) {
                    Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                currentUser.userName = newName;
                currentUser.updatedAt = System.currentTimeMillis();

                viewModel.updateAccount(currentUser);
                Toast.makeText(getContext(), "Settings saved!", Toast.LENGTH_SHORT).show();
                getParentFragmentManager().popBackStack();
            }
        });

        btnReset.setOnClickListener(v -> {
            viewModel.resetData();
            Toast.makeText(getContext(), "Transactions reset!", Toast.LENGTH_SHORT).show();
        });
    }
}
