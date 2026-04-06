package com.smilo.budgettracker.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.smilo.budgettracker.R;

public class MainActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
            NavigationUI.setupWithNavController(bottomNav, navController);

            // Ensure bottom navigation buttons always take you to the root of that section
            bottomNav.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                // If the destination is already in the backstack, pop back to it.
                // This ensures we always go to the root of the selected section (e.g., from Account back to Add).
                if (navController.popBackStack(itemId, false)) {
                    return true;
                }
                return NavigationUI.onNavDestinationSelected(item, navController);
            });

            // Reset to the section root when clicking the already-selected tab
            bottomNav.setOnItemReselectedListener(item -> {
                navController.popBackStack(item.getItemId(), false);
            });
        }
    }
}
