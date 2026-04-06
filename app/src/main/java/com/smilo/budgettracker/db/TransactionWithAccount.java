package com.smilo.budgettracker.db;

import androidx.room.Embedded;

public class TransactionWithAccount {
    @Embedded
    public TransactionEntity transaction;
    
    public String userName;
    public String accountName;
}
