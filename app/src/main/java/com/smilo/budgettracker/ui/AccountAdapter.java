package com.smilo.budgettracker.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.smilo.budgettracker.R;
import com.smilo.budgettracker.db.AccountWithBalance;
import java.util.ArrayList;
import java.util.List;

public class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.AccountViewHolder> {

    private List<AccountWithBalance> accounts = new ArrayList<>();

    public interface OnAccountClickListener {
        void onAccountClick(AccountWithBalance account);
    }

    private OnAccountClickListener listener;

    public void setOnAccountClickListener(OnAccountClickListener listener) {
        this.listener = listener;
    }

    public void setAccounts(List<AccountWithBalance> accounts) {
        this.accounts = accounts;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AccountViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_account, parent, false);
        return new AccountViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AccountViewHolder holder, int position) {
        AccountWithBalance account = accounts.get(position);
        holder.tvUserName.setText(String.format("(%s-%s)", account.userName, account.databaseName));
        holder.tvDatabaseName.setVisibility(View.GONE);

        holder.tvAccountBalance.setText(String.format("₹%.2f", account.balance));

        // Color based on balance
        int color = account.balance >= 0 ? R.color.money_left : R.color.overspending;
        holder.tvAccountBalance.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), color));

        // Icon based on name
        if (account.databaseName.toLowerCase().contains("bank")) {
            holder.ivIcon.setImageResource(android.R.drawable.ic_menu_agenda);
            holder.ivIcon.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.accent_gold));
        } else {
            holder.ivIcon.setImageResource(android.R.drawable.ic_menu_myplaces);
            holder.ivIcon.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.income_green));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAccountClick(account);
            }
        });
    }

    @Override
    public int getItemCount() {
        return accounts.size();
    }

    static class AccountViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvDatabaseName, tvAccountBalance;
        ImageView ivIcon;

        AccountViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tv_user_name);
            tvDatabaseName = itemView.findViewById(R.id.tv_database_name);
            tvAccountBalance = itemView.findViewById(R.id.tv_account_balance);
            ivIcon = itemView.findViewById(R.id.iv_account_icon);
        }
    }
}

