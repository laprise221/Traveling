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
import com.example.traveling.data.GroupRepository;
import com.example.traveling.data.NotificationRepository;
import com.example.traveling.session.SessionManager;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileFragment extends Fragment {

    private TextView profileAvatarInitials, profileName, profileEmail;
    private TextView statPhotosCount, statPathsCount;
    private TextView tvPublicationsCount, tvGroupsCount;
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
        tvGroupsCount = view.findViewById(R.id.tv_groups_count);

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

        MaterialButton btnNotifications = view.findViewById(R.id.btn_notifications);
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v ->
                    Navigation.findNavController(v).navigate(R.id.action_profile_to_notifications));
        }

        MaterialButton btnEditProfile = view.findViewById(R.id.btn_edit_profile);
        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v ->
                    Navigation.findNavController(v).navigate(R.id.action_profile_to_edit_profile));
        }

        view.findViewById(R.id.btn_logout).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            updateUI();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
        updateNotificationBadge();
    }

    private void updateNotificationBadge() {
        if (SessionManager.get().isGuest()) return;
        NotificationRepository.get().getUnreadCount(count -> {
            if (!isAdded() || getActivity() == null) return;

            // Badge on bottom nav Profile icon
            BottomNavigationView nav = getActivity().findViewById(R.id.bottom_navigation);
            if (nav != null) {
                if (count > 0) {
                    BadgeDrawable badge = nav.getOrCreateBadge(R.id.navigation_profile);
                    badge.setVisible(true);
                    badge.setNumber(count);
                } else {
                    nav.removeBadge(R.id.navigation_profile);
                }
            }

            // Counter next to the Notifications button
            MaterialButton btnNotifications = getView() != null
                    ? getView().findViewById(R.id.btn_notifications) : null;
            if (btnNotifications != null) {
                String label = count > 0
                        ? "Notifications  ·  " + count + " non lue" + (count > 1 ? "s" : "")
                        : "Notifications";
                btnNotifications.setText(label);
            }
        });
    }

    private void updateUI() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        boolean isRealUser = user != null && !user.isAnonymous();

        if (isRealUser) {
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
        FirestoreRepository.get().loadUserPhotos(uid, photos -> {
            if (!isAdded()) return;
            int photoCount = photos.size();
            statPhotosCount.setText(String.valueOf(photoCount));

            FirestoreRepository.get().loadUserPaths(uid, paths -> {
                if (!isAdded()) return;
                int pathCount = paths.size();
                statPathsCount.setText(String.valueOf(pathCount));
                int total = photoCount + pathCount;
                tvPublicationsCount.setText(total + " contenu" + (total > 1 ? "s" : ""));
            });
        });

        GroupRepository.get().loadUserGroups(groups -> {
            if (!isAdded()) return;
            int count = groups.size();
            tvGroupsCount.setText(count + " groupe" + (count > 1 ? "s" : ""));
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
