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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.traveling.R;
import com.example.traveling.data.UserRepository;
import com.example.traveling.model.Photo;
import com.example.traveling.model.TravelPath;
import com.example.traveling.session.SessionManager;
import com.example.traveling.ui.explore.PathAdapter;
import com.example.traveling.ui.explore.PhotoAdapter;
import com.google.android.material.tabs.TabLayout;

import java.util.List;

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
                if (tab.getPosition() == 0) showLikedPhotos();
                else showLikedPaths();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        showLikedPhotos();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Rafraîchit à chaque retour sur l'onglet
        if (!SessionManager.get().isAnonymous()) {
            int selected = tabLayout.getSelectedTabPosition();
            if (selected == 1) showLikedPaths();
            else showLikedPhotos();
        }
    }

    private void showLikedPhotos() {
        List<Photo> photos = UserRepository.get().getLikedPhotos();
        recyclerView.setAdapter(photoAdapter);
        photoAdapter.setPhotos(photos);
        emptyText.setVisibility(photos.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showLikedPaths() {
        List<TravelPath> paths = UserRepository.get().getLikedPaths();
        recyclerView.setAdapter(pathAdapter);
        pathAdapter.setPaths(paths);
        emptyText.setVisibility(paths.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override public void onPhotoClick(Photo photo) {
        Toast.makeText(requireContext(), photo.getTitle(), Toast.LENGTH_SHORT).show();
    }

    @Override public void onLikeClick(Photo photo, int position) {
        photo.setLiked(false);
        UserRepository.get().unlikePhoto(photo);
        showLikedPhotos();
    }

    @Override public void onPathClick(TravelPath path) {
        Toast.makeText(requireContext(), path.getTitle(), Toast.LENGTH_SHORT).show();
    }

    @Override public void onLikeClick(TravelPath path, int position) {
        path.setLiked(false);
        UserRepository.get().unlikePath(path);
        showLikedPaths();
    }
}
