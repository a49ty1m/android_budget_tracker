package com.smilo.budgettracker.db;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "savings",
    foreignKeys = @ForeignKey(
        entity = UserAccountEntity.class,
        parentColumns = "id",
        childColumns = "userId",
        onDelete = ForeignKey.CASCADE
    )
)
public class SavingEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public int userId;
    public String goalName;
    public double targetAmount;
    public double currentAmount;
    public String emoji;
    public long createdAt;

    public SavingEntity(int userId, String goalName, double targetAmount, double currentAmount, String emoji, long createdAt) {
        this.userId = userId;
        this.goalName = goalName;
        this.targetAmount = targetAmount;
        this.currentAmount = currentAmount;
        this.emoji = emoji;
        this.createdAt = createdAt;
    }
}
