package com.smilo.budgettracker.db;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "transactions",
    foreignKeys = @ForeignKey(
        entity = UserAccountEntity.class,
        parentColumns = "id",
        childColumns = "userId",
        onDelete = ForeignKey.CASCADE
    )
)
public class TransactionEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public int userId;
    public String type;
    public double amount;
    public String source;
    public String category;
    public String note;
    public long createdAt;

    public TransactionEntity(int userId, String type, double amount, String source, 
                           String category, String note, long createdAt) {
        this.userId = userId;
        this.type = type;
        this.amount = amount;
        this.source = source;
        this.category = category;
        this.note = note;
        this.createdAt = createdAt;
    }
}
