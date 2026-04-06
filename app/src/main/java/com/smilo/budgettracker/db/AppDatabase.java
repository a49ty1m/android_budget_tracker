package com.smilo.budgettracker.db;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
    entities = {UserAccountEntity.class, TransactionEntity.class, CategoryEntity.class, SavingEntity.class},
    version = 7,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    
    public abstract BudgetDAO budgetDAO();
    
    private static volatile AppDatabase INSTANCE;
    
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "budget_tracker.db"
                        )
                        .fallbackToDestructiveMigration()
                        .build();
                }
            }
        }
        return INSTANCE;
    }
}
