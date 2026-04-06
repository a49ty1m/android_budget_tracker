package com.smilo.budgettracker.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.smilo.budgettracker.R;
import com.smilo.budgettracker.db.SavingEntity;
import com.smilo.budgettracker.db.SavingWithAccount;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SavingAdapter extends RecyclerView.Adapter<SavingAdapter.SavingViewHolder> {

    private List<SavingWithAccount> savings = new ArrayList<>();
    private OnSavingClickListener listener;

    public interface OnSavingClickListener {
        void onSavingClick(SavingEntity saving);
    }

    public void setOnSavingClickListener(OnSavingClickListener listener) {
        this.listener = listener;
    }

    public void setSavings(List<SavingWithAccount> savings) {
        if (savings == null) {
            this.savings = new ArrayList<>();
        } else {
            this.savings = savings;
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SavingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_saving, parent, false);
        return new SavingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SavingViewHolder holder, int position) {
        SavingWithAccount item = savings.get(position);
        SavingEntity saving = item.saving;
        holder.tvEmoji.setText(saving.emoji != null ? saving.emoji : "🎯");
        
        String goalText = saving.goalName;
        if (item.userName != null && item.accountName != null) {
            goalText += String.format(" (%s-%s)", item.userName, item.accountName);
        }
        holder.tvGoalName.setText(goalText);
        
        String amountLabel = String.format(Locale.getDefault(), "₹%.0f / ₹%.0f", 
                saving.currentAmount, saving.targetAmount);
        holder.tvAmountLabel.setText(amountLabel);

        int progress = (int) ((saving.currentAmount / saving.targetAmount) * 100);
        holder.tvPercentage.setText(String.format(Locale.getDefault(), "%d%%", progress));
        holder.progressSaving.setProgress(progress);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSavingClick(saving);
            }
        });
    }

    @Override
    public int getItemCount() {
        return savings.size();
    }

    static class SavingViewHolder extends RecyclerView.ViewHolder {
        TextView tvEmoji, tvGoalName, tvAmountLabel, tvPercentage;
        LinearProgressIndicator progressSaving;

        SavingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmoji = itemView.findViewById(R.id.tv_saving_emoji);
            tvGoalName = itemView.findViewById(R.id.tv_saving_goal_name);
            tvAmountLabel = itemView.findViewById(R.id.tv_saving_amount_label);
            tvPercentage = itemView.findViewById(R.id.tv_saving_percentage);
            progressSaving = itemView.findViewById(R.id.progress_saving);
        }
    }
}
