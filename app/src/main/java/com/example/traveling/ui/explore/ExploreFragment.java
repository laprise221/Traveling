package com.example.traveling.ui.explore;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.traveling.R;
import com.example.traveling.data.SampleData;
import com.example.traveling.model.Photo;
import com.example.traveling.model.TravelPath;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

public class ExploreFragment extends Fragment
        implements PhotoAdapter.OnPhotoClickListener, PathAdapter.OnPathClickListener {

    private TabLayout tabLayout;
    private RecyclerView recyclerView;
    private TextView emptyStateText;
    private TextInputEditText searchEditText;

    private PhotoAdapter photoAdapter;
    private PathAdapter pathAdapter;

    private List<Photo> allPhotos;
    private List<TravelPath> allPaths;

    private boolean showingPhotos = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_explore, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tabLayout = view.findViewById(R.id.tab_layout_explore);
        recyclerView = view.findViewById(R.id.recycler_explore);
        emptyStateText = view.findViewById(R.id.empty_state_text);
        searchEditText = view.findViewById(R.id.search_edit_text);

        // Charger les données
        allPhotos = SampleData.getSamplePhotos();
        allPaths = SampleData.getSamplePaths();

        // Configurer les adaptateurs
        photoAdapter = new PhotoAdapter(this);
        pathAdapter = new PathAdapter(this);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Onglets
        tabLayout.addTab(tabLayout.newTab().setText("Photos"));
        tabLayout.addTab(tabLayout.newTab().setText("Parcours"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showingPhotos = tab.getPosition() == 0;
                updateList();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Recherche
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterList(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Afficher les photos par défaut
        showingPhotos = true;
        updateList();
    }

    private void updateList() {
        String query = searchEditText.getText() != null
                ? searchEditText.getText().toString() : "";

        if (showingPhotos) {
            recyclerView.setAdapter(photoAdapter);
            photoAdapter.setPhotos(allPhotos);
            if (!query.isEmpty()) {
                photoAdapter.filterByQuery(allPhotos, query);
            }
            emptyStateText.setVisibility(allPhotos.isEmpty() ? View.VISIBLE : View.GONE);
        } else {
            recyclerView.setAdapter(pathAdapter);
            pathAdapter.setPaths(allPaths);
            if (!query.isEmpty()) {
                pathAdapter.filterByQuery(allPaths, query);
            }
            emptyStateText.setVisibility(allPaths.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void filterList(String query) {
        if (showingPhotos) {
            photoAdapter.filterByQuery(allPhotos, query);
        } else {
            pathAdapter.filterByQuery(allPaths, query);
        }
    }

    // --- PhotoAdapter callbacks ---

    @Override
    public void onPhotoClick(Photo photo) {
        Toast.makeText(requireContext(),
                photo.getTitle() + " - " + photo.getLocationName(),
                Toast.LENGTH_SHORT).show();
        // TODO: Ouvrir la fiche détaillée de la photo
    }

    @Override
    public void onLikeClick(Photo photo, int position) {
        photo.setLiked(!photo.isLiked());
        photoAdapter.notifyItemChanged(position);
    }

    // --- PathAdapter callbacks ---

    @Override
    public void onPathClick(TravelPath path) {
        Toast.makeText(requireContext(),
                path.getTitle() + " - " + path.getCity(),
                Toast.LENGTH_SHORT).show();
        // TODO: Ouvrir la fiche détaillée du parcours
    }

    @Override
    public void onLikeClick(TravelPath path, int position) {
        path.setLiked(!path.isLiked());
        pathAdapter.notifyItemChanged(position);
    }
}
