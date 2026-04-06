package com.smilo.budgettracker.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.smilo.budgettracker.R;
import com.smilo.budgettracker.db.CategoryEntity;
import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {
    
    private List<CategoryEntity> categories = new ArrayList<>();
    private OnCategoryClickListener listener;

    public interface OnCategoryClickListener {
        void onCategoryClick(CategoryEntity category);
    }

    public void setCategories(List<CategoryEntity> categories) {
        this.categories = categories;
        notifyDataSetChanged();
    }

    public void setOnCategoryClickListener(OnCategoryClickListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_edit, parent, false);
        return new CategoryViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        CategoryEntity category = categories.get(position);
        holder.tvEmoji.setText(category.emoji);
        holder.tvName.setText(category.name);
        holder.tvType.setText(category.type);
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onCategoryClick(category);
        });
    }

    public void moveItem(int fromPosition, int toPosition) {
        CategoryEntity item = categories.remove(fromPosition);
        categories.add(toPosition, item);
        notifyItemMoved(fromPosition, toPosition);
    }

    public List<CategoryEntity> getCategories() {
        return categories;
    }
    
    @Override
    public int getItemCount() {
        return categories.size();
    }
    
    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvEmoji, tvName, tvType;
        
        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmoji = itemView.findViewById(R.id.tv_category_emoji);
            tvName = itemView.findViewById(R.id.tv_category_name);
            tvType = itemView.findViewById(R.id.tv_category_type);
        }
    }
}
