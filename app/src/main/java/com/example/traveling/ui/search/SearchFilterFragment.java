package com.example.traveling.ui.search;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.traveling.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;

import java.util.ArrayList;
import java.util.Locale;

public class SearchFilterFragment extends Fragment {

    private EditText searchInput;

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
        ImageView btnVoice = view.findViewById(R.id.btn_voice_search);
        Slider slider = view.findViewById(R.id.slider_radius);
        TextView radiusValue = view.findViewById(R.id.radius_value);
        MaterialButton apply = view.findViewById(R.id.btn_apply_filters);

        // Recherche vocale
        btnVoice.setOnClickListener(v -> launchVoiceSearch());

        // Slider rayon
        slider.addOnChangeListener((s, value, fromUser) ->
                radiusValue.setText((int) value + " km"));

        // Appliquer filtres
        apply.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Filtres appliqués", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(v).navigateUp();
        });
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
            Toast.makeText(requireContext(),
                    "Reconnaissance vocale non disponible", Toast.LENGTH_SHORT).show();
        }
    }
}
