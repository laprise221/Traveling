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
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileFragment extends Fragment {

    private TextView profileAvatarInitials;
    private TextView profileName;
    private TextView profileEmail;
    private View layoutConnected;
    private View layoutAnonymous;

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

        view.findViewById(R.id.btn_login).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_profile_to_login));
        view.findViewById(R.id.btn_register).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_profile_to_register));

        MaterialButton btnLogout = view.findViewById(R.id.btn_logout);
        btnLogout.setOnClickListener(v -> {
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
            // Connecté
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

            String email = user.getEmail();
            profileEmail.setText(email != null ? email : "");

        } else {
            // Anonyme
            layoutConnected.setVisibility(View.GONE);
            layoutAnonymous.setVisibility(View.VISIBLE);
            profileName.setText("Mode anonyme");
            profileEmail.setText("Connectez-vous pour accéder à toutes les fonctionnalités");
            profileAvatarInitials.setText("?");
        }
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
