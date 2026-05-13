package com.example.traveling.ui.create;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.Filter;

import java.util.ArrayList;
import java.util.List;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.traveling.R;
import com.example.traveling.data.FirestoreRepository;
import com.example.traveling.data.GroupRepository;
import com.example.traveling.data.ImageUtils;
import com.example.traveling.model.Group;
import com.example.traveling.model.Photo;
import com.example.traveling.session.SessionManager;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.example.traveling.data.GeocodingUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SharePhotoFragment extends Fragment {

    private ImageView ivPreview;
    private View pickPlaceholder;
    private com.google.android.material.textfield.TextInputEditText etTitle, etDescription;
    private AutoCompleteTextView etLocation;
    private ArrayAdapter<String> locationAdapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingLocationSearch;
    private ChipGroup chipGroupType;
    private SwitchMaterial switchPublic;
    private LinearLayout layoutGroupCheckboxes;
    private TextView tvNoGroups;

    private Uri selectedImageUri = null;
    private Bitmap selectedBitmap = null;
    private final List<String> selectedGroupIds = new ArrayList<>();
    private List<Group> userGroups = new ArrayList<>();

    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK
                        && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    selectedBitmap = null;
                    ivPreview.setImageURI(selectedImageUri);
                    showPreview();
                }
            });

    private final ActivityResultLauncher<Void> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
                if (bitmap != null) {
                    selectedBitmap = bitmap;
                    selectedImageUri = null;
                    ivPreview.setImageBitmap(bitmap);
                    showPreview();
                }
            });

    private final ActivityResultLauncher<String> fileLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    selectedBitmap = null;
                    ivPreview.setImageURI(uri);
                    showPreview();
                }
            });

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) cameraLauncher.launch(null);
                else Toast.makeText(requireContext(),
                        "Permission caméra refusée", Toast.LENGTH_SHORT).show();
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_share_photo, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ivPreview = view.findViewById(R.id.iv_preview);
        pickPlaceholder = view.findViewById(R.id.pick_placeholder);
        etTitle = view.findViewById(R.id.et_title);
        etDescription = view.findViewById(R.id.et_description);
        etLocation = view.findViewById(R.id.et_location);
        setupLocationAutocomplete();
        chipGroupType = view.findViewById(R.id.chip_group_type);
        switchPublic = view.findViewById(R.id.switch_public);

        layoutGroupCheckboxes = view.findViewById(R.id.layout_group_checkboxes);
        tvNoGroups = view.findViewById(R.id.tv_no_groups);

        FrameLayout photoZone = view.findViewById(R.id.photo_picker_zone);
        photoZone.setOnClickListener(v -> showImageSourceDialog());

        view.findViewById(R.id.btn_back).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());
        view.findViewById(R.id.btn_publish).setOnClickListener(v -> publish());

        if (!SessionManager.get().isAnonymous()) {
            loadUserGroups();
        } else {
            tvNoGroups.setText("Connectez-vous pour partager dans un groupe");
        }
    }

    private void setupLocationAutocomplete() {
        locationAdapter = new ArrayAdapter<String>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, new ArrayList<>()) {
            @Override
            public Filter getFilter() {
                return new Filter() {
                    @Override
                    protected Filter.FilterResults performFiltering(CharSequence constraint) {
                        Filter.FilterResults r = new Filter.FilterResults();
                        List<String> all = new ArrayList<>();
                        for (int i = 0; i < getCount(); i++) all.add(getItem(i));
                        r.values = all;
                        r.count = all.size();
                        return r;
                    }
                    @Override
                    protected void publishResults(CharSequence constraint, Filter.FilterResults results) {
                        notifyDataSetChanged();
                    }
                };
            }
        };
        etLocation.setAdapter(locationAdapter);
        etLocation.setThreshold(2);

        etLocation.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (pendingLocationSearch != null) mainHandler.removeCallbacks(pendingLocationSearch);
                String query = s.toString().trim();
                if (query.length() < 2) return;
                pendingLocationSearch = () -> searchLocations(query);
                mainHandler.postDelayed(pendingLocationSearch, 350);
            }
        });
    }

    private void searchLocations(String query) {
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

                org.json.JSONArray results = new org.json.JSONArray(sb.toString());
                List<String> places = new ArrayList<>();
                for (int i = 0; i < results.length(); i++) {
                    org.json.JSONObject item = results.getJSONObject(i);
                    String displayName = item.getString("display_name");
                    String[] parts = displayName.split(",");
                    String placeName = parts[0].trim();
                    if (parts.length > 1) placeName = placeName + ", " + parts[parts.length - 1].trim();
                    if (!places.contains(placeName)) places.add(placeName);
                }

                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    locationAdapter.clear();
                    locationAdapter.addAll(places);
                    locationAdapter.notifyDataSetChanged();
                    if (!places.isEmpty() && etLocation.hasFocus()) etLocation.showDropDown();
                });
            } catch (Exception ignored) {}
        });
    }

    private void loadUserGroups() {
        tvNoGroups.setText("Chargement des groupes...");
        GroupRepository.get().loadUserGroups(groups -> {
            if (!isAdded()) return;
            userGroups = groups;
            layoutGroupCheckboxes.removeAllViews();
            if (groups.isEmpty()) {
                tvNoGroups.setText("Vous n'avez pas encore de groupes");
                tvNoGroups.setVisibility(View.VISIBLE);
                return;
            }
            tvNoGroups.setVisibility(View.GONE);
            for (Group g : groups) {
                CheckBox cb = new CheckBox(requireContext());
                cb.setText(g.getName());
                cb.setTextSize(14f);
                cb.setOnCheckedChangeListener((btn, checked) -> {
                    if (checked) selectedGroupIds.add(g.getId());
                    else selectedGroupIds.remove(g.getId());
                });
                layoutGroupCheckboxes.addView(cb);
            }
        });
    }

    private void showImageSourceDialog() {
        String[] options = {"Galerie", "Appareil photo", "Fichiers / Documents"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Choisir une source")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            galleryLauncher.launch(new Intent(
                                    Intent.ACTION_PICK,
                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI));
                            break;
                        case 1:
                            if (ContextCompat.checkSelfPermission(requireContext(),
                                    Manifest.permission.CAMERA)
                                    == PackageManager.PERMISSION_GRANTED) {
                                cameraLauncher.launch(null);
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
                            }
                            break;
                        case 2:
                            fileLauncher.launch("image/*");
                            break;
                    }
                })
                .show();
    }

    private void showPreview() {
        ivPreview.setVisibility(View.VISIBLE);
        pickPlaceholder.setVisibility(View.GONE);
    }

    private void publish() {
        String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";
        String location = etLocation.getText() != null ? etLocation.getText().toString().trim() : "";

        if (selectedImageUri == null && selectedBitmap == null) {
            Toast.makeText(requireContext(), "Veuillez choisir une photo", Toast.LENGTH_SHORT).show();
            return;
        }
        if (title.isEmpty()) {
            etTitle.setError("Titre requis");
            etTitle.requestFocus();
            return;
        }
        if (location.isEmpty()) {
            etLocation.setError("Lieu requis");
            etLocation.requestFocus();
            return;
        }

        // Convert image to Base64
        String imageBase64;
        if (selectedBitmap != null) {
            imageBase64 = ImageUtils.bitmapToBase64(selectedBitmap);
        } else {
            imageBase64 = ImageUtils.uriToBase64(requireContext(), selectedImageUri);
        }
        if (imageBase64 == null) {
            Toast.makeText(requireContext(), "Erreur lors du traitement de l'image", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get current user info
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String authorId = user != null ? user.getUid() : "";
        String authorName = "Moi";
        if (user != null && user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
            authorName = user.getDisplayName();
        }

        String today = new java.text.SimpleDateFormat("dd MMM yyyy",
                java.util.Locale.FRENCH).format(new java.util.Date());

        boolean isPublic = switchPublic.isChecked();

        // Build Photo object
        Photo photo = new Photo();
        photo.setTitle(title);
        photo.setDescription(description);
        photo.setAuthorId(authorId);
        photo.setAuthorName(authorName);
        photo.setLocationName(location);
        photo.setDate(today);
        photo.setLocationType(getSelectedLocationType());
        photo.setIsPublic(isPublic);
        photo.setImageBase64(imageBase64);
        if (selectedBitmap != null) photo.setImageBitmap(selectedBitmap);
        else photo.setImageUri(selectedImageUri);

        // Disable publish button to prevent double-tap
        requireView().findViewById(R.id.btn_publish).setEnabled(false);

        // Géocodage du lieu avant sauvegarde
        GeocodingUtils.geocode(location, new GeocodingUtils.GeocodingCallback() {
            @Override
            public void onResult(double latitude, double longitude) {
                photo.setLatitude(latitude);
                photo.setLongitude(longitude);
                savePhoto(photo);
            }
            @Override
            public void onFailure() {
                savePhoto(photo); // sauvegarde sans coordonnées si échec
            }
        });
    }

    private void savePhoto(Photo photo) {
        // Save to Firestore
        FirestoreRepository.get().savePhoto(photo,
                photoId -> {
                    if (!isAdded()) return;
                    // Share to each selected group
                    for (String groupId : selectedGroupIds) {
                        GroupRepository.get().addGroupPost(groupId, photoId, null, null);
                    }
                    String msg = selectedGroupIds.isEmpty()
                            ? "Photo publiée !"
                            : "Photo publiée et partagée dans " + selectedGroupIds.size() + " groupe(s) !";
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(requireView()).navigateUp();
                },
                e -> {
                    if (isAdded()) {
                        requireView().findViewById(R.id.btn_publish).setEnabled(true);
                        Toast.makeText(requireContext(), "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String getSelectedLocationType() {
        int checkedId = chipGroupType.getCheckedChipId();
        if (checkedId == R.id.chip_nature) return "nature";
        if (checkedId == R.id.chip_musee) return "musée";
        if (checkedId == R.id.chip_rue) return "rue";
        if (checkedId == R.id.chip_restaurant) return "restaurant";
        if (checkedId == R.id.chip_autre) return "autre";
        return "";
    }
}
