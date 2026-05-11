package com.example.traveling.ui.search;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.traveling.R;
import com.example.traveling.data.FilterRegistry;
import com.example.traveling.data.SearchFilters;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.Slider;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class SearchFilterFragment extends Fragment {

    private EditText searchInput;
    private EditText authorInput;
    private TextView etFrom;
    private TextView etTo;
    private ChipGroup chipGroupTypes;
    private Slider slider;

    private long dateFromMs = -1;
    private long dateToMs   = -1;

    private final ActivityResultLauncher<Intent> voiceLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK
                                && result.getData() != null) {
                            ArrayList<String> matches = result.getData()
                                    .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                            if (matches != null && !matches.isEmpty()) {
                                searchInput.setText(matches.get(0));
                                searchInput.setSelection(searchInput.getText().length());
                            }
                        }
                    });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search_filter, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        searchInput = view.findViewById(R.id.filter_search_input);
        authorInput = view.findViewById(R.id.author_input);
        etFrom = view.findViewById(R.id.period_from);
        etTo = view.findViewById(R.id.period_to);
        chipGroupTypes = view.findViewById(R.id.chip_group_types);
        ImageView btnVoice = view.findViewById(R.id.btn_voice_search);
        slider = view.findViewById(R.id.slider_radius);
        android.widget.TextView radiusValue = view.findViewById(R.id.radius_value);
        MaterialButton apply = view.findViewById(R.id.btn_apply_filters);
        MaterialButton reset = view.findViewById(R.id.btn_reset_filters);

        // Restaurer l'état des filtres précédents
        restoreFilters(FilterRegistry.get());

        // Sélecteurs de date
        etFrom.setOnClickListener(v -> showDatePicker(true));
        etTo.setOnClickListener(v -> showDatePicker(false));

        // Recherche vocale
        btnVoice.setOnClickListener(v -> launchVoiceSearch());

        // Slider rayon
        slider.addOnChangeListener((s, value, fromUser) ->
                radiusValue.setText((int) value + " km"));

        // Appliquer filtres
        apply.setOnClickListener(v -> {
            FilterRegistry.set(buildFilters());
            Navigation.findNavController(v).navigateUp();
        });

        // Réinitialiser filtres
        reset.setOnClickListener(v -> {
            searchInput.setText("");
            authorInput.setText("");
            etFrom.setText("");
            etTo.setText("");
            dateFromMs = -1;
            dateToMs = -1;
            slider.setValue(20);
            for (int i = 0; i < chipGroupTypes.getChildCount(); i++) {
                View child = chipGroupTypes.getChildAt(i);
                if (child instanceof Chip) ((Chip) child).setChecked(false);
            }
            FilterRegistry.set(new SearchFilters());
            Navigation.findNavController(v).navigateUp();
        });
    }

    private void showDatePicker(boolean isFrom) {
        Calendar initial = Calendar.getInstance();
        long existing = isFrom ? dateFromMs : dateToMs;
        if (existing > 0) initial.setTimeInMillis(existing);

        new DatePickerDialog(requireContext(), (dp, year, month, day) -> {
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, day, isFrom ? 0 : 23, isFrom ? 0 : 59, isFrom ? 0 : 59);
            cal.set(Calendar.MILLISECOND, 0);

            if (isFrom) {
                dateFromMs = cal.getTimeInMillis();
                etFrom.setText(String.format(Locale.getDefault(), "%02d/%02d/%04d", day, month + 1, year));
            } else {
                dateToMs = cal.getTimeInMillis();
                etTo.setText(String.format(Locale.getDefault(), "%02d/%02d/%04d", day, month + 1, year));
            }
        }, initial.get(Calendar.YEAR), initial.get(Calendar.MONTH),
                initial.get(Calendar.DAY_OF_MONTH)).show();
    }

    private SearchFilters buildFilters() {
        SearchFilters filters = new SearchFilters();
        filters.query = searchInput.getText().toString().trim();
        filters.authorName = authorInput.getText().toString().trim();
        filters.dateFromMs = dateFromMs;
        filters.dateToMs = dateToMs;

        for (int i = 0; i < chipGroupTypes.getChildCount(); i++) {
            View child = chipGroupTypes.getChildAt(i);
            if (child instanceof Chip && ((Chip) child).isChecked()) {
                filters.locationTypes.add(((Chip) child).getText().toString());
            }
        }
        return filters;
    }

    private void restoreFilters(SearchFilters filters) {
        searchInput.setText(filters.query);
        authorInput.setText(filters.authorName);
        dateFromMs = filters.dateFromMs;
        dateToMs = filters.dateToMs;

        if (filters.dateFromMs > 0) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(filters.dateFromMs);
            etFrom.setText(String.format(Locale.getDefault(), "%02d/%02d/%04d",
                    c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR)));
        }
        if (filters.dateToMs > 0) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(filters.dateToMs);
            etTo.setText(String.format(Locale.getDefault(), "%02d/%02d/%04d",
                    c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR)));
        }

        for (int i = 0; i < chipGroupTypes.getChildCount(); i++) {
            View child = chipGroupTypes.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                chip.setChecked(filters.locationTypes.contains(chip.getText().toString()));
            }
        }
    }

    private void launchVoiceSearch() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Rechercher un lieu ou un parcours...");

        try {
            voiceLauncher.launch(intent);
        } catch (Exception e) {
            android.widget.Toast.makeText(requireContext(),
                    "Reconnaissance vocale non disponible",
                    android.widget.Toast.LENGTH_SHORT).show();
        }
    }
}
