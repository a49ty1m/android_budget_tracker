package com.smilo.budgettracker.db;

import androidx.room.Embedded;

public class SavingWithAccount {
    @Embedded
    public SavingEntity saving;
    
    public String userName;
    public String accountName;
}
