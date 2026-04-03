package com.example.traveling.ui.create;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.traveling.R;
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
        // TODO: Ouvrir l'écran de publication de photo (TravelShare)
    }

    private void openCreatePath() {
        // TODO: Ouvrir l'écran de création de parcours (TravelPath)
    }
}
