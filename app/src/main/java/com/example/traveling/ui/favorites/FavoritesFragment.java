package com.example.traveling.ui.favorites;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.traveling.R;
import com.example.traveling.data.FirestoreRepository;
import com.example.traveling.data.PhotoRegistry;
import com.example.traveling.model.Photo;
import com.example.traveling.model.TravelPath;
import com.example.traveling.session.SessionManager;
import com.example.traveling.ui.explore.PathAdapter;
import com.example.traveling.ui.explore.PhotoAdapter;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;

public class FavoritesFragment extends Fragment
        implements PhotoAdapter.OnPhotoClickListener, PathAdapter.OnPathClickListener {

    private TabLayout tabLayout;
    private RecyclerView recyclerView;
    private TextView emptyText;
    private View favoritesContainer;
    private TextView anonymousText;

    private PhotoAdapter photoAdapter;
    private PathAdapter pathAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favorites, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tabLayout = view.findViewById(R.id.tab_layout_favorites);
        favoritesContainer = view.findViewById(R.id.favorites_container);
        anonymousText = view.findViewById(R.id.anonymous_favorites_text);
        recyclerView = view.findViewById(R.id.recycler_favorites);
        emptyText = view.findViewById(R.id.empty_favorites_text);

        if (SessionManager.get().isAnonymous()) {
            tabLayout.setVisibility(View.GONE);
            favoritesContainer.setVisibility(View.GONE);
            anonymousText.setVisibility(View.VISIBLE);
            return;
        }

        photoAdapter = new PhotoAdapter(this);
        pathAdapter = new PathAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        tabLayout.addTab(tabLayout.newTab().setText("Photos"));
        tabLayout.addTab(tabLayout.newTab().setText("Parcours"));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) showFavoritedPhotos();
                else showFavoritedPaths();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        showFavoritedPhotos();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!SessionManager.get().isAnonymous()) {
            int selected = tabLayout.getSelectedTabPosition();
            if (selected == 1) showFavoritedPaths();
            else showFavoritedPhotos();
        }
    }

    private void showFavoritedPhotos() {
        recyclerView.setAdapter(photoAdapter);
        emptyText.setVisibility(View.GONE);

        FirestoreRepository.get().loadFavoritedPhotoIds(photoIds -> {
            if (!isAdded()) return;
            if (photoIds.isEmpty()) {
                photoAdapter.setPhotos(new ArrayList<>());
                emptyText.setVisibility(View.VISIBLE);
                return;
            }
            FirestoreRepository.get().loadPhotosByIds(photoIds, photos -> {
                if (!isAdded()) return;
                for (Photo p : photos) p.setFavoritedSilent(true);
                photoAdapter.setPhotos(photos);
                emptyText.setVisibility(photos.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }

    private void showFavoritedPaths() {
        recyclerView.setAdapter(pathAdapter);
        emptyText.setVisibility(View.GONE);

        FirestoreRepository.get().loadFavoritedPathIds(pathIds -> {
            if (!isAdded()) return;
            if (pathIds.isEmpty()) {
                pathAdapter.setPaths(new ArrayList<>());
                emptyText.setVisibility(View.VISIBLE);
                return;
            }
            FirestoreRepository.get().loadPathsByIds(pathIds, paths -> {
                if (!isAdded()) return;
                for (TravelPath p : paths) p.setFavoritedSilent(true);
                pathAdapter.setPaths(paths);
                emptyText.setVisibility(paths.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }

    @Override public void onPhotoClick(Photo photo) {
        PhotoRegistry.set(photo);
        Navigation.findNavController(requireView()).navigate(R.id.navigation_photo_detail);
    }

    @Override public void onLikeClick(Photo photo, int position) {
        // Like depuis les favoris — persiste directement
        boolean newLiked = !photo.isLiked();
        photo.setLiked(newLiked);
        photoAdapter.notifyItemChanged(position);
        if (photo.getId() != null) {
            FirestoreRepository.get().toggleLikePhoto(photo.getId(), newLiked, null);
        }
    }

    @Override public void onFavoriteClick(Photo photo, int position) {
        // Retirer des favoris
        photo.setFavorited(false);
        if (photo.getId() != null) {
            FirestoreRepository.get().toggleFavoritePhoto(photo.getId(), false, null);
        }
        showFavoritedPhotos();
    }

    @Override public void onPathClick(TravelPath path) {
        Toast.makeText(requireContext(), path.getTitle(), Toast.LENGTH_SHORT).show();
    }

    @Override public void onLikeClick(TravelPath path, int position) {
        boolean newLiked = !path.isLiked();
        path.setLiked(newLiked);
        pathAdapter.notifyItemChanged(position);
        if (path.getId() != null) {
            FirestoreRepository.get().toggleLikePath(path.getId(), newLiked, null);
        }
    }

    @Override public void onFavoriteClick(TravelPath path, int position) {
        path.setFavorited(false);
        if (path.getId() != null) {
            FirestoreRepository.get().toggleFavoritePath(path.getId(), false, null);
        }
        showFavoritedPaths();
    }
}
