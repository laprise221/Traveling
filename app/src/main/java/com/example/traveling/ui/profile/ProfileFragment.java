package com.example.traveling.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.traveling.R;
import com.example.traveling.data.FirestoreRepository;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileFragment extends Fragment {

    private TextView profileAvatarInitials, profileName, profileEmail;
    private TextView statPhotosCount, statPathsCount;
    private TextView tvPublicationsCount;
    private View layoutConnected, layoutAnonymous;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        profileAvatarInitials = view.findViewById(R.id.profile_avatar_initials);
        profileName = view.findViewById(R.id.profile_name);
        profileEmail = view.findViewById(R.id.profile_email);
        layoutConnected = view.findViewById(R.id.layout_connected);
        layoutAnonymous = view.findViewById(R.id.layout_anonymous);
        statPhotosCount = view.findViewById(R.id.stat_photos_count);
        statPathsCount = view.findViewById(R.id.stat_paths_count);
        tvPublicationsCount = view.findViewById(R.id.tv_publications_count);

        MaterialCardView cardPublications = view.findViewById(R.id.card_my_publications);
        MaterialCardView cardGroups = view.findViewById(R.id.card_my_groups);

        cardPublications.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_profile_to_publications));
        cardGroups.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_profile_to_groups));
        view.findViewById(R.id.btn_login).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_profile_to_login));
        view.findViewById(R.id.btn_register).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_profile_to_register));

        view.findViewById(R.id.btn_logout).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            updateUI();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }

    private void updateUI() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            layoutConnected.setVisibility(View.VISIBLE);
            layoutAnonymous.setVisibility(View.GONE);

            String displayName = user.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                profileName.setText(displayName);
                profileAvatarInitials.setText(getInitials(displayName));
            } else {
                profileName.setText("Utilisateur");
                profileAvatarInitials.setText("U");
            }
            profileEmail.setText(user.getEmail() != null ? user.getEmail() : "");

            updatePublications(user.getUid());
        } else {
            layoutConnected.setVisibility(View.GONE);
            layoutAnonymous.setVisibility(View.VISIBLE);
            profileName.setText("Mode anonyme");
            profileEmail.setText("Connectez-vous pour accéder à toutes les fonctionnalités");
            profileAvatarInitials.setText("?");
        }
    }

    private void updatePublications(String uid) {
        // Load photo count from Firestore
        FirestoreRepository.get().loadUserPhotos(uid, photos -> {
            if (!isAdded()) return;
            int photoCount = photos.size();
            statPhotosCount.setText(String.valueOf(photoCount));

            // Load path count from Firestore
            FirestoreRepository.get().loadUserPaths(uid, paths -> {
                if (!isAdded()) return;
                int pathCount = paths.size();
                statPathsCount.setText(String.valueOf(pathCount));

                int total = photoCount + pathCount;
                tvPublicationsCount.setText(total + " contenu" + (total > 1 ? "s" : ""));
            });
        });
    }

    private String getInitials(String fullName) {
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length >= 2) {
            return String.valueOf(parts[0].charAt(0)).toUpperCase()
                    + String.valueOf(parts[1].charAt(0)).toUpperCase();
        }
        return String.valueOf(fullName.charAt(0)).toUpperCase();
    }
}
