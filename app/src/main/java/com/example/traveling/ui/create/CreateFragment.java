package com.example.traveling.ui.create;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.traveling.R;
import com.example.traveling.session.SessionManager;
import com.google.android.material.button.MaterialButton;

public class CreateFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialButton btnSharePhoto = view.findViewById(R.id.btn_share_photo);
        MaterialButton btnCreatePath = view.findViewById(R.id.btn_create_path);

        btnSharePhoto.setOnClickListener(v -> openSharePhoto());
        btnCreatePath.setOnClickListener(v -> openCreatePath());
    }

    private void openSharePhoto() {
        if (SessionManager.get().isAnonymous()) {
            Toast.makeText(requireContext(),
                    "Connectez-vous pour partager une photo", Toast.LENGTH_SHORT).show();
            return;
        }
        Navigation.findNavController(requireView()).navigate(R.id.action_create_to_share_photo);
    }

    private void openCreatePath() {
        if (SessionManager.get().isAnonymous()) {
            Toast.makeText(requireContext(),
                    "Connectez-vous pour créer un parcours", Toast.LENGTH_SHORT).show();
            return;
        }
        Navigation.findNavController(requireView()).navigate(R.id.action_create_to_create_path);
    }
}
