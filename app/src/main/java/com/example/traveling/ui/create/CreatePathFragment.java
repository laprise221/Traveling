package com.example.traveling.ui.create;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.util.Log;

public class CreatePathFragment extends Fragment {

    private TextInputEditText etTitle;
    private AutoCompleteTextView etCity;
    private TextInputEditText etDescription;
    private ChipGroup chipGroupDuration;
    private ChipGroup chipGroupBudget;
    private ChipGroup chipGroupDifficulty;
    private LinearLayout stepsContainer;
    private SwitchMaterial switchPublic;

    private final List<String> steps = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ArrayAdapter<String> cityAdapter;
    private Runnable pendingSearch;

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

        setupCityAutocomplete();

        view.findViewById(R.id.btn_back).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());

        view.findViewById(R.id.btn_publish).setOnClickListener(v -> publish());

        view.findViewById(R.id.btn_add_step).setOnClickListener(v -> showAddStepDialog());

        view.findViewById(R.id.btn_generate).setOnClickListener(v -> generatePath());
    }

    private void setupCityAutocomplete() {
        cityAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        etCity.setAdapter(cityAdapter);

        etCity.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (pendingSearch != null) {
                    mainHandler.removeCallbacks(pendingSearch);
                }
                String query = s.toString().trim();
                if (query.length() < 2) return;

                pendingSearch = () -> searchCities(query);
                mainHandler.postDelayed(pendingSearch, 300);
            }
        });
    }

    private void searchCities(String query) {
        executor.execute(() -> {
            try {
                String encoded = URLEncoder.encode(query, "UTF-8");
                String urlStr = "https://nominatim.openstreetmap.org/search?q=" + encoded
                        + "&format=json&addressdetails=1&limit=5"
                        + "&featuretype=city&accept-language=fr";

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "TravelingApp/1.0");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                JSONArray results = new JSONArray(sb.toString());
                List<String> cities = new ArrayList<>();

                for (int i = 0; i < results.length(); i++) {
                    JSONObject item = results.getJSONObject(i);
                    String displayName = item.getString("display_name");
                    String[] parts = displayName.split(",");
                    String cityName = parts[0].trim();
                    if (parts.length > 1) {
                        String country = parts[parts.length - 1].trim();
                        cityName = cityName + ", " + country;
                    }
                    if (!cities.contains(cityName)) {
                        cities.add(cityName);
                    }
                }

                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    cityAdapter.clear();
                    cityAdapter.addAll(cities);
                    cityAdapter.notifyDataSetChanged();
                    if (!cities.isEmpty() && etCity.hasFocus()) {
                        etCity.showDropDown();
                    }
                });

            } catch (Exception e) {
                // Silently ignore network errors
            }
        });
    }

    private void generatePath() {
        String city = etCity.getText() != null ? etCity.getText().toString().trim() : "";
        if (city.isEmpty()) {
            etCity.setError("Entrez une ville d'abord");
            etCity.requestFocus();
            return;
        }

        View btnGenerate = requireView().findViewById(R.id.btn_generate);
        btnGenerate.setEnabled(false);
        Toast.makeText(requireContext(), "Génération en cours...", Toast.LENGTH_SHORT).show();

        executor.execute(() -> {
            try {
                String encoded = URLEncoder.encode(city, "UTF-8");
                String urlStr = "https://nominatim.openstreetmap.org/search?q=" + encoded
                        + "&format=json&limit=1";

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "TravelingApp/1.0");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONArray cityResults = new JSONArray(sb.toString());
                if (cityResults.length() == 0) {
                    mainHandler.post(() -> {
                        if (!isAdded()) return;
                        btnGenerate.setEnabled(true);
                        Toast.makeText(requireContext(), "Ville introuvable", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                JSONObject cityObj = cityResults.getJSONObject(0);
                String lat = cityObj.getString("lat");
                String lon = cityObj.getString("lon");

                String apiKey = "5ae2e3f221c38a28845f05b64ceacf3f82755ce118bd16981c7984e8";
                String poiUrl = "https://api.opentripmap.com/0.1/en/places/radius"
                        + "?radius=10000"
                        + "&lon=" + lon
                        + "&lat=" + lat
                        + "&kinds=interesting_places"
                        + "&limit=10"
                        + "&format=json"
                        + "&apikey=" + apiKey;

                URL poiUrlObj = new URL(poiUrl);
                HttpURLConnection poiConn = (HttpURLConnection) poiUrlObj.openConnection();
                poiConn.setRequestProperty("User-Agent", "TravelingApp/1.0");
                poiConn.setConnectTimeout(10000);
                poiConn.setReadTimeout(10000);

                int responseCode = poiConn.getResponseCode();
                Log.d("CreatePath", "OpenTripMap response code: " + responseCode);
                Log.d("CreatePath", "OpenTripMap URL: " + poiUrl);

                if (responseCode != 200) {
                    BufferedReader errorReader = new BufferedReader(
                            new InputStreamReader(poiConn.getErrorStream()));
                    StringBuilder errorSb = new StringBuilder();
                    while ((line = errorReader.readLine()) != null) errorSb.append(line);
                    errorReader.close();
                    Log.e("CreatePath", "OpenTripMap error: " + errorSb.toString());
                    mainHandler.post(() -> {
                        if (!isAdded()) return;
                        btnGenerate.setEnabled(true);
                        Toast.makeText(requireContext(),
                                "Erreur API: code " + responseCode, Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                BufferedReader poiReader = new BufferedReader(
                        new InputStreamReader(poiConn.getInputStream()));
                StringBuilder poiSb = new StringBuilder();
                while ((line = poiReader.readLine()) != null) poiSb.append(line);
                poiReader.close();

                JSONArray poiResults = new JSONArray(poiSb.toString());
                List<String> generatedSteps = new ArrayList<>();

                for (int i = 0; i < poiResults.length(); i++) {
                    JSONObject poi = poiResults.getJSONObject(i);
                    if (!poi.has("name") || poi.getString("name").isEmpty()) continue;
                    String stepName = poi.getString("name");
                    if (!generatedSteps.contains(stepName)) {
                        generatedSteps.add(stepName);
                    }
                }

                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    btnGenerate.setEnabled(true);

                    if (generatedSteps.isEmpty()) {
                        Toast.makeText(requireContext(),
                                "Aucun lieu trouvé pour cette ville", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    steps.clear();
                    stepsContainer.removeAllViews();
                    for (String step : generatedSteps) {
                        steps.add(step);
                        addStepView(step, steps.size());
                    }
                    Toast.makeText(requireContext(),
                            generatedSteps.size() + " étapes générées", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                Log.e("CreatePath", "Generate error", e);
                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    btnGenerate.setEnabled(true);
                    Toast.makeText(requireContext(),
                            "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (pendingSearch != null) {
            mainHandler.removeCallbacks(pendingSearch);
        }
        executor.shutdownNow();
    }
}
