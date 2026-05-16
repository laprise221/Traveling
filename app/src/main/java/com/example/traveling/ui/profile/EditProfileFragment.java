package com.example.traveling.ui.profile;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.traveling.R;
import com.example.traveling.data.FirestoreRepository;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import java.util.ArrayList;
import java.util.List;

public class EditProfileFragment extends Fragment {

    private TextInputEditText etUsername;
    private ChipGroup chipGroupTags;
    private ProgressBar progressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etUsername = view.findViewById(R.id.et_username);
        chipGroupTags = view.findViewById(R.id.chip_group_tags);
        progressBar = view.findViewById(R.id.progress_bar);

        view.findViewById(R.id.btn_back).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            if (user.getDisplayName() != null) etUsername.setText(user.getDisplayName());
            FirestoreRepository.get().loadFollowedTags(user.getUid(), tags -> {
                if (!isAdded()) return;
                for (String tag : tags) {
                    checkChipForTag(tag);
                }
            });
        }

        view.findViewById(R.id.btn_save).setOnClickListener(v -> save());
    }

    private void checkChipForTag(String tag) {
        String lower = tag.toLowerCase();
        int chipId = View.NO_ID;
        if (lower.equals("nature"))           chipId = R.id.chip_tag_nature;
        else if (lower.equals("musée"))       chipId = R.id.chip_tag_musee;
        else if (lower.contains("rue"))       chipId = R.id.chip_tag_rue;
        else if (lower.equals("restaurant"))  chipId = R.id.chip_tag_restaurant;
        else if (lower.equals("culture"))     chipId = R.id.chip_tag_culture;
        else if (lower.equals("restauration"))chipId = R.id.chip_tag_restauration;
        else if (lower.equals("loisirs"))     chipId = R.id.chip_tag_loisirs;
        else if (lower.contains("couverte"))  chipId = R.id.chip_tag_decouverte;
        else if (lower.equals("autre"))       chipId = R.id.chip_tag_autre;

        if (chipId != View.NO_ID) {
            Chip chip = chipGroupTags.findViewById(chipId);
            if (chip != null) chip.setChecked(true);
        }
    }

    private void save() {
        String newUsername = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
        if (TextUtils.isEmpty(newUsername)) {
            etUsername.setError("Nom d'utilisateur requis");
            etUsername.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        requireView().findViewById(R.id.btn_save).setEnabled(false);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        List<String> selectedTags = getSelectedTags();

        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(newUsername)
                .build();

        user.updateProfile(profileUpdates).addOnCompleteListener(task -> {
            if (!isAdded()) return;
            if (task.isSuccessful()) {
                FirestoreRepository.get().saveUser(user.getUid(), newUsername, user.getEmail() != null ? user.getEmail() : "");
                FirestoreRepository.get().saveFollowedTags(user.getUid(), selectedTags, v -> {
                    if (!isAdded()) return;
                    progressBar.setVisibility(View.GONE);
                    requireView().findViewById(R.id.btn_save).setEnabled(true);
                    Toast.makeText(requireContext(), "Profil mis à jour", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(requireView()).navigateUp();
                });
            } else {
                progressBar.setVisibility(View.GONE);
                requireView().findViewById(R.id.btn_save).setEnabled(true);
                Toast.makeText(requireContext(), "Erreur lors de la mise à jour", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private List<String> getSelectedTags() {
        List<String> tags = new ArrayList<>();
        for (int id : chipGroupTags.getCheckedChipIds()) {
            Chip chip = chipGroupTags.findViewById(id);
            if (chip != null) tags.add(chip.getText().toString().toLowerCase());
        }
        return tags;
    }
}
