package com.example.traveling.ui.create;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import android.Manifest;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.traveling.R;
import com.example.traveling.worker.SchedulePublishHelper;
import com.example.traveling.data.GeocodingUtils;
import com.example.traveling.data.ImageUtils;
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
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
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
    private ChipGroup chipGroupMeteo;
    private CheckBox cbChaleur;
    private CheckBox cbPluie;
    private TextView tvMeteo;
    private TextView tvResume;
    private View cardMeteoResume;
    private TextView tvOptionsTitle;
    private LinearLayout optionsContainer;
    private LinearLayout stepsContainer;
    private SwitchMaterial switchPublic;
    private PathOption selectedOption;

    private final List<PathStep> steps = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ArrayAdapter<String> cityAdapter;
    private Runnable pendingSearch;
    private double geocodedLat = 0;
    private double geocodedLon = 0;

    private ActivityResultLauncher<String> stepPhotoLauncher;
    private String pendingImageBase64 = null;
    private ImageView dialogPhotoPreview = null;

    private ActivityResultLauncher<String> coverGalleryLauncher;
    private ActivityResultLauncher<Uri> coverCameraLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private Uri coverCameraUri;
    private String coverImageBase64 = null;
    private ImageView imgCover;
    private View coverPlaceholder;
    private View btnChangeCover;

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
        chipGroupMeteo = view.findViewById(R.id.chip_group_meteo);
        tvMeteo = view.findViewById(R.id.tv_meteo);
        tvResume = view.findViewById(R.id.tv_resume);
        cardMeteoResume = view.findViewById(R.id.card_meteo_resume);
        tvOptionsTitle = view.findViewById(R.id.tv_options_title);

        // Synchroniser les chips météo avec les checkboxes (utilisés dans generatePath)
        ((Chip) view.findViewById(R.id.chip_chaleur))
                .setOnCheckedChangeListener((btn, checked) -> cbChaleur.setChecked(checked));
        ((Chip) view.findViewById(R.id.chip_pluie))
                .setOnCheckedChangeListener((btn, checked) -> cbPluie.setChecked(checked));
        optionsContainer = view.findViewById(R.id.options_container);
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

        view.findViewById(R.id.btn_publish).setOnClickListener(v -> showPublishOptionsDialog());

        view.findViewById(R.id.btn_add_step).setOnClickListener(v -> showAddStepDialog());

        view.findViewById(R.id.btn_generate).setOnClickListener(v -> generatePath());

        imgCover = view.findViewById(R.id.img_cover);
        coverPlaceholder = view.findViewById(R.id.cover_placeholder);
        btnChangeCover = view.findViewById(R.id.btn_change_cover);

        view.findViewById(R.id.card_cover).setOnClickListener(v -> showCoverPicker());
        btnChangeCover.setOnClickListener(v -> showCoverPicker());

        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) launchCamera();
                    else Toast.makeText(requireContext(),
                            "Permission caméra refusée", Toast.LENGTH_SHORT).show();
                });

        coverGalleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri == null) return;
                    executor.execute(() -> {
                        String b64 = ImageUtils.uriToBase64(requireContext(), uri);
                        mainHandler.post(() -> applyCoverImage(b64));
                    });
                });

        coverCameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (!success || coverCameraUri == null) return;
                    executor.execute(() -> {
                        String b64 = ImageUtils.uriToBase64(requireContext(), coverCameraUri);
                        mainHandler.post(() -> applyCoverImage(b64));
                    });
                });

        stepPhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri == null) return;
                    executor.execute(() -> {
                        String b64 = ImageUtils.uriToBase64(requireContext(), uri);
                        mainHandler.post(() -> {
                            if (!isAdded()) return;
                            pendingImageBase64 = b64;
                            if (dialogPhotoPreview != null && b64 != null) {
                                Bitmap bmp = ImageUtils.base64ToBitmap(b64);
                                if (bmp != null) {
                                    dialogPhotoPreview.setImageBitmap(bmp);
                                    dialogPhotoPreview.setVisibility(View.VISIBLE);
                                    View card = (View) dialogPhotoPreview.getParent();
                                    card.setVisibility(View.VISIBLE);
                                }
                            }
                        });
                    });
                });
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

                // ---- ÉTAPE 3 : Récupérer les POI (OpenTripMap) — appel unique centré sur la ville ----
                String apiKey = "5ae2e3f221c38a28845f05b64ceacf3f82755ce118bd16981c7984e8";
                double centerLat = Double.parseDouble(lat);
                double centerLon = Double.parseDouble(lon);

                JSONArray poiResults = new JSONArray();

                // Premier appel : rate=2 (lieux notables)
                for (int rateMin : new int[]{2, 1}) {
                    String poiUrl = "https://api.opentripmap.com/0.1/en/places/radius"
                            + "?radius=" + radius
                            + "&lon=" + centerLon
                            + "&lat=" + centerLat
                            + "&kinds=" + kinds
                            + "&rate=" + rateMin
                            + "&limit=100"
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
                        poiResults = new JSONArray(poiSb.toString());
                    }

                    // Si on a trouvé assez de lieux on s'arrête, sinon on descend à rate=1
                    if (poiResults.length() >= maxSteps + 3) break;
                }
                Log.d("CreatePath", "Total POI récupérés: " + poiResults.length());

                // ---- ÉTAPE 4 : Filtrer selon météo et budget ----
                List<JSONObject> filteredPOIs = filterPOIs(
                        poiResults, temperature, weatherCode,
                        sensibleChaleur, sensiblePluie, budget, maxSteps
                );

                // ---- ÉTAPE 5 : Générer 3 options (économique, équilibré, confort) ----
                Log.d("CreatePath", "Filtered POIs: " + filteredPOIs.size()
                        + ", radius: " + radius + "m, maxSteps: " + maxSteps);

                double maxRouteKm = maxWalkKm(effort, dureeMax);
                List<PathOption> generatedOptions = new ArrayList<>();
                // costPerStep = frais annexes par étape (boisson, transport local)
                // le coût d'entrée réel est estimé par estimatePOICost() selon la catégorie
                generatedOptions.add(buildOption(
                        "Économique", filteredPOIs,
                        Math.max(2, maxSteps - 1), true, false, 3, maxRouteKm));
                generatedOptions.add(buildOption(
                        "Équilibré", filteredPOIs,
                        maxSteps, false, false, 5, maxRouteKm));
                generatedOptions.add(buildOption(
                        "Confort", filteredPOIs,
                        maxSteps + 1, false, true, 10, maxRouteKm));

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

                    List<PathOption> nonEmpty = new ArrayList<>();
                    for (PathOption opt : generatedOptions) {
                        if (!opt.steps.isEmpty()) nonEmpty.add(opt);
                    }

                    if (nonEmpty.isEmpty()) {
                        Toast.makeText(requireContext(),
                                "Aucun lieu trouvé avec ces critères", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    tvMeteo.setText("Météo : " + finalMeteoInfo);
                    tvResume.setText("Critères : ~" + dureeMax + "h · " + budget + "€ max · " + effort);
                    cardMeteoResume.setVisibility(View.VISIBLE);

                    displayOptions(nonEmpty, dureeMax);

                    Toast.makeText(requireContext(),
                            nonEmpty.size() + " options proposées " + finalMeteoInfo,
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
    private void showCoverPicker() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Photo de couverture")
                .setItems(new String[]{"Galerie", "Appareil photo"}, (dialog, which) -> {
                    if (which == 0) {
                        coverGalleryLauncher.launch("image/*");
                    } else {
                        if (ContextCompat.checkSelfPermission(requireContext(),
                                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            launchCamera();
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
                        }
                    }
                })
                .show();
    }

    private void launchCamera() {
        try {
            File dir = requireContext().getExternalFilesDir(null);
            if (dir == null) dir = requireContext().getCacheDir();
            File tmp = File.createTempFile("cover_", ".jpg", dir);
            coverCameraUri = FileProvider.getUriForFile(requireContext(),
                    "com.example.traveling.fileprovider", tmp);
            coverCameraLauncher.launch(coverCameraUri);
        } catch (Exception e) {
            Log.e("CreatePath", "Camera launch failed", e);
            Toast.makeText(requireContext(),
                    "Erreur caméra : " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void applyCoverImage(String b64) {
        if (!isAdded() || b64 == null) return;
        coverImageBase64 = b64;
        Bitmap bmp = ImageUtils.base64ToBitmap(b64);
        if (bmp != null) {
            imgCover.setImageBitmap(bmp);
            imgCover.setVisibility(View.VISIBLE);
            coverPlaceholder.setVisibility(View.GONE);
            btnChangeCover.setVisibility(View.VISIBLE);
        }
    }

    private void showAddStepDialog() {
        pendingImageBase64 = null;

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_step, null);

        TextInputEditText etName = dialogView.findViewById(R.id.et_step_name);
        TextInputEditText etDesc = dialogView.findViewById(R.id.et_step_desc);
        dialogPhotoPreview = dialogView.findViewById(R.id.img_step_preview);

        dialogView.findViewById(R.id.btn_pick_photo).setOnClickListener(v ->
                stepPhotoLauncher.launch("image/*"));

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Ajouter une étape")
                .setView(dialogView)
                .setPositiveButton("Ajouter", (dialog, which) -> {
                    String stepName = etName.getText() != null ? etName.getText().toString().trim() : "";
                    if (stepName.isEmpty()) return;
                    String stepDesc = etDesc.getText() != null ? etDesc.getText().toString().trim() : "";
                    PathStep step = new PathStep(stepName, stepDesc, 0, 0, null, null);
                    step.setImageBase64(pendingImageBase64);
                    steps.add(step);
                    addStepView(step, steps.size());
                    pendingImageBase64 = null;
                    dialogPhotoPreview = null;
                })
                .setNegativeButton("Annuler", (dialog, which) -> {
                    pendingImageBase64 = null;
                    dialogPhotoPreview = null;
                })
                .show();
    }

    private void addStepView(PathStep step, int index) {
        View stepView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_step, stepsContainer, false);

        TextView tvStepNumber = stepView.findViewById(R.id.tv_step_number);
        TextView tvStepName = stepView.findViewById(R.id.tv_step_name);

        tvStepNumber.setText(String.valueOf(index));
        tvStepName.setText(step.getName());

        String desc = step.getDescription();
        if (desc != null && !desc.isEmpty()) {
            TextView tvDesc = stepView.findViewById(R.id.tv_step_desc);
            tvDesc.setText(desc);
            tvDesc.setVisibility(View.VISIBLE);
        }

        String b64 = step.getImageBase64();
        if (b64 != null && !b64.isEmpty()) {
            Bitmap bmp = ImageUtils.base64ToBitmap(b64);
            if (bmp != null) {
                ((ImageView) stepView.findViewById(R.id.img_step_photo)).setImageBitmap(bmp);
                stepView.findViewById(R.id.card_step_photo).setVisibility(View.VISIBLE);
                tvStepNumber.setVisibility(View.GONE);
            }
        }

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

    private void showPublishOptionsDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Publier")
                .setItems(new String[]{"Publier maintenant", "Planifier"}, (dialog, which) -> {
                    if (which == 0) publish(null);
                    else showScheduleDialog();
                })
                .show();
    }

    private void showScheduleDialog() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        android.app.DatePickerDialog dateDialog = new android.app.DatePickerDialog(
                requireContext(),
                (datePicker, year, month, day) -> {
                    android.app.TimePickerDialog timeDialog = new android.app.TimePickerDialog(
                            requireContext(),
                            (timePicker, hour, minute) -> {
                                java.util.Calendar scheduled = java.util.Calendar.getInstance();
                                scheduled.set(year, month, day, hour, minute, 0);
                                scheduled.set(java.util.Calendar.MILLISECOND, 0);
                                if (scheduled.getTimeInMillis() <= System.currentTimeMillis()) {
                                    android.widget.Toast.makeText(requireContext(),
                                            "La date doit être dans le futur",
                                            android.widget.Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                publish(scheduled.getTime());
                            },
                            cal.get(java.util.Calendar.HOUR_OF_DAY),
                            cal.get(java.util.Calendar.MINUTE),
                            true
                    );
                    timeDialog.show();
                },
                cal.get(java.util.Calendar.YEAR),
                cal.get(java.util.Calendar.MONTH),
                cal.get(java.util.Calendar.DAY_OF_MONTH)
        );
        dateDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        dateDialog.show();
    }

    private void publish(java.util.Date scheduledDate) {
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

        String pathType = selectedOption != null
                ? selectedOption.name.toLowerCase()
                : "équilibré";

        TravelPath path = new TravelPath(null, title, city, description,
                "Moi", geocodedLat, geocodedLon,
                duration,
                budgetStr,
                difficulty.isEmpty() ? "-" : difficulty,
                pathType, steps.size(), 0, R.drawable.sample_path_1);
        if (scheduledDate != null) {
            path.setVisibility("scheduled");
            path.setScheduledPublishDate(scheduledDate);
        } else {
            path.setPublic(isPublic);
        }
        path.setSteps(new ArrayList<>(steps));
        if (coverImageBase64 != null) path.setImageBase64(coverImageBase64);

        View btnPublish = requireView().findViewById(R.id.btn_publish);
        btnPublish.setEnabled(false);

        TravelPath finalPath = path;
        GeocodingUtils.geocode(city, new GeocodingUtils.GeocodingCallback() {
            @Override
            public void onResult(double latitude, double longitude) {
                finalPath.setStartLatitude(latitude);
                finalPath.setStartLongitude(longitude);
                savePath(finalPath, btnPublish, scheduledDate);
            }
            @Override
            public void onFailure() {
                savePath(finalPath, btnPublish, scheduledDate);
            }
        });
    }

    private void savePath(TravelPath path, View btnPublish, java.util.Date scheduledDate) {
        Log.d("CreatePath", "Saving path to Firestore...");
        PathRepository.get().savePath(path)
                .addOnSuccessListener(docRef -> {
                    Log.d("CreatePath", "Path saved: " + docRef.getId());
                    if (!isAdded()) return;
                    path.setId(docRef.getId());
                    UserRepository.get().addPath(path);
                    String msg;
                    if (scheduledDate != null) {
                        SchedulePublishHelper.schedule(requireContext(), docRef.getId(),
                                SchedulePublishHelper.COLLECTION_PATHS, scheduledDate);
                        String formatted = new java.text.SimpleDateFormat(
                                "dd MMM yyyy 'à' HH:mm", java.util.Locale.FRENCH)
                                .format(scheduledDate);
                        msg = "Publication planifiée pour le " + formatted;
                    } else {
                        msg = "Parcours publié !";
                    }
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
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
                                        int budget, int maxSteps) throws JSONException {

        boolean ilPleut = weatherCode >= 51 && weatherCode <= 67;
        boolean ilFaitChaud = temperature > 30;

        // --- Passe 1 : dédupliquer par xid (doublons exacts de la grille) ---
        java.util.LinkedHashMap<String, JSONObject> byXid = new java.util.LinkedHashMap<>();
        for (int i = 0; i < poiResults.length(); i++) {
            JSONObject poi = poiResults.getJSONObject(i);
            String xid = poi.optString("xid", "");
            if (xid.isEmpty()) xid = poi.optString("name", "") + i;
            if (!byXid.containsKey(xid)) byXid.put(xid, poi);
        }

        // --- Passe 2 : filtres météo / budget / nom ---
        List<JSONObject> filtered = new ArrayList<>();
        for (JSONObject poi : byXid.values()) {
            if (!poi.has("name") || poi.getString("name").isEmpty()) continue;

            String kinds = poi.optString("kinds", "");

            if (sensiblePluie && ilPleut) {
                boolean estCouvert = kinds.contains("museums")
                        || kinds.contains("theatres")
                        || kinds.contains("shops")
                        || kinds.contains("restaurants")
                        || kinds.contains("cafes");
                if (!estCouvert) continue;
            }

            if (sensibleChaleur && ilFaitChaud) {
                boolean estExterieur = kinds.contains("beaches")
                        || kinds.contains("view_points")
                        || kinds.contains("gardens");
                if (estExterieur) continue;
            }

            // Exclure les lieux dont le coût d'entrée dépasse le budget par étape
            int perStepBudget = budget / Math.max(2, maxSteps);
            if (estimatePOICost(poi) > perStepBudget) continue;

            filtered.add(poi);
        }

        // --- Passe 3 : dédupliquer par proximité (100 m) ---
        List<JSONObject> deduplicated = new ArrayList<>();
        for (JSONObject candidate : filtered) {
            if (!candidate.has("point")) { deduplicated.add(candidate); continue; }
            JSONObject cp = candidate.getJSONObject("point");
            double cLat = cp.optDouble("lat", 0);
            double cLon = cp.optDouble("lon", 0);
            int cRate = candidate.optInt("rate", 0);

            boolean tooClose = false;
            for (int k = 0; k < deduplicated.size(); k++) {
                JSONObject existing = deduplicated.get(k);
                if (!existing.has("point")) continue;
                JSONObject ep = existing.getJSONObject("point");
                double dist = distanceKm(cLat, cLon,
                        ep.optDouble("lat", 0), ep.optDouble("lon", 0)) * 1000;
                if (dist < 100) {
                    // Keep the better-rated one
                    if (cRate > existing.optInt("rate", 0)) {
                        deduplicated.set(k, candidate);
                    }
                    tooClose = true;
                    break;
                }
            }
            if (!tooClose) deduplicated.add(candidate);
        }

        return deduplicated;
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
        // Fraction du temps passée à marcher selon l'effort
        double walkFraction;
        switch (effort.toLowerCase()) {
            case "facile":    walkFraction = 0.30; break;
            case "difficile": walkFraction = 0.60; break;
            default:          walkFraction = 0.40; break;
        }
        // Rayon = moitié du budget de marche total (diamètre ≤ budget)
        double maxWalkKm = 5.0 * dureeHeures * walkFraction;
        return Math.max(500, (int) (maxWalkKm / 2.0 * 1000));
    }

    double maxWalkKm(String effort, int dureeHeures) {
        double walkFraction;
        switch (effort.toLowerCase()) {
            case "facile":    walkFraction = 0.30; break;
            case "difficile": walkFraction = 0.60; break;
            default:          walkFraction = 0.40; break;
        }
        return 5.0 * dureeHeures * walkFraction;
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

    private static class PathOption {
        final String name;
        final List<PathStep> steps;
        final double totalDistanceKm;
        final int estimatedBudget;

        PathOption(String name, List<PathStep> steps,
                   double totalDistanceKm, int estimatedBudget) {
            this.name = name;
            this.steps = steps;
            this.totalDistanceKm = totalDistanceKm;
            this.estimatedBudget = estimatedBudget;
        }
    }

    private PathOption buildOption(String name, List<JSONObject> pool, int maxSteps,
                                   boolean excludeExpensive, boolean preferClose,
                                   int overheadPerStep, double maxRouteKm) throws JSONException {
        List<JSONObject> pois = new ArrayList<>();
        for (JSONObject poi : pool) {
            // Pour "Économique", exclure les lieux payants (musées, restos)
            if (excludeExpensive && estimatePOICost(poi) >= 10) continue;
            pois.add(poi);
        }

        List<PathStep> picked = new ArrayList<>();
        java.util.Map<PathStep, Integer> admissionCosts = new java.util.IdentityHashMap<>();

        for (int idx = 0; idx < pois.size(); idx++) {
            JSONObject first = pois.get(idx);
            if (!first.has("point") || !first.has("name") || first.getString("name").isEmpty())
                continue;
            JSONObject fp = first.getJSONObject("point");
            PathStep firstStep = new PathStep(first.getString("name"), "",
                    fp.optDouble("lat", 0), fp.optDouble("lon", 0), null, null);
            firstStep.setXid(first.optString("xid", ""));
            admissionCosts.put(firstStep, estimatePOICost(first));
            picked.add(firstStep);
            pois.remove(idx);
            break;
        }

        while (picked.size() < maxSteps && !pois.isEmpty()) {
            double bestScore = preferClose ? Double.MAX_VALUE : -1;
            int bestIdx = -1;
            double bestLat = 0, bestLon = 0;
            String bestName = "";
            String bestXid = "";
            int bestCost = 0;

            for (int i = 0; i < pois.size(); i++) {
                JSONObject poi = pois.get(i);
                if (!poi.has("point") || !poi.has("name") || poi.getString("name").isEmpty())
                    continue;
                JSONObject pt = poi.getJSONObject("point");
                double pLat = pt.optDouble("lat", 0);
                double pLon = pt.optDouble("lon", 0);
                String pName = poi.getString("name");

                boolean sameName = false;
                for (PathStep s : picked) {
                    if (s.getName().equals(pName)) { sameName = true; break; }
                }
                if (sameName) continue;

                double minDist = Double.MAX_VALUE;
                for (PathStep s : picked) {
                    double d = distanceKm(s.getLatitude(), s.getLongitude(), pLat, pLon);
                    if (d < minDist) minDist = d;
                }

                boolean better = preferClose ? (minDist < bestScore) : (minDist > bestScore);
                if (better) {
                    bestScore = minDist;
                    bestIdx = i;
                    bestLat = pLat;
                    bestLon = pLon;
                    bestName = pName;
                    bestXid = poi.optString("xid", "");
                    bestCost = estimatePOICost(poi);
                }
            }

            if (bestIdx == -1) break;
            PathStep bestStep = new PathStep(bestName, "", bestLat, bestLon, null, null);
            bestStep.setXid(bestXid);
            admissionCosts.put(bestStep, bestCost);
            picked.add(bestStep);
            pois.remove(bestIdx);
        }

        // Reorder for shortest open path
        picked = optimalOrder(picked);

        // Écrêter si le total dépasse le budget de marche
        while (picked.size() > 2) {
            double total = 0;
            for (int i = 1; i < picked.size(); i++) {
                total += distanceKm(
                        picked.get(i - 1).getLatitude(), picked.get(i - 1).getLongitude(),
                        picked.get(i).getLatitude(), picked.get(i).getLongitude());
            }
            if (total <= maxRouteKm) break;
            picked.remove(picked.size() - 1);
        }

        double totalKm = 0;
        for (int i = 1; i < picked.size(); i++) {
            totalKm += distanceKm(
                    picked.get(i - 1).getLatitude(), picked.get(i - 1).getLongitude(),
                    picked.get(i).getLatitude(), picked.get(i).getLongitude());
        }

        // Budget = coût d'entrée réel par lieu + frais annexes (boissons, transport local)
        int estBudget = 0;
        for (PathStep s : picked) {
            estBudget += admissionCosts.getOrDefault(s, 0) + overheadPerStep;
        }
        return new PathOption(name, picked, totalKm, estBudget);
    }

    private int estimatePOICost(JSONObject poi) {
        String kinds = poi.optString("kinds", "");
        if (kinds.contains("restaurants") || kinds.contains("foods")) return 18;
        if (kinds.contains("cafes")) return 5;
        if (kinds.contains("museums")) return 12;
        if (kinds.contains("theatres_and_entertainments") || kinds.contains("cinemas")) return 10;
        if (kinds.contains("amusements")) return 8;
        // monuments, historic, architecture, natural, parks, view_points → gratuits
        return 0;
    }

    /**
     * Returns the permutation of steps that minimises total straight-line distance
     * (open path, no fixed start/end).
     * Exact TSP with branch-and-bound for N ≤ 9; NN+2-opt fallback beyond that.
     */
    private List<PathStep> optimalOrder(List<PathStep> steps) {
        int n = steps.size();
        if (n <= 2) return new ArrayList<>(steps);
        if (n <= 9) {
            TspState state = new TspState();
            permTSP(null, new ArrayList<>(steps), new ArrayList<>(n), 0.0, state);
            return state.bestPath != null ? state.bestPath : new ArrayList<>(steps);
        }
        // Fallback for large N: nearest-neighbor + 2-opt
        List<PathStep> result = new ArrayList<>(steps);
        nnOrder(result);
        twoOptImprove(result);
        return result;
    }

    private static class TspState {
        List<PathStep> bestPath = null;
        double bestDist = Double.MAX_VALUE;
    }

    private void permTSP(PathStep current, List<PathStep> remaining,
                         List<PathStep> path, double dist, TspState state) {
        if (remaining.isEmpty()) {
            if (dist < state.bestDist) {
                state.bestDist = dist;
                state.bestPath = new ArrayList<>(path);
            }
            return;
        }
        for (int i = 0; i < remaining.size(); i++) {
            PathStep next = remaining.remove(i);
            double d = current == null ? 0.0 : distanceKm(
                    current.getLatitude(), current.getLongitude(),
                    next.getLatitude(), next.getLongitude());
            double newDist = dist + d;
            if (newDist < state.bestDist) { // branch-and-bound pruning
                path.add(next);
                permTSP(next, remaining, path, newDist, state);
                path.remove(path.size() - 1);
            }
            remaining.add(i, next);
        }
    }

    /** Nearest-neighbor reorder in-place (used as fallback seed for 2-opt). */
    private void nnOrder(List<PathStep> steps) {
        int n = steps.size();
        for (int i = 0; i < n - 1; i++) {
            PathStep cur = steps.get(i);
            int nearestIdx = i + 1;
            double minDist = distanceKm(cur.getLatitude(), cur.getLongitude(),
                    steps.get(i + 1).getLatitude(), steps.get(i + 1).getLongitude());
            for (int j = i + 2; j < n; j++) {
                double d = distanceKm(cur.getLatitude(), cur.getLongitude(),
                        steps.get(j).getLatitude(), steps.get(j).getLongitude());
                if (d < minDist) { minDist = d; nearestIdx = j; }
            }
            Collections.swap(steps, i + 1, nearestIdx);
        }
    }

    /** 2-opt improvement in-place (no fixed start). */
    private void twoOptImprove(List<PathStep> route) {
        int n = route.size();
        boolean improved = true;
        while (improved) {
            improved = false;
            outer:
            for (int i = 0; i < n - 1; i++) {
                for (int j = i + 2; j < n; j++) {
                    double before = stepDist(route, i, i + 1)
                            + (j < n - 1 ? stepDist(route, j, j + 1) : 0);
                    double after = stepDist(route, i, j)
                            + (j < n - 1 ? stepDist(route, i + 1, j + 1) : 0);
                    if (after < before - 1e-9) {
                        Collections.reverse(route.subList(i + 1, j + 1));
                        improved = true;
                        break outer;
                    }
                }
            }
        }
    }

    private double stepDist(List<PathStep> route, int a, int b) {
        return distanceKm(route.get(a).getLatitude(), route.get(a).getLongitude(),
                route.get(b).getLatitude(), route.get(b).getLongitude());
    }

    private void displayOptions(List<PathOption> options, int dureeMax) {
        optionsContainer.removeAllViews();
        selectedOption = null;
        steps.clear();
        stepsContainer.removeAllViews();

        tvOptionsTitle.setVisibility(View.VISIBLE);

        for (PathOption option : options) {
            View card = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_path_option, optionsContainer, false);

            TextView tvName = card.findViewById(R.id.tv_option_name);
            TextView tvMetrics = card.findViewById(R.id.tv_option_metrics);
            TextView tvSteps = card.findViewById(R.id.tv_option_steps);
            com.google.android.material.button.MaterialButton btnSelect =
                    card.findViewById(R.id.btn_option_select);

            tvName.setText(option.name);
            tvMetrics.setText(option.steps.size() + " étapes · ~"
                    + String.format("%.1f", option.totalDistanceKm) + " km · ~"
                    + option.estimatedBudget + " €");

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < option.steps.size(); i++) {
                if (i > 0) sb.append(" → ");
                sb.append(option.steps.get(i).getName());
            }
            tvSteps.setText(sb.toString());

            btnSelect.setOnClickListener(v -> selectOption(option));
            card.setOnClickListener(v -> selectOption(option));

            optionsContainer.addView(card);
        }
    }

    private void selectOption(PathOption option) {
        selectedOption = option;
        steps.clear();
        stepsContainer.removeAllViews();
        for (PathStep step : option.steps) {
            steps.add(step);
            addStepView(step, steps.size());
        }
        enrichStepsWithDetails();
        Toast.makeText(requireContext(),
                "Parcours « " + option.name + " » sélectionné",
                Toast.LENGTH_SHORT).show();
    }

    private void enrichStepsWithDetails() {
        String apiKey = "5ae2e3f221c38a28845f05b64ceacf3f82755ce118bd16981c7984e8";
        for (int i = 0; i < steps.size(); i++) {
            PathStep step = steps.get(i);
            int stepIndex = i;
            executor.execute(() -> {
                try {
                    String desc = "";
                    String imageUrl = null;

                    // 1. OpenTripMap details (si xid disponible)
                    String xid = step.getXid();
                    if (xid != null && !xid.isEmpty()) {
                        try {
                            URL otmUrl = new URL("https://api.opentripmap.com/0.1/en/places/xid/"
                                    + xid + "?apikey=" + apiKey);
                            HttpURLConnection otmConn = (HttpURLConnection) otmUrl.openConnection();
                            otmConn.setRequestProperty("User-Agent", "TravelingApp/1.0");
                            otmConn.setConnectTimeout(6000);
                            otmConn.setReadTimeout(6000);
                            if (otmConn.getResponseCode() == 200) {
                                BufferedReader r = new BufferedReader(
                                        new InputStreamReader(otmConn.getInputStream()));
                                StringBuilder sb = new StringBuilder();
                                String l;
                                while ((l = r.readLine()) != null) sb.append(l);
                                r.close();
                                JSONObject detail = new JSONObject(sb.toString());
                                Log.d("CreatePath", "OTM " + step.getName()
                                        + " hasPreview=" + detail.has("preview")
                                        + " hasImage=" + detail.has("image")
                                        + " hasWiki=" + detail.has("wikipedia_extracts"));
                                if (detail.has("wikipedia_extracts")) {
                                    desc = detail.getJSONObject("wikipedia_extracts")
                                            .optString("text", "");
                                    if (desc.length() > 200) desc = desc.substring(0, 197) + "…";
                                }
                                if (desc.isEmpty() && detail.has("info")) {
                                    desc = detail.getJSONObject("info").optString("descr", "");
                                }
                                if (detail.has("preview")) {
                                    imageUrl = detail.getJSONObject("preview")
                                            .optString("source", null);
                                }
                                if (imageUrl == null || imageUrl.isEmpty()) {
                                    String img = detail.optString("image", "");
                                    if (img.startsWith("http")) imageUrl = img;
                                }
                            }
                        } catch (Exception e) {
                            Log.w("CreatePath", "OTM detail failed for " + xid, e);
                        }
                    }

                    // 2. Wikipedia REST API en fallback (meilleure couverture)
                    if (imageUrl == null || desc.isEmpty()) {
                        String encoded = URLEncoder.encode(step.getName(), "UTF-8");
                        for (String lang : new String[]{"fr", "en"}) {
                            try {
                                URL wikiUrl = new URL("https://" + lang
                                        + ".wikipedia.org/api/rest_v1/page/summary/" + encoded);
                                HttpURLConnection wikiConn =
                                        (HttpURLConnection) wikiUrl.openConnection();
                                wikiConn.setRequestProperty("User-Agent", "TravelingApp/1.0");
                                wikiConn.setConnectTimeout(6000);
                                wikiConn.setReadTimeout(6000);
                                if (wikiConn.getResponseCode() == 200) {
                                    BufferedReader r = new BufferedReader(
                                            new InputStreamReader(wikiConn.getInputStream()));
                                    StringBuilder sb = new StringBuilder();
                                    String l;
                                    while ((l = r.readLine()) != null) sb.append(l);
                                    r.close();
                                    JSONObject wikiData = new JSONObject(sb.toString());
                                    if (desc.isEmpty()) {
                                        String extract = wikiData.optString("extract", "");
                                        if (!extract.isEmpty()) {
                                            desc = extract.length() > 200
                                                    ? extract.substring(0, 197) + "…" : extract;
                                        }
                                    }
                                    if (imageUrl == null && wikiData.has("thumbnail")) {
                                        imageUrl = wikiData.getJSONObject("thumbnail")
                                                .optString("source", null);
                                    }
                                    if (!desc.isEmpty() && imageUrl != null) break;
                                }
                            } catch (Exception ignored) {}
                        }
                    }

                    Log.d("CreatePath", "Enriched " + step.getName()
                            + " → desc=" + !desc.isEmpty() + " img=" + (imageUrl != null));

                    // 3. Prix réel depuis Overpass (tags OSM fee/charge)
                    String priceInfo = fetchPriceFromOverpass(
                            step.getLatitude(), step.getLongitude());

                    Bitmap bmp = (imageUrl != null) ? downloadBitmap(imageUrl) : null;
                    String finalDesc = priceInfo != null
                            ? (priceInfo + (desc.isEmpty() ? "" : "\n" + desc))
                            : desc;
                    String b64 = bmp != null ? ImageUtils.bitmapToBase64(bmp) : null;

                    mainHandler.post(() -> {
                        if (!isAdded()) return;
                        if (!finalDesc.isEmpty()) step.setDescription(finalDesc);
                        if (b64 != null) step.setImageBase64(b64);
                        if (stepIndex < stepsContainer.getChildCount()) {
                            updateStepViewDetails(stepsContainer.getChildAt(stepIndex), step);
                        }
                    });
                } catch (Exception e) {
                    Log.w("CreatePath", "Enrich " + step.getName() + " failed", e);
                }
            });
        }
    }

    private Bitmap downloadBitmap(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "TravelingApp/1.0");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            if (conn.getResponseCode() != 200) return null;
            InputStream is = conn.getInputStream();
            Bitmap bmp = BitmapFactory.decodeStream(is);
            is.close();
            return bmp;
        } catch (Exception e) {
            return null;
        }
    }

    private String fetchPriceFromOverpass(double lat, double lon) {
        if (lat == 0 && lon == 0) return null;
        try {
            String query = "[out:json][timeout:8];"
                    + "(node(around:150," + lat + "," + lon + ")[\"fee\"];"
                    + "way(around:150," + lat + "," + lon + ")[\"fee\"];);"
                    + "out tags;";
            URL url = new URL("https://overpass-api.de/api/interpreter?data="
                    + URLEncoder.encode(query, "UTF-8"));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "TravelingApp/1.0");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            if (conn.getResponseCode() != 200) return null;

            BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String l;
            while ((l = r.readLine()) != null) sb.append(l);
            r.close();

            JSONArray elements = new JSONObject(sb.toString()).optJSONArray("elements");
            if (elements == null) return null;

            for (int i = 0; i < elements.length(); i++) {
                JSONObject tags = elements.getJSONObject(i).optJSONObject("tags");
                if (tags == null) continue;
                String fee = tags.optString("fee", "");
                if (fee.isEmpty()) continue;
                if ("no".equals(fee)) return "Entrée gratuite";
                String charge = tags.optString("charge", "");
                if (!charge.isEmpty()) return "Entrée : " + simplifyCharge(charge);
                return "Entrée payante";
            }
        } catch (Exception e) {
            Log.d("CreatePath", "Overpass price fetch failed: " + e.getMessage());
        }
        return null;
    }

    private String simplifyCharge(String charge) {
        // Normaliser la devise
        String c = charge.replaceAll("(?i)euros?", "€").replaceAll("(?i)eur\\b", "€").trim();
        // Extraire le premier montant (ex: "adult: 15 €; child: 7 €" → "15 €")
        Matcher m = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*€").matcher(c);
        if (m.find()) return m.group(1).replace(",", ".") + " €";
        m = Pattern.compile("€\\s*(\\d+(?:[.,]\\d+)?)").matcher(c);
        if (m.find()) return m.group(1).replace(",", ".") + " €";
        return c.length() > 40 ? c.substring(0, 37) + "…" : c;
    }

    private void updateStepViewDetails(View stepView, PathStep step) {
        String desc = step.getDescription();
        if (desc != null && !desc.isEmpty()) {
            TextView tvDesc = stepView.findViewById(R.id.tv_step_desc);
            tvDesc.setText(desc);
            tvDesc.setVisibility(View.VISIBLE);
        }
        String b64 = step.getImageBase64();
        if (b64 != null && !b64.isEmpty()) {
            Bitmap bmp = ImageUtils.base64ToBitmap(b64);
            if (bmp != null) {
                ((ImageView) stepView.findViewById(R.id.img_step_photo)).setImageBitmap(bmp);
                stepView.findViewById(R.id.card_step_photo).setVisibility(View.VISIBLE);
                stepView.findViewById(R.id.tv_step_number).setVisibility(View.GONE);
            }
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
                    kinds.add("tourist_facilities");
                    kinds.add("monuments_and_memorials");
                    kinds.add("view_points");
                    kinds.add("historic");
                    break;
            }
        }

        if (kinds.isEmpty()) {
            kinds.add("tourist_facilities");
            kinds.add("monuments_and_memorials");
            kinds.add("historic");
            kinds.add("cultural");
        }
        return TextUtils.join(",", kinds);
    }

}
