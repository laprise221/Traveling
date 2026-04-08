package com.example.traveling.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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

    private FirebaseAuth mAuth;

    private TextView profileName;
    private TextView profileStatus;
    private LinearLayout layoutAuthButtons;
    private MaterialButton btnLogout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        profileName = view.findViewById(R.id.profile_name);
        profileStatus = view.findViewById(R.id.profile_status);
        layoutAuthButtons = view.findViewById(R.id.layout_auth_buttons);
        btnLogout = view.findViewById(R.id.btn_logout);

        MaterialButton btnLogin = view.findViewById(R.id.btn_login);
        MaterialButton btnRegister = view.findViewById(R.id.btn_register);

        btnLogin.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_profile_to_login));
        btnRegister.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_profile_to_register));
        btnLogout.setOnClickListener(v -> logout());
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI(mAuth.getCurrentUser());
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            String displayName = user.getDisplayName();
            profileName.setText(displayName != null && !displayName.isEmpty()
                    ? displayName : "Utilisateur");
            profileStatus.setText(user.getEmail());
            layoutAuthButtons.setVisibility(View.GONE);
            btnLogout.setVisibility(View.VISIBLE);
        } else {
            profileName.setText("Mode anonyme");
            profileStatus.setText("Connectez-vous pour accéder à toutes les fonctionnalités");
            layoutAuthButtons.setVisibility(View.VISIBLE);
            btnLogout.setVisibility(View.GONE);
        }
    }

    private void logout() {
        mAuth.signOut();
        updateUI(null);
    }
}
