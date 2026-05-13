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
import com.example.traveling.data.GeocodingUtils;
import com.example.traveling.data.PathRepository;
import com.example.traveling.data.UserRepository;
import com.example.traveling.model.PathStep;
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

    private final List<PathStep> steps = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ArrayAdapter<String> cityAdapter;
    private Runnable pendingSearch;
    private double geocodedLat = 0;
    private double geocodedLon = 0;

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
        cityAdapter = buildNoFilterAdapter();
        etCity.setAdapter(cityAdapter);
        etCity.setThreshold(2);

        etCity.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (pendingSearch != null) mainHandler.removeCallbacks(pendingSearch);
                String query = s.toString().trim();
                if (query.length() < 2) return;
                pendingSearch = () -> searchCities(query, etCity, cityAdapter);
                mainHandler.postDelayed(pendingSearch, 350);
            }
        });
    }

    private ArrayAdapter<String> buildNoFilterAdapter() {
        return new ArrayAdapter<String>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, new ArrayList<>()) {
            @Override
            public android.widget.Filter getFilter() {
                return new android.widget.Filter() {
                    @Override
                    protected android.widget.Filter.FilterResults performFiltering(CharSequence constraint) {
                        android.widget.Filter.FilterResults r = new android.widget.Filter.FilterResults();
                        List<String> all = new ArrayList<>();
                        for (int i = 0; i < getCount(); i++) all.add(getItem(i));
                        r.values = all;
                        r.count = all.size();
                        return r;
                    }
                    @Override
                    protected void publishResults(CharSequence constraint, android.widget.Filter.FilterResults results) {
                        notifyDataSetChanged();
                    }
                };
            }
        };
    }

    private void searchCities(String query, AutoCompleteTextView field, ArrayAdapter<String> adapter) {
        executor.execute(() -> {
            try {
                String encoded = URLEncoder.encode(query, "UTF-8");
                String urlStr = "https://nominatim.openstreetmap.org/search?q=" + encoded
                        + "&format=json&addressdetails=1&limit=5&accept-language=fr";

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "TravelingApp/1.0");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                if (conn.getResponseCode() != 200) return;

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONArray results = new JSONArray(sb.toString());
                List<String> cities = new ArrayList<>();
                for (int i = 0; i < results.length(); i++) {
                    JSONObject item = results.getJSONObject(i);
                    String displayName = item.getString("display_name");
                    String[] parts = displayName.split(",");
                    String cityName = parts[0].trim();
                    if (parts.length > 1) {
                        cityName = cityName + ", " + parts[parts.length - 1].trim();
                    }
                    if (!cities.contains(cityName)) cities.add(cityName);
                }

                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    adapter.clear();
                    adapter.addAll(cities);
                    adapter.notifyDataSetChanged();
                    if (!cities.isEmpty() && field.hasFocus()) field.showDropDown();
                });
            } catch (Exception e) {
                Log.e("CreatePath", "City search failed", e);
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
        int radius = getRadiusForEffort(effort, dureeMax);
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

                // ---- ÉTAPE 3 : Récupérer les POI (OpenTripMap) en quadrillant la zone ----
                String apiKey = "5ae2e3f221c38a28845f05b64ceacf3f82755ce118bd16981c7984e8";
                double centerLat = Double.parseDouble(lat);
                double centerLon = Double.parseDouble(lon);

                double offsetDeg = radius / 111000.0;
                double[][] searchPoints = {
                        {centerLat, centerLon},
                        {centerLat + offsetDeg, centerLon},
                        {centerLat - offsetDeg, centerLon},
                        {centerLat, centerLon + offsetDeg},
                        {centerLat, centerLon - offsetDeg},
                        {centerLat + offsetDeg / 2, centerLon + offsetDeg / 2},
                        {centerLat + offsetDeg / 2, centerLon - offsetDeg / 2},
                        {centerLat - offsetDeg / 2, centerLon + offsetDeg / 2},
                        {centerLat - offsetDeg / 2, centerLon - offsetDeg / 2},
                };

                int subRadius = radius / 3;
                JSONArray poiResults = new JSONArray();

                for (double[] pt : searchPoints) {
                    try {
                        String poiUrl = "https://api.opentripmap.com/0.1/en/places/radius"
                                + "?radius=" + subRadius
                                + "&lon=" + pt[1]
                                + "&lat=" + pt[0]
                                + "&kinds=" + kinds
                                + "&limit=20"
                                + "&format=json"
                                + "&apikey=" + apiKey;

                        URL poiUrlObj = new URL(poiUrl);
                        HttpURLConnection poiConn = (HttpURLConnection) poiUrlObj.openConnection();
                        poiConn.setRequestProperty("User-Agent", "TravelingApp/1.0");
                        poiConn.setConnectTimeout(10000);
                        poiConn.setReadTimeout(10000);

                        if (poiConn.getResponseCode() == 200) {
                            BufferedReader poiReader = new BufferedReader(
                                    new InputStreamReader(poiConn.getInputStream()));
                            StringBuilder poiSb = new StringBuilder();
                            while ((line = poiReader.readLine()) != null) poiSb.append(line);
                            poiReader.close();

                            JSONArray partial = new JSONArray(poiSb.toString());
                            for (int i = 0; i < partial.length(); i++) {
                                poiResults.put(partial.getJSONObject(i));
                            }
                        }
                    } catch (Exception ignored) {}
                }
                Log.d("CreatePath", "Total POI récupérés: " + poiResults.length());

                // ---- ÉTAPE 4 : Filtrer selon météo et budget ----
                List<JSONObject> filteredPOIs = filterPOIs(
                        poiResults, temperature, weatherCode,
                        sensibleChaleur, sensiblePluie, budget
                );

                // ---- ÉTAPE 5 : Sélectionner les étapes les plus éloignées entre elles ----
                Log.d("CreatePath", "Filtered POIs: " + filteredPOIs.size()
                        + ", radius: " + radius + "m, maxSteps: " + maxSteps);

                List<PathStep> generatedSteps = new ArrayList<>();

                // Ajouter le POI le mieux noté comme point de départ
                for (int idx = 0; idx < filteredPOIs.size(); idx++) {
                    JSONObject first = filteredPOIs.get(idx);
                    if (!first.has("point") || !first.has("name") || first.getString("name").isEmpty())
                        continue;
                    JSONObject fp = first.getJSONObject("point");
                    generatedSteps.add(new PathStep(first.getString("name"), "",
                            fp.optDouble("lat", 0), fp.optDouble("lon", 0), null, null));
                    filteredPOIs.remove(idx);
                    break;
                }

                // Ajouter à chaque fois le POI le plus éloigné de tous ceux déjà sélectionnés
                while (generatedSteps.size() < maxSteps && !filteredPOIs.isEmpty()) {
                    double bestMinDist = -1;
                    int bestIdx = -1;
                    double bestLat = 0, bestLon = 0;
                    String bestName = "";

                    for (int i = 0; i < filteredPOIs.size(); i++) {
                        JSONObject poi = filteredPOIs.get(i);
                        if (!poi.has("point") || !poi.has("name") || poi.getString("name").isEmpty())
                            continue;
                        JSONObject pt = poi.getJSONObject("point");
                        double pLat = pt.optDouble("lat", 0);
                        double pLon = pt.optDouble("lon", 0);
                        String pName = poi.getString("name");

                        boolean sameName = false;
                        for (PathStep s : generatedSteps) {
                            if (s.getName().equals(pName)) { sameName = true; break; }
                        }
                        if (sameName) continue;

                        double minDist = Double.MAX_VALUE;
                        for (PathStep s : generatedSteps) {
                            double d = distanceKm(s.getLatitude(), s.getLongitude(), pLat, pLon);
                            if (d < minDist) minDist = d;
                        }

                        if (minDist > bestMinDist) {
                            bestMinDist = minDist;
                            bestIdx = i;
                            bestLat = pLat;
                            bestLon = pLon;
                            bestName = pName;
                        }
                    }

                    if (bestIdx == -1) break;

                    Log.d("CreatePath", "AJOUTÉ: " + bestName + " (dist=" + String.format("%.3f", bestMinDist) + "km)");
                    generatedSteps.add(new PathStep(bestName, "", bestLat, bestLon, null, null));
                    filteredPOIs.remove(bestIdx);
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
                    for (PathStep step : generatedSteps) {
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
                        PathStep step = new PathStep(stepName, "", 0, 0, null, null);
                        steps.add(step);
                        addStepView(step, steps.size());
                    }
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void addStepView(PathStep step, int index) {
        View stepView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_step, stepsContainer, false);

        TextView tvStepNumber = stepView.findViewById(R.id.tv_step_number);
        TextView tvStepName = stepView.findViewById(R.id.tv_step_name);

        tvStepNumber.setText(String.valueOf(index));
        tvStepName.setText(step.getName());

        stepView.findViewById(R.id.btn_remove_step).setOnClickListener(v -> {
            steps.remove(step);
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

        TravelPath path = new TravelPath(null, title, city, description,
                "Moi", geocodedLat, geocodedLon,
                duration,
                budgetStr,
                difficulty.isEmpty() ? "-" : difficulty,
                "équilibré", steps.size(), 0, R.drawable.sample_path_1);
        path.setPublic(isPublic);
        path.setSteps(new ArrayList<>(steps));

        View btnPublish = requireView().findViewById(R.id.btn_publish);
        btnPublish.setEnabled(false);

        TravelPath finalPath = path;
        GeocodingUtils.geocode(city, new GeocodingUtils.GeocodingCallback() {
            @Override
            public void onResult(double latitude, double longitude) {
                finalPath.setStartLatitude(latitude);
                finalPath.setStartLongitude(longitude);
                savePath(finalPath, btnPublish);
            }
            @Override
            public void onFailure() {
                savePath(finalPath, btnPublish);
            }
        });
    }

    private void savePath(TravelPath path, View btnPublish) {
        Log.d("CreatePath", "Saving path to Firestore...");
        PathRepository.get().savePath(path)
                .addOnSuccessListener(docRef -> {
                    Log.d("CreatePath", "Path saved: " + docRef.getId());
                    if (!isAdded()) return;
                    path.setId(docRef.getId());
                    UserRepository.get().addPath(path);
                    Toast.makeText(requireContext(), "Parcours publié !", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(requireView()).navigateUp();
                })
                .addOnFailureListener(e -> {
                    Log.e("CreatePath", "Save failed", e);
                    if (!isAdded()) return;
                    btnPublish.setEnabled(true);
                    Toast.makeText(requireContext(),
                            "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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

    private int getRadiusForEffort(String effort, int dureeHeures) {
        int baseRadius;
        switch (effort.toLowerCase()) {
            case "facile":    baseRadius = 2000; break;
            case "difficile": baseRadius = 5000; break;
            default:          baseRadius = 3000; break;
        }
        return Math.min(25000, baseRadius * dureeHeures);
    }

    private double getMinDistanceBetweenSteps(int radiusMeters) {
        double radiusKm = radiusMeters / 1000.0;
        return Math.max(0.1, radiusKm / 15.0);
    }

    private double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
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
