package com.example.traveling.ui.create;

import android.net.Uri;
import android.os.Bundle;
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
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.traveling.R;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

public class SharePhotoFragment extends Fragment {

    private ImageView ivPreview;
    private View pickPlaceholder;
    private TextInputEditText etTitle;
    private TextInputEditText etDescription;
    private TextInputEditText etLocation;
    private ChipGroup chipGroupType;
    private SwitchMaterial switchPublic;

    private Uri selectedImageUri = null;

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    ivPreview.setImageURI(uri);
                    ivPreview.setVisibility(View.VISIBLE);
                    pickPlaceholder.setVisibility(View.GONE);
                }
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
        photoZone.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        view.findViewById(R.id.btn_back).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());

        view.findViewById(R.id.btn_publish).setOnClickListener(v -> publish());
    }

    private void publish() {
        String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";
        String location = etLocation.getText() != null ? etLocation.getText().toString().trim() : "";

        if (selectedImageUri == null) {
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

        String locationType = getSelectedLocationType();
        boolean isPublic = switchPublic.isChecked();

        // TODO: sauvegarder dans Firestore + uploader l'image dans Firebase Storage
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
