package com.example.traveling.ui.search;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.traveling.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;

public class SearchFilterFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search_filter, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Slider slider = view.findViewById(R.id.slider_radius);
        TextView radiusValue = view.findViewById(R.id.radius_value);
        slider.addOnChangeListener((s, value, fromUser) ->
                radiusValue.setText((int) value + " km"));

        MaterialButton apply = view.findViewById(R.id.btn_apply_filters);
        apply.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Filtres appliqués", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(v).navigateUp();
        });
    }
}
