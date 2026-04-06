package com.smilo.budgettracker.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.smilo.budgettracker.R;
import com.smilo.budgettracker.db.TransactionEntity;
import com.smilo.budgettracker.db.TransactionWithAccount;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {
    
    private List<TransactionWithAccount> transactions = new ArrayList<>();
    private OnTransactionClickListener listener;

    public interface OnTransactionClickListener {
        void onTransactionClick(TransactionEntity transaction);
    }

    public void setOnTransactionClickListener(OnTransactionClickListener listener) {
        this.listener = listener;
    }
    
    public void setTransactions(List<TransactionWithAccount> transactions) {
        if (transactions == null) {
            this.transactions = new ArrayList<>();
        } else {
            this.transactions = transactions;
        }
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        TransactionWithAccount item = transactions.get(position);
        TransactionEntity transaction = item.transaction;
        
        String category = transaction.category != null ? transaction.category : "✨ Other";
        
        // Extract emoji and name
        String emoji = "✨";
        String name = category;
        if (category != null && category.length() >= 2) {
            // Simple check for emoji + space
            if (Character.isSurrogate(category.charAt(0)) || category.codePointAt(0) > 255) {
                 int emojiEnd = Character.charCount(category.codePointAt(0));
                 emoji = category.substring(0, emojiEnd);
                 name = category.substring(emojiEnd).trim();
            }
        }

        holder.tvSource.setText(name);
        holder.tvCategoryEmoji.setText(emoji);
        
        holder.tvNote.setText(transaction.note != null && !transaction.note.isEmpty() ? transaction.note : "No note");
        
        String accountDisplay = "UPI";
        if (item.userName != null && item.accountName != null) {
            accountDisplay = String.format("(%s-%s)", item.userName, item.accountName);
        } else if (transaction.source != null) {
            accountDisplay = transaction.source;
        }
        holder.tvPaymentMethod.setText(accountDisplay);

        boolean isIncome = "Income".equalsIgnoreCase(transaction.type);
        String prefix = isIncome ? "+" : "-";
        holder.tvAmount.setText(String.format(Locale.getDefault(), "%s₹%.0f", prefix, transaction.amount));
        
        int colorRes = isIncome ? R.color.money_left : R.color.overspending;
        holder.tvAmount.setTextColor(holder.itemView.getContext().getResources().getColor(colorRes));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTransactionClick(transaction);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return transactions.size();
    }
    
    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView tvSource, tvAmount, tvNote, tvCategoryEmoji, tvPaymentMethod;
        
        TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSource = itemView.findViewById(R.id.tv_source);
            tvAmount = itemView.findViewById(R.id.tv_amount);
            tvNote = itemView.findViewById(R.id.tv_note);
            tvCategoryEmoji = itemView.findViewById(R.id.tv_category_emoji);
            tvPaymentMethod = itemView.findViewById(R.id.tv_payment_method);
        }
    }
}
