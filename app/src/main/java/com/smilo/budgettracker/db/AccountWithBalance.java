package com.smilo.budgettracker.db;

public class AccountWithBalance {
    public int id;
    public String userName;
    public String databaseName;
    public String emoji;
    public double balance;

    public AccountWithBalance(int id, String userName, String databaseName, String emoji, double balance) {
        this.id = id;
        this.userName = userName;
        this.databaseName = databaseName;
        this.emoji = emoji;
        this.balance = balance;
    }
}
