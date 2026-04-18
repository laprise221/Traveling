package com.example.traveling.ui.explore;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;

import com.example.traveling.R;
import com.example.traveling.data.PathRegistry;
import com.example.traveling.data.PathRepository;
import com.example.traveling.data.PhotoRegistry;
import com.example.traveling.data.SampleData;
import com.example.traveling.data.UserRepository;
import com.example.traveling.session.SessionManager;
import com.example.traveling.model.Photo;
import com.example.traveling.model.TravelPath;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class ExploreFragment extends Fragment
        implements PhotoCardAdapter.Listener, PathCardAdapter.Listener {

    private PhotoCardAdapter photoAdapter1, photoAdapter2;
    private PathCardAdapter pathAdapter1, pathAdapter2;
    private RecyclerView recycler1, recycler2;
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

        MaterialCardView searchBar = view.findViewById(R.id.search_bar_card);
        MaterialButton planBtn = view.findViewById(R.id.btn_plan_path);
        TabLayout tabLayout = view.findViewById(R.id.tab_layout_explore);
        recycler1 = view.findViewById(R.id.recycler_photos);
        recycler2 = view.findViewById(R.id.recycler_paths);

        recycler1.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        recycler2.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));

        photoAdapter1 = new PhotoCardAdapter(this);
        photoAdapter2 = new PhotoCardAdapter(this);
        pathAdapter1 = new PathCardAdapter(this);
        pathAdapter2 = new PathCardAdapter(this);

        photoAdapter1.setPhotos(SampleData.getSamplePhotos());
        photoAdapter2.setPhotos(SampleData.getSamplePhotos());

        loadPathsFromFirestore();

        // Onglets
        tabLayout.addTab(tabLayout.newTab().setText("Photos"));
        tabLayout.addTab(tabLayout.newTab().setText("Parcours"));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                showingPhotos = tab.getPosition() == 0;
                updateContent();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
        updateContent();

        searchBar.setOnClickListener(v -> {
            NavOptions opts = new NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setPopUpTo(R.id.navigation_explore, false)
                    .build();
            Navigation.findNavController(v)
                    .navigate(R.id.navigation_search_filter, null, opts);
        });

        planBtn.setOnClickListener(v -> {
            if (SessionManager.get().isAnonymous()) {
                Toast.makeText(requireContext(),
                        "Connectez-vous pour planifier un parcours",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(requireContext(), "Planifier un parcours (TravelPath)",
                    Toast.LENGTH_SHORT).show();
        });
    }

    private void loadPathsFromFirestore() {
        List<TravelPath> samplePaths = SampleData.getSamplePaths();
        pathAdapter1.setPaths(samplePaths);
        pathAdapter2.setPaths(samplePaths);

        PathRepository.get().getPublicPaths()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded()) return;
                    Log.d("Explore", "Firestore: " + querySnapshot.size() + " parcours trouvés");
                    List<TravelPath> firestorePaths = querySnapshot.toObjects(TravelPath.class);
                    for (int i = 0; i < querySnapshot.size(); i++) {
                        firestorePaths.get(i).setId(querySnapshot.getDocuments().get(i).getId());
                    }
                    List<TravelPath> allPaths = new ArrayList<>(samplePaths);
                    allPaths.addAll(0, firestorePaths);
                    pathAdapter1.setPaths(allPaths);
                    pathAdapter2.setPaths(allPaths);
                })
                .addOnFailureListener(e -> {
                    Log.e("Explore", "Erreur chargement Firestore", e);
                });
    }

    private void updateContent() {
        if (showingPhotos) {
            recycler1.setAdapter(photoAdapter1);
            recycler2.setAdapter(photoAdapter2);
        } else {
            recycler1.setAdapter(pathAdapter1);
            recycler2.setAdapter(pathAdapter2);
        }
    }

    @Override
    public void onPhotoClick(Photo photo) {
        PhotoRegistry.set(photo);
        Navigation.findNavController(requireView())
                .navigate(R.id.navigation_photo_detail);
    }

    @Override
    public void onLikeClick(Photo photo, int position) {
        if (SessionManager.get().isAnonymous()) {
            Toast.makeText(requireContext(),
                    "Connectez-vous pour liker une photo",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        photo.setLiked(!photo.isLiked());
        if (photo.isLiked()) UserRepository.get().likePhoto(photo);
        else UserRepository.get().unlikePhoto(photo);
        photoAdapter1.notifyItemChanged(position);
        photoAdapter2.notifyItemChanged(position);
    }

    @Override
    public void onPathClick(TravelPath path) {
        PathRegistry.set(path);
        Navigation.findNavController(requireView())
                .navigate(R.id.navigation_path_detail);
    }

    @Override
    public void onLikeClick(TravelPath path, int position) {
        if (SessionManager.get().isAnonymous()) {
            Toast.makeText(requireContext(),
                    "Connectez-vous pour liker un parcours",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        path.setLiked(!path.isLiked());
        if (path.isLiked()) UserRepository.get().likePath(path);
        else UserRepository.get().unlikePath(path);
        pathAdapter1.notifyItemChanged(position);
        pathAdapter2.notifyItemChanged(position);
    }
}
