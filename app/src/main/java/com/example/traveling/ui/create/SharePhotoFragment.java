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
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.traveling.R;
import com.example.traveling.data.UserRepository;
import com.example.traveling.model.Photo;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

public class SharePhotoFragment extends Fragment {

    private ImageView ivPreview;
    private View pickPlaceholder;
    private TextInputEditText etTitle, etDescription, etLocation;
    private ChipGroup chipGroupType;
    private SwitchMaterial switchPublic;

    private Uri selectedImageUri = null;
    private Bitmap selectedBitmap = null;

    // 1. Galerie (ACTION_PICK → ouvre la galerie locale directement)
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

    // 2. Appareil photo (prévisualisation basse résolution, sans FileProvider)
    private final ActivityResultLauncher<Void> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
                if (bitmap != null) {
                    selectedBitmap = bitmap;
                    selectedImageUri = null;
                    ivPreview.setImageBitmap(bitmap);
                    showPreview();
                }
            });

    // 3. Fichiers (picker système, inclut Drive, téléchargements, etc.)
    private final ActivityResultLauncher<String> fileLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    selectedBitmap = null;
                    ivPreview.setImageURI(uri);
                    showPreview();
                }
            });

    // Permission caméra
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
        chipGroupType = view.findViewById(R.id.chip_group_type);
        switchPublic = view.findViewById(R.id.switch_public);

        FrameLayout photoZone = view.findViewById(R.id.photo_picker_zone);
        photoZone.setOnClickListener(v -> showImageSourceDialog());

        view.findViewById(R.id.btn_back).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());
        view.findViewById(R.id.btn_publish).setOnClickListener(v -> publish());
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

        String id = "user_" + System.currentTimeMillis();
        String today = new java.text.SimpleDateFormat("dd MMM yyyy",
                java.util.Locale.FRENCH).format(new java.util.Date());

        Photo photo = new Photo(id, title, description,
                "Moi", location, 0, 0, today, getSelectedLocationType(), 0, 0);
        if (selectedBitmap != null) photo.setImageBitmap(selectedBitmap);
        else photo.setImageUri(selectedImageUri);

        UserRepository.get().addPhoto(photo);
        Toast.makeText(requireContext(), "Photo publiée !", Toast.LENGTH_SHORT).show();
        Navigation.findNavController(requireView()).navigateUp();
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
