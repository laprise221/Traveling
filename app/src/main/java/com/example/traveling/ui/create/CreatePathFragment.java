package com.example.traveling.ui.create;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.traveling.R;
import com.example.traveling.data.UserRepository;
import com.example.traveling.model.TravelPath;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class CreatePathFragment extends Fragment {

    private TextInputEditText etTitle;
    private TextInputEditText etCity;
    private TextInputEditText etDescription;
    private ChipGroup chipGroupDuration;
    private ChipGroup chipGroupBudget;
    private ChipGroup chipGroupDifficulty;
    private LinearLayout stepsContainer;
    private SwitchMaterial switchPublic;

    private final List<String> steps = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_path, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etTitle = view.findViewById(R.id.et_title);
        etCity = view.findViewById(R.id.et_city);
        etDescription = view.findViewById(R.id.et_description);
        chipGroupDuration = view.findViewById(R.id.chip_group_duration);
        chipGroupBudget = view.findViewById(R.id.chip_group_budget);
        chipGroupDifficulty = view.findViewById(R.id.chip_group_difficulty);
        stepsContainer = view.findViewById(R.id.steps_container);
        switchPublic = view.findViewById(R.id.switch_public);

        view.findViewById(R.id.btn_back).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());

        view.findViewById(R.id.btn_publish).setOnClickListener(v -> publish());

        view.findViewById(R.id.btn_add_step).setOnClickListener(v -> showAddStepDialog());
    }

    private void showAddStepDialog() {
        TextInputLayout inputLayout = new TextInputLayout(requireContext());
        inputLayout.setHint("Nom de l'étape (ex: Tour Eiffel)");
        inputLayout.setPadding(48, 16, 48, 0);

        TextInputEditText input = new TextInputEditText(requireContext());
        inputLayout.addView(input);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Ajouter une étape")
                .setView(inputLayout)
                .setPositiveButton("Ajouter", (dialog, which) -> {
                    String stepName = input.getText() != null ? input.getText().toString().trim() : "";
                    if (!stepName.isEmpty()) {
                        steps.add(stepName);
                        addStepView(stepName, steps.size());
                    }
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void addStepView(String stepName, int index) {
        View stepView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_step, stepsContainer, false);

        TextView tvStepNumber = stepView.findViewById(R.id.tv_step_number);
        TextView tvStepName = stepView.findViewById(R.id.tv_step_name);

        tvStepNumber.setText(String.valueOf(index));
        tvStepName.setText(stepName);

        stepView.findViewById(R.id.btn_remove_step).setOnClickListener(v -> {
            steps.remove(stepName);
            stepsContainer.removeView(stepView);
            refreshStepNumbers();
        });

        stepsContainer.addView(stepView);
    }

    private void refreshStepNumbers() {
        for (int i = 0; i < stepsContainer.getChildCount(); i++) {
            TextView tvNum = stepsContainer.getChildAt(i).findViewById(R.id.tv_step_number);
            if (tvNum != null) tvNum.setText(String.valueOf(i + 1));
        }
    }

    private void publish() {
        String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        String city = etCity.getText() != null ? etCity.getText().toString().trim() : "";
        String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";

        if (title.isEmpty()) {
            etTitle.setError("Titre requis");
            etTitle.requestFocus();
            return;
        }
        if (city.isEmpty()) {
            etCity.setError("Ville requise");
            etCity.requestFocus();
            return;
        }

        String duration = getSelectedChipText(chipGroupDuration);
        String budget = getSelectedChipText(chipGroupBudget);
        String difficulty = getSelectedChipText(chipGroupDifficulty);
        boolean isPublic = switchPublic.isChecked();

        String id = "path_" + System.currentTimeMillis();
        TravelPath path = new TravelPath(id, title, city, description,
                "Moi", 0, 0,
                duration.isEmpty() ? "-" : duration,
                budget.isEmpty() ? "-" : budget,
                difficulty.isEmpty() ? "-" : difficulty,
                "équilibré", steps.size(), 0, R.drawable.sample_path_1);
        UserRepository.get().addPath(path);

        Toast.makeText(requireContext(), "Parcours publié !", Toast.LENGTH_SHORT).show();
        Navigation.findNavController(requireView()).navigateUp();
    }

    private String getSelectedChipText(ChipGroup group) {
        int checkedId = group.getCheckedChipId();
        if (checkedId == View.NO_ID) return "";
        View chip = group.findViewById(checkedId);
        if (chip instanceof com.google.android.material.chip.Chip) {
            return ((com.google.android.material.chip.Chip) chip).getText().toString();
        }
        return "";
    }
}
