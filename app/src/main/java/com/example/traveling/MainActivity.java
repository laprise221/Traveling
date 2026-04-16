package com.example.traveling;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment.getNavController();

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // Synchronise la sélection du bas avec la destination courante
        NavigationUI.setupWithNavController(bottomNav, navController);

        // Override : chaque tap sur un onglet vide la pile jusqu'à la racine de cet onglet
        bottomNav.setOnItemSelectedListener(item -> {
            int destId = item.getItemId();
            NavOptions opts = new NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    // Vide la pile jusqu'à la destination tap (inclusive), puis y navigue
                    .setPopUpTo(destId, true)
                    .build();
            try {
                navController.navigate(destId, null, opts);
            } catch (IllegalArgumentException e) {
                // Destination inconnue depuis ici, navigation simple
                navController.navigate(destId);
            }
            return true;
        });
    }
}
