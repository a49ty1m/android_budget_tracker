package com.smilo.budgettracker.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.smilo.budgettracker.db.AppDatabase;
import com.smilo.budgettracker.db.BudgetDAO;
import com.smilo.budgettracker.db.CategoryEntity;
import com.smilo.budgettracker.db.SavingEntity;
import com.smilo.budgettracker.db.TransactionEntity;
import com.smilo.budgettracker.db.UserAccountEntity;
import com.smilo.budgettracker.db.AccountWithBalance;
import com.smilo.budgettracker.db.SavingWithAccount;
import com.smilo.budgettracker.db.TransactionWithAccount;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BudgetRepository {
    
    private BudgetDAO budgetDAO;
    private ExecutorService executorService;
    
    public BudgetRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        budgetDAO = db.budgetDAO();
        executorService = Executors.newFixedThreadPool(2);
    }
    
    // User Account Operations
    public void insertUserAccount(UserAccountEntity account) {
        executorService.execute(() -> budgetDAO.insertUserAccount(account));
    }
    
    public void updateUserAccount(UserAccountEntity account) {
        executorService.execute(() -> budgetDAO.updateUserAccount(account));
    }
    
    public void deleteUserAccount(UserAccountEntity account) {
        executorService.execute(() -> budgetDAO.deleteUserAccount(account));
    }
    
    public LiveData<List<UserAccountEntity>> getAllUserAccounts() {
        return budgetDAO.getAllUserAccounts();
    }
    
    public LiveData<UserAccountEntity> getUserAccountById(int id) {
        return budgetDAO.getUserAccountById(id);
    }

    public UserAccountEntity getFirstUserSync() {
        return budgetDAO.getFirstUserSync();
    }
    
    // Transaction Operations
    public void insertTransaction(TransactionEntity transaction) {
        executorService.execute(() -> budgetDAO.insertTransaction(transaction));
    }
    
    public void updateTransaction(TransactionEntity transaction) {
        executorService.execute(() -> budgetDAO.updateTransaction(transaction));
    }
    
    public void deleteTransaction(TransactionEntity transaction) {
        executorService.execute(() -> budgetDAO.deleteTransaction(transaction));
    }
    
    public LiveData<List<TransactionWithAccount>> getTransactionsByUserId(int userId) {
        return budgetDAO.getTransactionsByUserId(userId);
    }
    
    public LiveData<List<TransactionWithAccount>> getAllTransactions() {
        return budgetDAO.getAllTransactions();
    }
    
    public void deleteAllTransactionsForUser(int userId) {
        executorService.execute(() -> budgetDAO.deleteAllTransactionsForUser(userId));
    }

    // Category Operations
    public void insertCategory(CategoryEntity category) {
        executorService.execute(() -> budgetDAO.insertCategory(category));
    }

    public void updateCategory(CategoryEntity category) {
        executorService.execute(() -> budgetDAO.updateCategory(category));
    }

    public void deleteCategory(CategoryEntity category) {
        executorService.execute(() -> budgetDAO.deleteCategory(category));
    }

    public LiveData<List<CategoryEntity>> getAllCategories() {
        return budgetDAO.getAllCategories();
    }

    public LiveData<List<CategoryEntity>> getCategoriesByType(String type) {
        return budgetDAO.getCategoriesByType(type);
    }

    public void initDefaultCategories() {
        executorService.execute(() -> {
            if (budgetDAO.getCategoryCountSync() == 0) {
                budgetDAO.insertCategory(new CategoryEntity("Food", "Expense", "🍔"));
                budgetDAO.insertCategory(new CategoryEntity("Travel", "Expense", "🚌"));
                budgetDAO.insertCategory(new CategoryEntity("Shop", "Expense", "🛍️"));
                budgetDAO.insertCategory(new CategoryEntity("Subs", "Expense", "📺"));
                budgetDAO.insertCategory(new CategoryEntity("Rent", "Expense", "🏠"));
                budgetDAO.insertCategory(new CategoryEntity("Other", "Expense", "✨"));
                
                budgetDAO.insertCategory(new CategoryEntity("Salary", "Income", "💰"));
                budgetDAO.insertCategory(new CategoryEntity("Gift", "Income", "🎁"));
                budgetDAO.insertCategory(new CategoryEntity("Freelance", "Income", "💼"));
                budgetDAO.insertCategory(new CategoryEntity("Pocket", "Income", "💵"));
            }
        });
    }

    // Savings Operations
    public void insertSaving(SavingEntity saving) {
        executorService.execute(() -> budgetDAO.insertSaving(saving));
    }

    public void updateSaving(SavingEntity saving) {
        executorService.execute(() -> budgetDAO.updateSaving(saving));
    }

    public void deleteSaving(SavingEntity saving) {
        executorService.execute(() -> budgetDAO.deleteSaving(saving));
    }

    public LiveData<List<SavingWithAccount>> getAllSavings() {
        return budgetDAO.getAllSavings();
    }

    public LiveData<List<AccountWithBalance>> getAccountsWithBalance() {
        return budgetDAO.getAccountsWithBalance();
    }
}
