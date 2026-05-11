package com.smilo.budgettracker.db;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "categories",
        indices = {@Index(value = {"name", "type"}, unique = true)})
public class CategoryEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String name;
    public String type; // "Expense" or "Income"
    public String emoji;
    public int displayOrder;

    public CategoryEntity(String name, String type, String emoji) {
        this.name = name;
        this.type = type;
        this.emoji = emoji;
        this.displayOrder = 0;
    }
}
