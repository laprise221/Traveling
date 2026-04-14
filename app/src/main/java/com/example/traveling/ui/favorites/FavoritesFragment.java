package com.example.traveling.ui.favorites;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.traveling.R;
import com.example.traveling.session.SessionManager;
import com.google.android.material.tabs.TabLayout;

public class FavoritesFragment extends Fragment {

    private TabLayout tabLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favorites, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tabLayout = view.findViewById(R.id.tab_layout_favorites);
        View favoritesContainer = view.findViewById(R.id.favorites_container);
        TextView anonymousText = view.findViewById(R.id.anonymous_favorites_text);

        if (isAnonymous()) {
            tabLayout.setVisibility(View.GONE);
            favoritesContainer.setVisibility(View.GONE);
            anonymousText.setVisibility(View.VISIBLE);
            return;
        }

        tabLayout.addTab(tabLayout.newTab().setText("Photos"));
        tabLayout.addTab(tabLayout.newTab().setText("Parcours"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        showLikedPhotos();
                        break;
                    case 1:
                        showLikedPaths();
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        showLikedPhotos();
    }

    private boolean isAnonymous() {
        return SessionManager.get().isAnonymous();
    }

    private void showLikedPhotos() {
        // TODO: Afficher les photos likées (TravelShare)
    }

    private void showLikedPaths() {
        // TODO: Afficher les parcours likés (TravelPath)
    }
}
