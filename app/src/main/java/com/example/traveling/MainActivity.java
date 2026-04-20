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

import com.example.traveling.data.NotificationRepository;
import com.google.android.material.badge.BadgeDrawable;
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
        NavigationUI.setupWithNavController(bottomNav, navController);

        bottomNav.setOnItemSelectedListener(item -> {
            int destId = item.getItemId();

            NavOptions opts = new NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setPopUpTo(R.id.navigation_explore, false)
                    .build();

            navController.navigate(destId, null, opts);
            return true;
        });

        refreshNotificationBadge();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshNotificationBadge();
    }

    private void refreshNotificationBadge() {
        NotificationRepository.get().getUnreadCount(count -> {
            BottomNavigationView nav = findViewById(R.id.bottom_navigation);
            if (nav == null) return;
            if (count > 0) {
                BadgeDrawable badge = nav.getOrCreateBadge(R.id.navigation_profile);
                badge.setVisible(true);
                badge.setNumber(count);
            } else {
                nav.removeBadge(R.id.navigation_profile);
            }
        });
    }
}
