package com.smilo.budgettracker.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
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
            if (currentUser == null) return;

            final EditText input = new EditText(requireContext());
            input.setHint(R.string.reset_confirm_hint);
            input.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            input.setHintTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));

            android.widget.FrameLayout container = new android.widget.FrameLayout(requireContext());
            android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            int margin = (int) (24 * getResources().getDisplayMetrics().density);
            params.setMargins(margin, margin / 2, margin, margin / 2);
            input.setLayoutParams(params);
            container.addView(input);

            new androidx.appcompat.app.AlertDialog.Builder(requireContext(), R.style.Theme_BudgetTracker)
                    .setTitle(R.string.nuclear_option)
                    .setMessage(R.string.nuclear_desc)
                    .setView(container)
                    .setPositiveButton(R.string.nuclear_positive, (dialog, which) -> {
                        String newName = input.getText().toString().trim();
                        if (!newName.isEmpty()) {
                            viewModel.resetEverything(newName);
                            Toast.makeText(getContext(), R.string.poof_gone, Toast.LENGTH_SHORT).show();
                            getParentFragmentManager().popBackStack();
                        } else {
                            Toast.makeText(getContext(), R.string.reset_error_empty, Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton(R.string.nuclear_negative, null)
                    .show();
        });
    }
}
