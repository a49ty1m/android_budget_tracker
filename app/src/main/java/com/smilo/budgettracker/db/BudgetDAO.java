package com.smilo.budgettracker.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface BudgetDAO {
    
    // User Account Operations
    @Insert
    long insertUserAccount(UserAccountEntity account);
    
    @Update
    void updateUserAccount(UserAccountEntity account);
    
    @Delete
    void deleteUserAccount(UserAccountEntity account);
    
    @Query("SELECT * FROM user_accounts")
    LiveData<List<UserAccountEntity>> getAllUserAccounts();
    
    @Query("SELECT * FROM user_accounts WHERE id = :id")
    LiveData<UserAccountEntity> getUserAccountById(int id);

    @Query("SELECT * FROM user_accounts LIMIT 1")
    UserAccountEntity getFirstUserSync();
    
    // Transaction Operations
    @Insert
    long insertTransaction(TransactionEntity transaction);
    
    @Update
    void updateTransaction(TransactionEntity transaction);
    
    @Delete
    void deleteTransaction(TransactionEntity transaction);
    
    @Query("SELECT t.*, ua.userName as userName, ua.databaseName as accountName FROM transactions t JOIN user_accounts ua ON t.userId = ua.id WHERE t.userId = :userId ORDER BY t.createdAt DESC")
    LiveData<List<TransactionWithAccount>> getTransactionsByUserId(int userId);
    
    @Query("SELECT * FROM transactions WHERE id = :id")
    TransactionEntity getTransactionById(int id);
    
    @Query("SELECT t.*, ua.userName as userName, ua.databaseName as accountName FROM transactions t JOIN user_accounts ua ON t.userId = ua.id ORDER BY t.createdAt DESC")
    LiveData<List<TransactionWithAccount>> getAllTransactions();
    
    @Query("SELECT SUM(amount) FROM transactions WHERE userId = :userId AND type = :type")
    double getSumByUserAndType(int userId, String type);

    @Query("DELETE FROM transactions WHERE userId = :userId")
    void deleteAllTransactionsForUser(int userId);

    // Category Operations
    @Insert
    long insertCategory(CategoryEntity category);

    @Update
    void updateCategory(CategoryEntity category);

    @Delete
    void deleteCategory(CategoryEntity category);

    @Query("SELECT * FROM categories ORDER BY displayOrder ASC, id ASC")
    LiveData<List<CategoryEntity>> getAllCategories();

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY displayOrder ASC, id ASC")
    LiveData<List<CategoryEntity>> getCategoriesByType(String type);

    @Query("SELECT COUNT(*) FROM categories")
    int getCategoryCountSync();

    // Savings Operations
    @Insert
    long insertSaving(SavingEntity saving);

    @Update
    void updateSaving(SavingEntity saving);

    @Delete
    void deleteSaving(SavingEntity saving);

    @Query("SELECT s.*, ua.userName as userName, ua.databaseName as accountName FROM savings s JOIN user_accounts ua ON s.userId = ua.id ORDER BY s.createdAt DESC")
    LiveData<List<SavingWithAccount>> getAllSavings();

    @Query("SELECT ua.id, ua.userName, ua.databaseName, COALESCE(SUM(CASE WHEN t.type = 'Income' THEN t.amount ELSE -t.amount END), 0) as balance FROM user_accounts ua LEFT JOIN transactions t ON ua.id = t.userId GROUP BY ua.id")
    LiveData<List<AccountWithBalance>> getAccountsWithBalance();
}

