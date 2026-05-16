package com.example.traveling.ui.create;

import android.Manifest;
import android.content.ClipData;
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
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.traveling.BuildConfig;
import com.example.traveling.R;
import com.example.traveling.worker.SchedulePublishHelper;
import com.example.traveling.data.FirestoreRepository;
import com.example.traveling.data.GroupRepository;
import com.example.traveling.data.ImageUtils;
import com.example.traveling.data.PhotoRegistry;
import com.example.traveling.model.Group;
import com.example.traveling.model.Photo;
import com.example.traveling.session.SessionManager;
import com.example.traveling.ui.explore.PhotoCarouselAdapter;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.example.traveling.data.GeocodingUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SharePhotoFragment extends Fragment {

    private static final String GROQ_API_KEY = BuildConfig.GROQ_API_KEY;
    private static final int MAX_PHOTOS = 10;

    // Views
    private ViewPager2 vpPreview;
    private LinearLayout createDotsIndicator;
    private TextView tvPhotoCount;
    private View pickPlaceholder;
    private FrameLayout photoPickerZone;
    private LinearLayout layoutThumbnailsSection;
    private TextView tvImageCount;
    private com.google.android.material.button.MaterialButton btnAddPhoto;
    private RecyclerView rvThumbnails;
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
    private com.google.android.material.button.MaterialButton btnAiAnnotate;

    // Data
    private final List<Object> selectedImages = new ArrayList<>(); // Uri or Bitmap
    private final List<String> selectedGroupIds = new ArrayList<>();
    private List<Group> userGroups = new ArrayList<>();

    // Edit mode
    private Photo editingPhoto = null;

    // Adapters
    private PhotoCarouselAdapter previewCarouselAdapter;
    private SelectedPhotoAdapter selectedPhotoAdapter;

    // Launchers
    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    int remaining = MAX_PHOTOS - selectedImages.size();
                    if (data.getClipData() != null) {
                        ClipData clip = data.getClipData();
                        int count = Math.min(clip.getItemCount(), remaining);
                        for (int i = 0; i < count; i++) {
                            selectedImages.add(clip.getItemAt(i).getUri());
                        }
                        if (clip.getItemCount() > remaining) {
                            Toast.makeText(requireContext(),
                                    "Maximum " + MAX_PHOTOS + " photos. Seules les " + remaining + " premières ont été ajoutées.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else if (data.getData() != null && remaining > 0) {
                        selectedImages.add(data.getData());
                    }
                    updatePreviewUI();
                }
            });

    private final ActivityResultLauncher<Void> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
                if (bitmap != null && selectedImages.size() < MAX_PHOTOS) {
                    selectedImages.add(bitmap);
                    updatePreviewUI();
                } else if (bitmap != null) {
                    Toast.makeText(requireContext(), "Maximum " + MAX_PHOTOS + " photos atteint", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String> fileLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null && selectedImages.size() < MAX_PHOTOS) {
                    selectedImages.add(uri);
                    updatePreviewUI();
                } else if (uri != null) {
                    Toast.makeText(requireContext(), "Maximum " + MAX_PHOTOS + " photos atteint", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) cameraLauncher.launch(null);
                else Toast.makeText(requireContext(), "Permission caméra refusée", Toast.LENGTH_SHORT).show();
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

        photoPickerZone = view.findViewById(R.id.photo_picker_zone);
        vpPreview = view.findViewById(R.id.vp_preview);
        createDotsIndicator = view.findViewById(R.id.create_dots_indicator);
        tvPhotoCount = view.findViewById(R.id.tv_photo_count);
        pickPlaceholder = view.findViewById(R.id.pick_placeholder);
        layoutThumbnailsSection = view.findViewById(R.id.layout_thumbnails_section);
        tvImageCount = view.findViewById(R.id.tv_image_count);
        btnAddPhoto = view.findViewById(R.id.btn_add_photo);
        rvThumbnails = view.findViewById(R.id.rv_thumbnails);

        etTitle = view.findViewById(R.id.et_title);
        etDescription = view.findViewById(R.id.et_description);
        etLocation = view.findViewById(R.id.et_location);
        setupLocationAutocomplete();
        chipGroupType = view.findViewById(R.id.chip_group_type);
        switchPublic = view.findViewById(R.id.switch_public);
        layoutGroupCheckboxes = view.findViewById(R.id.layout_group_checkboxes);
        tvNoGroups = view.findViewById(R.id.tv_no_groups);
        btnAiAnnotate = view.findViewById(R.id.btn_ai_annotate);

        // Preview carousel
        previewCarouselAdapter = new PhotoCarouselAdapter();
        vpPreview.setAdapter(previewCarouselAdapter);
        vpPreview.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateDots(createDotsIndicator, selectedImages.size(), position);
                tvPhotoCount.setText((position + 1) + "/" + selectedImages.size());
            }
        });

        // Thumbnails strip
        selectedPhotoAdapter = new SelectedPhotoAdapter(selectedImages, pos -> {
            selectedImages.remove(pos);
            selectedPhotoAdapter.notifyItemRemoved(pos);
            selectedPhotoAdapter.notifyItemRangeChanged(pos, selectedImages.size());
            updatePreviewUI();
        });
        rvThumbnails.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvThumbnails.setAdapter(selectedPhotoAdapter);

        // Click handlers
        photoPickerZone.setOnClickListener(v -> {
            if (selectedImages.isEmpty()) showImageSourceDialog();
        });
        btnAddPhoto.setOnClickListener(v -> showImageSourceDialog());
        btnAiAnnotate.setOnClickListener(v -> annotateWithAI());

        view.findViewById(R.id.btn_back).setOnClickListener(v -> handleBack());
        view.findViewById(R.id.btn_publish).setOnClickListener(v -> showPublishOptionsDialog());

        if (!SessionManager.get().isAnonymous()) {
            loadUserGroups();
        } else {
            tvNoGroups.setText("Connectez-vous pour partager dans un groupe");
        }

        // Edit mode: check if a photo was passed via PhotoRegistry
        Photo toEdit = PhotoRegistry.get();
        if (toEdit != null) {
            PhotoRegistry.set(null);
            editingPhoto = toEdit;
            enterEditMode(toEdit);
        }
    }

    private void enterEditMode(Photo photo) {
        // Update toolbar title
        View toolbar = requireView().findViewById(R.id.btn_back);
        if (toolbar != null && toolbar.getParent() instanceof android.view.ViewGroup) {
            android.view.ViewGroup bar = (android.view.ViewGroup) toolbar.getParent();
            for (int i = 0; i < bar.getChildCount(); i++) {
                if (bar.getChildAt(i) instanceof TextView) {
                    ((TextView) bar.getChildAt(i)).setText("Modifier la photo");
                }
            }
        }

        // Pre-fill text fields
        if (photo.getTitle() != null)        etTitle.setText(photo.getTitle());
        if (photo.getDescription() != null)  etDescription.setText(photo.getDescription());
        if (photo.getLocationName() != null) etLocation.setText(photo.getLocationName());

        // Switch: public ON, private/draft OFF
        switchPublic.setChecked(!"private".equals(photo.getVisibility()));

        // Location type chip
        String type = photo.getLocationType();
        if (type != null) {
            switch (type) {
                case "nature":      chipGroupType.check(R.id.chip_nature);      break;
                case "musée":       chipGroupType.check(R.id.chip_musee);       break;
                case "rue":         chipGroupType.check(R.id.chip_rue);         break;
                case "restaurant":  chipGroupType.check(R.id.chip_restaurant);  break;
                case "autre":       chipGroupType.check(R.id.chip_autre);       break;
            }
        }

        // Load existing images as Bitmaps in background
        List<String> images = photo.getImages();
        if (!images.isEmpty()) {
            executor.execute(() -> {
                for (String b64 : images) {
                    android.graphics.Bitmap bmp = com.example.traveling.data.ImageUtils.base64ToBitmap(b64);
                    if (bmp != null) selectedImages.add(bmp);
                }
                mainHandler.post(() -> { if (isAdded()) updatePreviewUI(); });
            });
        }
    }

    private void updatePreviewUI() {
        if (selectedImages.isEmpty()) {
            pickPlaceholder.setVisibility(View.VISIBLE);
            vpPreview.setVisibility(View.GONE);
            createDotsIndicator.setVisibility(View.GONE);
            tvPhotoCount.setVisibility(View.GONE);
            layoutThumbnailsSection.setVisibility(View.GONE);
            photoPickerZone.setClickable(true);
            return;
        }

        pickPlaceholder.setVisibility(View.GONE);
        vpPreview.setVisibility(View.VISIBLE);
        layoutThumbnailsSection.setVisibility(View.VISIBLE);
        photoPickerZone.setClickable(false);

        // Update carousel
        previewCarouselAdapter.setImages(selectedImages);

        // Dots (only when 2+ images)
        int count = selectedImages.size();
        if (count > 1) {
            createDotsIndicator.setVisibility(View.VISIBLE);
            tvPhotoCount.setVisibility(View.VISIBLE);
            int current = vpPreview.getCurrentItem();
            setupDots(createDotsIndicator, count);
            updateDots(createDotsIndicator, count, Math.min(current, count - 1));
            tvPhotoCount.setText((Math.min(current, count - 1) + 1) + "/" + count);
        } else {
            createDotsIndicator.setVisibility(View.GONE);
            tvPhotoCount.setVisibility(View.GONE);
        }

        // Thumbnails
        selectedPhotoAdapter.notifyDataSetChanged();
        tvImageCount.setText(count + "/" + MAX_PHOTOS + " photo" + (count > 1 ? "s" : ""));
        btnAddPhoto.setVisibility(count < MAX_PHOTOS ? View.VISIBLE : View.GONE);
    }

    private void setupDots(LinearLayout container, int count) {
        container.removeAllViews();
        int sizeSel = dpToPx(8);
        int sizeUnsel = dpToPx(6);
        int margin = dpToPx(4);
        for (int i = 0; i < count; i++) {
            View dot = new View(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sizeSel, sizeSel);
            lp.setMargins(margin, 0, margin, 0);
            dot.setLayoutParams(lp);
            dot.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.dot_unselected));
            container.addView(dot);
        }
    }

    private void updateDots(LinearLayout container, int count, int selected) {
        for (int i = 0; i < container.getChildCount(); i++) {
            View dot = container.getChildAt(i);
            boolean isSelected = (i == selected);
            int size = dpToPx(isSelected ? 8 : 6);
            int margin = dpToPx(4);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMargins(margin, 0, margin, 0);
            dot.setLayoutParams(lp);
            dot.setBackground(ContextCompat.getDrawable(requireContext(),
                    isSelected ? R.drawable.dot_selected : R.drawable.dot_unselected));
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void setupLocationAutocomplete() {
        locationAdapter = new ArrayAdapter<String>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, new ArrayList<>()) {
            @Override
            public Filter getFilter() {
                return new Filter() {
                    @Override
                    protected FilterResults performFiltering(CharSequence constraint) {
                        FilterResults r = new FilterResults();
                        List<String> all = new ArrayList<>();
                        for (int i = 0; i < getCount(); i++) all.add(getItem(i));
                        r.values = all;
                        r.count = all.size();
                        return r;
                    }
                    @Override
                    protected void publishResults(CharSequence constraint, FilterResults results) {
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
                            Intent intent = new Intent(Intent.ACTION_PICK,
                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                            galleryLauncher.launch(intent);
                            break;
                        case 1:
                            if (ContextCompat.checkSelfPermission(requireContext(),
                                    Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
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

    private boolean hasContent() {
        boolean hasImage = !selectedImages.isEmpty();
        boolean hasTitle = etTitle.getText() != null && !etTitle.getText().toString().trim().isEmpty();
        return hasImage || hasTitle;
    }

    private void handleBack() {
        if (editingPhoto != null) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Abandonner les modifications ?")
                    .setMessage("Vos modifications ne seront pas sauvegardées.")
                    .setPositiveButton("Abandonner", (d, w) ->
                            Navigation.findNavController(requireView()).navigateUp())
                    .setNegativeButton("Rester", null)
                    .show();
            return;
        }
        if (!hasContent()) {
            Navigation.findNavController(requireView()).navigateUp();
            return;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("Enregistrer en brouillon ?")
                .setMessage("Voulez-vous sauvegarder cette publication en brouillon pour la reprendre plus tard ?")
                .setPositiveButton("Brouillon", (d, w) -> saveAsDraft())
                .setNegativeButton("Abandonner", (d, w) ->
                        Navigation.findNavController(requireView()).navigateUp())
                .setNeutralButton("Annuler", null)
                .show();
    }

    private void saveAsDraft() {
        if (selectedImages.isEmpty()) {
            Navigation.findNavController(requireView()).navigateUp();
            return;
        }
        executor.execute(() -> {
            List<String> base64List = new ArrayList<>();
            for (Object img : selectedImages) {
                String b64 = (img instanceof Bitmap)
                        ? ImageUtils.bitmapToBase64((Bitmap) img)
                        : ImageUtils.uriToBase64(requireContext(), (Uri) img);
                if (b64 != null) base64List.add(b64);
            }
            if (base64List.isEmpty()) {
                mainHandler.post(() -> Navigation.findNavController(requireView()).navigateUp());
                return;
            }
            mainHandler.post(() -> {
                if (!isAdded()) return;
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                String authorId = user != null ? user.getUid() : "";
                String authorName = (user != null && user.getDisplayName() != null
                        && !user.getDisplayName().isEmpty()) ? user.getDisplayName() : "Moi";
                String title = etTitle.getText() != null
                        ? etTitle.getText().toString().trim() : "";
                if (title.isEmpty()) title = "Brouillon";
                String description = etDescription.getText() != null
                        ? etDescription.getText().toString().trim() : "";
                String location = etLocation.getText() != null
                        ? etLocation.getText().toString().trim() : "";
                String today = new java.text.SimpleDateFormat("dd MMM yyyy",
                        java.util.Locale.FRENCH).format(new java.util.Date());

                Photo photo = new Photo();
                photo.setTitle(title);
                photo.setDescription(description);
                photo.setAuthorId(authorId);
                photo.setAuthorName(authorName);
                photo.setLocationName(location);
                photo.setDate(today);
                photo.setLocationType(getSelectedLocationType());
                photo.setVisibility("draft");
                photo.setImageBase64List(base64List);
                photo.setImageBase64(base64List.get(0));
                Object first = selectedImages.get(0);
                if (first instanceof Bitmap) photo.setImageBitmap((Bitmap) first);
                else photo.setImageUri((Uri) first);

                FirestoreRepository.get().savePhoto(photo,
                        id -> {
                            if (!isAdded()) return;
                            Toast.makeText(requireContext(), "Brouillon enregistré",
                                    Toast.LENGTH_SHORT).show();
                            Navigation.findNavController(requireView()).navigateUp();
                        },
                        e -> Navigation.findNavController(requireView()).navigateUp());
            });
        });
    }

    private void showPublishOptionsDialog() {
        new AlertDialog.Builder(requireContext())
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
                                    Toast.makeText(requireContext(),
                                            "La date doit être dans le futur", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                publish(new com.google.firebase.Timestamp(scheduled.getTime()));
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

    private void publish(com.google.firebase.Timestamp scheduledDate) {
        String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";
        String location = etLocation.getText() != null ? etLocation.getText().toString().trim() : "";
        if (selectedImages.isEmpty()) {
            Toast.makeText(requireContext(), "Veuillez choisir au moins une photo", Toast.LENGTH_SHORT).show();
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
        String visibility = scheduledDate != null ? "scheduled"
                : (switchPublic.isChecked() ? "public" : "private");

        requireView().findViewById(R.id.btn_publish).setEnabled(false);

        // Encode all images to Base64 on background thread
        executor.execute(() -> {
            List<String> base64List = new ArrayList<>();
            for (Object img : selectedImages) {
                String b64;
                if (img instanceof Bitmap) {
                    b64 = ImageUtils.bitmapToBase64((Bitmap) img);
                } else {
                    b64 = ImageUtils.uriToBase64(requireContext(), (Uri) img);
                }
                if (b64 != null) base64List.add(b64);
            }

            if (base64List.isEmpty()) {
                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    requireView().findViewById(R.id.btn_publish).setEnabled(true);
                    Toast.makeText(requireContext(), "Erreur lors du traitement des images", Toast.LENGTH_SHORT).show();
                });
                return;
            }

            mainHandler.post(() -> {
                if (!isAdded()) return;

                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                String authorId = user != null ? user.getUid() : "";
                String authorName = "Moi";
                if (user != null && user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                    authorName = user.getDisplayName();
                }

                String today = new java.text.SimpleDateFormat("dd MMM yyyy",
                        java.util.Locale.FRENCH).format(new java.util.Date());

                Photo photo = new Photo();
                photo.setTitle(title);
                photo.setDescription(description);
                photo.setAuthorId(authorId);
                photo.setAuthorName(authorName);
                photo.setLocationName(location);
                photo.setDate(today);
                photo.setLocationType(getSelectedLocationType());
                photo.setVisibility(visibility);
                if (scheduledDate != null) photo.setScheduledPublishDate(scheduledDate);
                photo.setImageBase64List(base64List);
                photo.setImageBase64(base64List.get(0));

                // Set local preview from first selected image
                Object first = selectedImages.get(0);
                if (first instanceof Bitmap) photo.setImageBitmap((Bitmap) first);
                else photo.setImageUri((Uri) first);

                GeocodingUtils.geocode(location, new GeocodingUtils.GeocodingCallback() {
                    @Override public void onResult(double lat, double lon) {
                        photo.setLatitude(lat);
                        photo.setLongitude(lon);
                        savePhoto(photo, scheduledDate);
                    }
                    @Override public void onFailure() { savePhoto(photo, scheduledDate); }
                });
            });
        });
    }

    private void savePhoto(Photo photo, com.google.firebase.Timestamp scheduledDate) {
        if (editingPhoto != null && editingPhoto.getId() != null) {
            FirestoreRepository.get().updatePhoto(editingPhoto.getId(), photo,
                    v -> {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), "Modifications enregistrées", Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(requireView()).navigateUp();
                    },
                    e -> {
                        if (isAdded()) {
                            requireView().findViewById(R.id.btn_publish).setEnabled(true);
                            Toast.makeText(requireContext(), "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            FirestoreRepository.get().savePhoto(photo,
                    photoId -> {
                        if (!isAdded()) return;
                        for (String groupId : selectedGroupIds) {
                            GroupRepository.get().addGroupPost(groupId, photoId, null, null);
                        }
                        // Notify users following the photo's tag (only for immediate publications)
                        android.util.Log.d("SharePhoto", "locationType=" + photo.getLocationType() + " scheduledDate=" + scheduledDate);
                        if (scheduledDate == null && photo.getLocationType() != null && !photo.getLocationType().isEmpty()) {
                            FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
                            String publisherName = me != null && me.getDisplayName() != null ? me.getDisplayName() : "Quelqu'un";
                            String publisherId = me != null ? me.getUid() : "";
                            android.util.Log.d("SharePhoto", "Calling notifyTagFollowers tag=" + photo.getLocationType());
                            FirestoreRepository.get().notifyTagFollowers(
                                    photo.getLocationType(), publisherId, publisherName,
                                    photoId, "photo", photo.getTitle());
                        }
                        String msg;
                        if (scheduledDate != null) {
                            SchedulePublishHelper.schedule(requireContext(), photoId,
                                    SchedulePublishHelper.COLLECTION_PHOTOS,
                                    scheduledDate.toDate());
                            String formatted = new java.text.SimpleDateFormat(
                                    "dd MMM yyyy 'à' HH:mm", java.util.Locale.FRENCH)
                                    .format(scheduledDate.toDate());
                            msg = "Publication planifiée pour le " + formatted;
                        } else {
                            msg = selectedGroupIds.isEmpty()
                                    ? "Photo publiée !"
                                    : "Photo publiée et partagée dans " + selectedGroupIds.size() + " groupe(s) !";
                        }
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
                        Navigation.findNavController(requireView()).navigateUp();
                    },
                    e -> {
                        if (isAdded()) {
                            requireView().findViewById(R.id.btn_publish).setEnabled(true);
                            Toast.makeText(requireContext(), "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void annotateWithAI() {
        if (selectedImages.isEmpty()) {
            Toast.makeText(requireContext(), "Veuillez d'abord choisir une photo", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        String location = etLocation.getText() != null ? etLocation.getText().toString().trim() : "";
        String locationType = getSelectedLocationType();

        // Use first image for AI annotation
        Object firstImg = selectedImages.get(0);
        final String imageBase64;
        if (firstImg instanceof Bitmap) {
            imageBase64 = ImageUtils.bitmapToBase64((Bitmap) firstImg);
        } else {
            imageBase64 = ImageUtils.uriToBase64(requireContext(), (Uri) firstImg);
        }
        if (imageBase64 == null) {
            Toast.makeText(requireContext(), "Erreur lors du traitement de l'image", Toast.LENGTH_SHORT).show();
            return;
        }

        btnAiAnnotate.setEnabled(false);
        btnAiAnnotate.setText("Génération en cours…");

        executor.execute(() -> {
            try {
                StringBuilder prompt = new StringBuilder();
                prompt.append("Tu es un assistant de voyage. Génère une description courte et inspirante (2-3 phrases) pour cette photo de voyage.");
                if (!title.isEmpty()) prompt.append(" Titre : \"").append(title).append("\".");
                if (!location.isEmpty()) prompt.append(" Lieu : ").append(location).append(".");
                if (!locationType.isEmpty()) prompt.append(" Type de lieu : ").append(locationType).append(".");
                prompt.append(" Réponds uniquement avec la description, sans guillemets ni introduction.");

                org.json.JSONObject textPart = new org.json.JSONObject();
                textPart.put("type", "text");
                textPart.put("text", prompt.toString());

                org.json.JSONObject imageUrl = new org.json.JSONObject();
                imageUrl.put("url", "data:image/jpeg;base64," + imageBase64);

                org.json.JSONObject imagePart = new org.json.JSONObject();
                imagePart.put("type", "image_url");
                imagePart.put("image_url", imageUrl);

                org.json.JSONArray contentArray = new org.json.JSONArray();
                contentArray.put(textPart);
                contentArray.put(imagePart);

                org.json.JSONObject message = new org.json.JSONObject();
                message.put("role", "user");
                message.put("content", contentArray);

                org.json.JSONObject body = new org.json.JSONObject();
                body.put("model", "meta-llama/llama-4-scout-17b-16e-instruct");
                body.put("messages", new org.json.JSONArray().put(message));

                java.net.URL url = new java.net.URL("https://api.groq.com/openai/v1/chat/completions");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + GROQ_API_KEY);
                conn.setDoOutput(true);
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(30000);
                conn.getOutputStream().write(body.toString().getBytes("UTF-8"));

                int code = conn.getResponseCode();
                java.io.InputStream stream = code == 200 ? conn.getInputStream() : conn.getErrorStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                if (code != 200) {
                    mainHandler.post(() -> {
                        if (!isAdded()) return;
                        resetAiButton();
                        Toast.makeText(requireContext(), "Erreur IA (" + code + ")", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                String generated = new org.json.JSONObject(sb.toString())
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content");

                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    resetAiButton();
                    etDescription.setText(generated.trim());
                });

            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    resetAiButton();
                    Toast.makeText(requireContext(), "Erreur lors de la génération IA", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void resetAiButton() {
        btnAiAnnotate.setEnabled(true);
        btnAiAnnotate.setText("✨ Annoter avec l'IA");
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
