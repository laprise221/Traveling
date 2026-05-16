package com.example.traveling.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.traveling.R;
import com.example.traveling.data.FirestoreRepository;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class TagSelectionFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tag_selection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ChipGroup chipGroup = view.findViewById(R.id.chip_group_tags);

        view.findViewById(R.id.btn_save_tags).setOnClickListener(v -> {
            List<String> selectedTags = getSelectedTags(chipGroup);
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && !selectedTags.isEmpty()) {
                FirestoreRepository.get().saveFollowedTags(user.getUid(), selectedTags, null);
            }
            goToProfile(view);
        });

        view.findViewById(R.id.btn_skip_tags).setOnClickListener(v -> goToProfile(view));
    }

    private List<String> getSelectedTags(ChipGroup chipGroup) {
        List<String> tags = new ArrayList<>();
        for (int id : chipGroup.getCheckedChipIds()) {
            Chip chip = chipGroup.findViewById(id);
            if (chip != null) tags.add(chip.getText().toString().toLowerCase());
        }
        return tags;
    }

    private void goToProfile(View view) {
        Navigation.findNavController(view).navigate(R.id.navigation_profile);
    }
}
