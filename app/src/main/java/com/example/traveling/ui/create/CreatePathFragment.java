package com.example.traveling.ui.create;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
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
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.widget.CheckBox;
import com.google.android.material.chip.Chip;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
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
    private Slider sliderBudget;
    private TextView tvBudgetValue;
    private Slider sliderDuree;
    private TextView tvDureeValue;
    private ChipGroup chipGroupDifficulty;
    private ChipGroup chipGroupActivities;
    private CheckBox cbChaleur;
    private CheckBox cbPluie;
    private TextView tvMeteo;
    private TextView tvResume;
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
        chipGroupDifficulty = view.findViewById(R.id.chip_group_difficulty);
        chipGroupActivities = view.findViewById(R.id.chip_group_activities);
        cbChaleur = view.findViewById(R.id.cb_chaleur);
        cbPluie = view.findViewById(R.id.cb_pluie);
        tvMeteo = view.findViewById(R.id.tv_meteo);
        tvResume = view.findViewById(R.id.tv_resume);
        stepsContainer = view.findViewById(R.id.steps_container);
        switchPublic = view.findViewById(R.id.switch_public);
        sliderBudget = view.findViewById(R.id.slider_budget);
        tvBudgetValue = view.findViewById(R.id.tv_budget_value);
        sliderDuree = view.findViewById(R.id.slider_duree);
        tvDureeValue = view.findViewById(R.id.tv_duree_value);

        sliderBudget.addOnChangeListener((slider, value, fromUser) -> {
            tvBudgetValue.setText((int) value + " €");
        });

        sliderDuree.addOnChangeListener((slider, value, fromUser) -> {
            tvDureeValue.setText((int) value + " h");
        });
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

        // Récupérer les préférences
        int dureeMax = (int) sliderDuree.getValue();
        int budget = (int) sliderBudget.getValue();
        String effortRaw = getSelectedChipText(chipGroupDifficulty);
        String effort = effortRaw.isEmpty() ? "modéré" : effortRaw;
        List<String> activites = getSelectedActivities();
        boolean sensibleChaleur = cbChaleur.isChecked();
        boolean sensiblePluie = cbPluie.isChecked();

        // Calculer les paramètres dérivés
        int radius = getRadiusForEffort(effort);
        int maxSteps = getMaxSteps(dureeMax, effort);
        String kinds = mapActivitesToKinds(activites);

        View btnGenerate = requireView().findViewById(R.id.btn_generate);
        btnGenerate.setEnabled(false);
        Toast.makeText(requireContext(), "Génération en cours...", Toast.LENGTH_SHORT).show();

        executor.execute(() -> {
            try {
                // ---- ÉTAPE 1 : Géocoder la ville (Nominatim) ----
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

                // ---- ÉTAPE 2 : Récupérer la météo (Open-Meteo) ----
                String meteoUrl = "https://api.open-meteo.com/v1/forecast"
                        + "?latitude=" + lat
                        + "&longitude=" + lon
                        + "&current_weather=true";

                URL meteoUrlObj = new URL(meteoUrl);
                HttpURLConnection meteoConn = (HttpURLConnection) meteoUrlObj.openConnection();
                meteoConn.setRequestProperty("User-Agent", "TravelingApp/1.0");

                BufferedReader meteoReader = new BufferedReader(
                        new InputStreamReader(meteoConn.getInputStream()));
                StringBuilder meteoSb = new StringBuilder();
                while ((line = meteoReader.readLine()) != null) meteoSb.append(line);
                meteoReader.close();

                JSONObject meteoData = new JSONObject(meteoSb.toString())
                        .getJSONObject("current_weather");
                double temperature = meteoData.getDouble("temperature");
                int weatherCode = meteoData.getInt("weathercode");

                // ---- ÉTAPE 3 : Récupérer les POI (OpenTripMap) ----
                String apiKey = "5ae2e3f221c38a28845f05b64ceacf3f82755ce118bd16981c7984e8";
                String poiUrl = "https://api.opentripmap.com/0.1/en/places/radius"
                        + "?radius=" + radius           // adapté à l'effort
                        + "&lon=" + lon
                        + "&lat=" + lat
                        + "&kinds=" + kinds              // adapté aux activités choisies
                        + "&limit=" + (maxSteps * 3)     // prendre plus pour filtrer ensuite
                        + "&format=json"
                        + "&apikey=" + apiKey;

                URL poiUrlObj = new URL(poiUrl);
                HttpURLConnection poiConn = (HttpURLConnection) poiUrlObj.openConnection();
                poiConn.setRequestProperty("User-Agent", "TravelingApp/1.0");
                poiConn.setConnectTimeout(10000);
                poiConn.setReadTimeout(10000);

                int responseCode = poiConn.getResponseCode();
                if (responseCode != 200) {
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

                // ---- ÉTAPE 4 : Filtrer selon météo et budget ----
                List<JSONObject> filteredPOIs = filterPOIs(
                        poiResults, temperature, weatherCode,
                        sensibleChaleur, sensiblePluie, budget
                );

                // ---- ÉTAPE 5 : Limiter au nombre d'étapes selon la durée ----
                List<String> generatedSteps = new ArrayList<>();
                int count = 0;
                for (JSONObject poi : filteredPOIs) {
                    if (count >= maxSteps) break;
                    String stepName = poi.getString("name");
                    if (!generatedSteps.contains(stepName)) {
                        generatedSteps.add(stepName);
                        count++;
                    }
                }

                // ---- ÉTAPE 6 : Construire le résumé ----
                String meteoInfo;
                if (weatherCode <= 3) meteoInfo = "☀ " + temperature + "°C";
                else if (weatherCode <= 50) meteoInfo = "☁ " + temperature + "°C";
                else if (weatherCode <= 67) meteoInfo = "🌧 " + temperature + "°C";
                else meteoInfo = "❄ " + temperature + "°C";

                String finalMeteoInfo = meteoInfo;

                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    btnGenerate.setEnabled(true);

                    if (generatedSteps.isEmpty()) {
                        Toast.makeText(requireContext(),
                                "Aucun lieu trouvé avec ces critères", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Afficher la météo
                    tvMeteo.setText("Météo : " + finalMeteoInfo);
                    tvMeteo.setVisibility(View.VISIBLE);

                    // Afficher les résumés
                    tvResume.setText(generatedSteps.size() + " étapes · ~"
                            + dureeMax + "h · " + budget + "€ max · " + effort);
                    tvResume.setVisibility(View.VISIBLE);

                    // Afficher les étapes
                    steps.clear();
                    stepsContainer.removeAllViews();
                    for (String step : generatedSteps) {
                        steps.add(step);
                        addStepView(step, steps.size());
                    }

                    Toast.makeText(requireContext(),
                            generatedSteps.size() + " étapes générées " + finalMeteoInfo,
                            Toast.LENGTH_SHORT).show();
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

        String duration = (int) sliderDuree.getValue() + "h";
        String budgetStr = (int) sliderBudget.getValue() + "€";
        String difficulty = getSelectedChipText(chipGroupDifficulty);
        boolean isPublic = switchPublic.isChecked();

        String id = "path_" + System.currentTimeMillis();
        TravelPath path = new TravelPath(id, title, city, description,
                "Moi", 0, 0,
                duration,
                budgetStr,
                difficulty.isEmpty() ? "-" : difficulty,
                "équilibré", steps.size(), 0, R.drawable.sample_path_1);
        UserRepository.get().addPath(path);

        Toast.makeText(requireContext(), "Parcours publié !", Toast.LENGTH_SHORT).show();
        Navigation.findNavController(requireView()).navigateUp();
    }

    private List<String> getSelectedActivities() {
        List<String> selected = new ArrayList<>();
        for (int id : chipGroupActivities.getCheckedChipIds()) {
            Chip chip = chipGroupActivities.findViewById(id);
            if (chip != null) {
                selected.add(chip.getText().toString());
            }
        }
        return selected;
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

    private List<JSONObject> filterPOIs(JSONArray poiResults,
                                        double temperature, int weatherCode,
                                        boolean sensibleChaleur, boolean sensiblePluie,
                                        int budget) throws JSONException {

        boolean ilPleut = weatherCode >= 51 && weatherCode <= 67;
        boolean ilFaitChaud = temperature > 30;
        boolean ilFaitFroid = temperature < 5;

        List<JSONObject> filtered = new ArrayList<>();

        for (int i = 0; i < poiResults.length(); i++) {
            JSONObject poi = poiResults.getJSONObject(i);
            if (!poi.has("name") || poi.getString("name").isEmpty()) continue;

            String kinds = poi.optString("kinds", "");

            // Si sensible à la pluie et il pleut → privilégier les lieux couverts
            if (sensiblePluie && ilPleut) {
                boolean estCouvert = kinds.contains("museums")
                        || kinds.contains("theatres")
                        || kinds.contains("shops")
                        || kinds.contains("restaurants")
                        || kinds.contains("cafes");
                if (!estCouvert) continue;  // exclure les lieux extérieurs
            }

            // Si sensible à la chaleur et il fait très chaud → éviter les lieux extérieurs
            if (sensibleChaleur && ilFaitChaud) {
                boolean estExterieur = kinds.contains("beaches")
                        || kinds.contains("view_points")
                        || kinds.contains("gardens");
                if (estExterieur) continue;
            }

            // Filtrer par budget via le rate (OpenTripMap donne un score 1-3)
            int rate = poi.optInt("rate", 0);
            if (budget < 50 && rate >= 3) continue;  // lieux chers exclus si petit budget

            filtered.add(poi);
        }

        return filtered;
    }

    private int getMaxSteps(int dureeHeures, String effort) {
        int minutesParEtape;
        switch (effort.toLowerCase()) {
            case "facile":    minutesParEtape = 90; break;
            case "difficile": minutesParEtape = 40; break;
            default:          minutesParEtape = 60; break;
        }
        return Math.max(2, (dureeHeures * 60) / minutesParEtape);
    }

    private int getRadiusForEffort(String effort) {
        switch (effort.toLowerCase()) {
            case "facile":    return 3000;
            case "modéré":    return 7000;
            case "difficile": return 15000;
            default:          return 5000;
        }
    }

    private String mapActivitesToKinds(List<String> activites) {
        List<String> kinds = new ArrayList<>();

        for (String activite : activites) {
            switch (activite.toLowerCase()) {
                case "culture":
                    kinds.add("museums");
                    kinds.add("theatres_and_entertainments");
                    kinds.add("historic");
                    kinds.add("architecture");
                    break;
                case "restauration":
                    kinds.add("foods");
                    kinds.add("restaurants");
                    kinds.add("cafes");
                    break;
                case "nature":
                    kinds.add("natural");
                    kinds.add("gardens_and_parks");
                    kinds.add("beaches");
                    break;
                case "loisirs":
                    kinds.add("amusements");
                    kinds.add("sport");
                    kinds.add("shops");
                    break;
                case "découverte":
                    kinds.add("interesting_places");
                    kinds.add("view_points");
                    kinds.add("monuments_and_memorials");
                    break;
            }
        }

        if (kinds.isEmpty()) kinds.add("interesting_places");
        return TextUtils.join(",", kinds);
    }

}
