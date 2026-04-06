package com.smilo.budgettracker.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_accounts")
public class UserAccountEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String userName;
    public String databaseName;
    public long createdAt;
    public long updatedAt;

    public UserAccountEntity(String userName, String databaseName, long createdAt, long updatedAt) {
        this.userName = userName;
        this.databaseName = databaseName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
