package com.example.traveling.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.traveling.R;
import com.google.android.material.button.MaterialButton;

public class ProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialButton btnLogin = view.findViewById(R.id.btn_login);
        MaterialButton btnRegister = view.findViewById(R.id.btn_register);

        btnLogin.setOnClickListener(v -> openLogin());
        btnRegister.setOnClickListener(v -> openRegister());
    }

    private void openLogin() {
        Navigation.findNavController(requireView()).navigate(R.id.action_profile_to_login);
    }

    private void openRegister() {
        Navigation.findNavController(requireView()).navigate(R.id.action_profile_to_register);
    }
}
