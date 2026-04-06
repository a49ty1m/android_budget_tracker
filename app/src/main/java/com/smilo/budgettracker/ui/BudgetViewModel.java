package com.smilo.budgettracker.ui;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import com.smilo.budgettracker.db.AccountWithBalance;
import com.smilo.budgettracker.db.CategoryEntity;
import com.smilo.budgettracker.db.SavingEntity;
import com.smilo.budgettracker.db.SavingWithAccount;
import com.smilo.budgettracker.db.TransactionEntity;
import com.smilo.budgettracker.db.TransactionWithAccount;
import com.smilo.budgettracker.db.UserAccountEntity;
import com.smilo.budgettracker.repository.BudgetRepository;
import java.util.List;

public class BudgetViewModel extends AndroidViewModel {
    
    private BudgetRepository repository;
    private LiveData<List<TransactionWithAccount>> allTransactions;
    private MutableLiveData<Integer> currentUserId = new MutableLiveData<>(1);
    
    private LiveData<UserAccountEntity> currentUser;
    private MediatorLiveData<Double> totalBalance = new MediatorLiveData<>();
    private MediatorLiveData<Double> totalIncome = new MediatorLiveData<>();
    private MediatorLiveData<Double> totalExpense = new MediatorLiveData<>();
    private LiveData<List<CategoryEntity>> allCategories;
    private LiveData<List<SavingWithAccount>> allSavings;
    private LiveData<List<AccountWithBalance>> accountsWithBalance;

    public BudgetViewModel(@NonNull Application application) {
        super(application);
        repository = new BudgetRepository(application);
        
        currentUser = Transformations.switchMap(currentUserId, id -> repository.getUserAccountById(id));
        allTransactions = Transformations.switchMap(currentUserId, id -> repository.getTransactionsByUserId(id));
        allCategories = repository.getAllCategories();
        allSavings = repository.getAllSavings();
        accountsWithBalance = repository.getAccountsWithBalance();
        
        setupCalculations();
        ensureUserExists();
        repository.initDefaultCategories();
    }
    
    private void ensureUserExists() {
        new Thread(() -> {
            UserAccountEntity user = repository.getFirstUserSync();
            if (user == null) {
                long now = System.currentTimeMillis();
                repository.insertUserAccount(new UserAccountEntity("Smilo", "Wallet", now, now));
                repository.insertUserAccount(new UserAccountEntity("Smilo", "HDFC Bank", now, now));
            } else {
                currentUserId.postValue(user.id);
            }
        }).start();
    }
    
    private void setupCalculations() {
        totalIncome.addSource(allTransactions, this::calculateTotals);
        totalExpense.addSource(allTransactions, this::calculateTotals);
        totalBalance.addSource(allTransactions, this::calculateTotals);
        totalBalance.addSource(allSavings, s -> calculateTotals(allTransactions.getValue()));
    }
    
    private void calculateTotals(List<TransactionWithAccount> transactions) {
        double income = 0;
        double expense = 0;
        if (transactions != null) {
            for (TransactionWithAccount t : transactions) {
                if ("Income".equalsIgnoreCase(t.transaction.type)) {
                    income += t.transaction.amount;
                } else if ("Expense".equalsIgnoreCase(t.transaction.type)) {
                    expense += t.transaction.amount;
                }
            }
        }
        totalIncome.setValue(income);
        totalExpense.setValue(expense);

        // Calculate total savings to deduct from balance (loot)
        double totalSavingsAmount = 0;
        List<SavingWithAccount> savingsList = allSavings.getValue();
        if (savingsList != null) {
            for (SavingWithAccount s : savingsList) {
                totalSavingsAmount += s.saving.currentAmount;
            }
        }

        // Balance = Income - Expense - Savings
        totalBalance.setValue(income - expense - totalSavingsAmount);
    }

    public LiveData<List<UserAccountEntity>> getAllUserAccounts() {
        return repository.getAllUserAccounts();
    }

    public LiveData<List<AccountWithBalance>> getAccountsWithBalance() {
        return accountsWithBalance;
    }

    public LiveData<List<TransactionWithAccount>> getAllTransactions() {
        return allTransactions;
    }
    
    public LiveData<UserAccountEntity> getCurrentUser() {
        return currentUser;
    }
    
    public LiveData<Double> getTotalBalance() { return totalBalance; }
    public LiveData<Double> getTotalIncome() { return totalIncome; }
    public LiveData<Double> getTotalExpense() { return totalExpense; }

    public void insertAccount(UserAccountEntity account) {
        repository.insertUserAccount(account);
    }

    public void updateAccount(UserAccountEntity account) {
        repository.updateUserAccount(account);
    }

    public void deleteAccount(UserAccountEntity account) {
        repository.deleteUserAccount(account);
    }

    public void insertTransaction(TransactionEntity transaction) {
        repository.insertTransaction(transaction);
    }

    public void updateTransaction(TransactionEntity transaction) {
        repository.updateTransaction(transaction);
    }

    public void deleteTransaction(TransactionEntity transaction) {
        repository.deleteTransaction(transaction);
    }
    
    public void resetData() {
        Integer userId = currentUserId.getValue();
        if (userId != null) {
            repository.deleteAllTransactionsForUser(userId);
        }
    }
    
    public void setCurrentUserId(int userId) {
        currentUserId.setValue(userId);
    }

    // Category Operations
    public LiveData<List<CategoryEntity>> getAllCategories() {
        return allCategories;
    }

    public LiveData<List<CategoryEntity>> getCategoriesByType(String type) {
        return repository.getCategoriesByType(type);
    }

    public void insertCategory(CategoryEntity category) {
        repository.insertCategory(category);
    }

    public void updateCategory(CategoryEntity category) {
        repository.updateCategory(category);
    }

    public void deleteCategory(CategoryEntity category) {
        repository.deleteCategory(category);
    }

    // Savings Operations
    public LiveData<List<SavingWithAccount>> getAllSavings() {
        return allSavings;
    }

    public void insertSaving(SavingEntity saving) {
        repository.insertSaving(saving);
    }

    public void updateSaving(SavingEntity saving) {
        repository.updateSaving(saving);
    }

    public void deleteSaving(SavingEntity saving) {
        repository.deleteSaving(saving);
    }
}

