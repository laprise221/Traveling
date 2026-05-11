package com.example.traveling.ui.explore;

import android.os.Bundle;
import android.util.Log;
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

import com.example.traveling.R;
import com.example.traveling.data.FirestoreRepository;
import com.example.traveling.data.NotificationRepository;
import com.example.traveling.data.PathRegistry;
import com.example.traveling.data.PhotoRegistry;
import com.example.traveling.model.Photo;
import com.example.traveling.model.TravelPath;
import com.example.traveling.session.SessionManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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

    private List<Photo> loadedPhotos = new ArrayList<>();
    private List<TravelPath> loadedPaths = new ArrayList<>();

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
        loadPhotosFromFirestore();
        loadPathsFromFirestore();

        searchBar.setOnClickListener(v -> {
            NavOptions opts = new NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setPopUpTo(R.id.navigation_explore, false)
                    .build();
            Navigation.findNavController(v)
                    .navigate(R.id.navigation_search_filter, null, opts);
        });


    }

    private void loadPhotosFromFirestore() {
        FirestoreRepository.get().loadPublicPhotos(photos -> {
            if (!isAdded()) return;
            loadedPhotos = photos;
            if (!SessionManager.get().isAnonymous()) {
                FirestoreRepository.get().checkLikedPhotos(photos, () -> {
                    if (!isAdded()) return;
                    FirestoreRepository.get().checkFavoritedPhotos(photos, () -> {
                        if (!isAdded()) return;
                        photoAdapter1.setPhotos(loadedPhotos);
                        photoAdapter2.setPhotos(loadedPhotos);
                    });
                });
            } else {
                photoAdapter1.setPhotos(loadedPhotos);
                photoAdapter2.setPhotos(loadedPhotos);
            }
        });
    }

    private void loadPathsFromFirestore() {
        FirestoreRepository.get().loadPublicPaths(paths -> {
            if (!isAdded()) return;
            loadedPaths = paths;
            if (!SessionManager.get().isAnonymous()) {
                FirestoreRepository.get().checkLikedPaths(paths, () -> {
                    if (!isAdded()) return;
                    FirestoreRepository.get().checkFavoritedPaths(paths, () -> {
                        if (!isAdded()) return;
                        pathAdapter1.setPaths(loadedPaths);
                        pathAdapter2.setPaths(loadedPaths);
                    });
                });
            } else {
                pathAdapter1.setPaths(loadedPaths);
                pathAdapter2.setPaths(loadedPaths);
            }
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
            Toast.makeText(requireContext(), "Connectez-vous pour liker", Toast.LENGTH_SHORT).show();
            return;
        }
        boolean newLiked = !photo.isLiked();
        photo.setLiked(newLiked);
        photoAdapter1.notifyItemChanged(position);
        photoAdapter2.notifyItemChanged(position);
        if (photo.getId() != null) {
            FirestoreRepository.get().toggleLikePhoto(photo.getId(), newLiked, null);
            if (newLiked && photo.getAuthorId() != null) {
                FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
                if (me != null && !me.getUid().equals(photo.getAuthorId())) {
                    String name = me.getDisplayName() != null ? me.getDisplayName() : "Quelqu'un";
                    NotificationRepository.get().sendNotification(
                            photo.getAuthorId(), "like", me.getUid(), name,
                            photo.getId(), "photo", photo.getTitle(), null);
                }
            }
        }
    }

    @Override
    public void onFavoriteClick(Photo photo, int position) {
        if (SessionManager.get().isAnonymous()) {
            Toast.makeText(requireContext(), "Connectez-vous pour ajouter aux favoris", Toast.LENGTH_SHORT).show();
            return;
        }
        boolean newFav = !photo.isFavorited();
        photo.setFavorited(newFav);
        photoAdapter1.notifyItemChanged(position);
        photoAdapter2.notifyItemChanged(position);
        if (photo.getId() != null) {
            FirestoreRepository.get().toggleFavoritePhoto(photo.getId(), newFav, null);
            if (newFav && photo.getAuthorId() != null) {
                FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
                if (me != null && !me.getUid().equals(photo.getAuthorId())) {
                    String name = me.getDisplayName() != null ? me.getDisplayName() : "Quelqu'un";
                    NotificationRepository.get().sendNotification(
                            photo.getAuthorId(), "favorite", me.getUid(), name,
                            photo.getId(), "photo", photo.getTitle(), null);
                }
            }
        }
        Toast.makeText(requireContext(),
                newFav ? "Ajouté aux favoris" : "Retiré des favoris",
                Toast.LENGTH_SHORT).show();
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
            Toast.makeText(requireContext(), "Connectez-vous pour liker", Toast.LENGTH_SHORT).show();
            return;
        }
        boolean newLiked = !path.isLiked();
        path.setLiked(newLiked);
        pathAdapter1.notifyItemChanged(position);
        pathAdapter2.notifyItemChanged(position);
        if (path.getId() != null) {
            FirestoreRepository.get().toggleLikePath(path.getId(), newLiked, null);
            if (newLiked && path.getAuthorId() != null) {
                FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
                if (me != null && !me.getUid().equals(path.getAuthorId())) {
                    String name = me.getDisplayName() != null ? me.getDisplayName() : "Quelqu'un";
                    NotificationRepository.get().sendNotification(
                            path.getAuthorId(), "like", me.getUid(), name,
                            path.getId(), "path", path.getTitle(), null);
                }
            }
        }
    }

    @Override
    public void onFavoriteClick(TravelPath path, int position) {
        if (SessionManager.get().isAnonymous()) {
            Toast.makeText(requireContext(), "Connectez-vous pour ajouter aux favoris", Toast.LENGTH_SHORT).show();
            return;
        }
        boolean newFav = !path.isFavorited();
        path.setFavorited(newFav);
        pathAdapter1.notifyItemChanged(position);
        pathAdapter2.notifyItemChanged(position);
        if (path.getId() != null) {
            FirestoreRepository.get().toggleFavoritePath(path.getId(), newFav, null);
            if (newFav && path.getAuthorId() != null) {
                FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
                if (me != null && !me.getUid().equals(path.getAuthorId())) {
                    String name = me.getDisplayName() != null ? me.getDisplayName() : "Quelqu'un";
                    NotificationRepository.get().sendNotification(
                            path.getAuthorId(), "favorite", me.getUid(), name,
                            path.getId(), "path", path.getTitle(), null);
                }
            }
        }
        Toast.makeText(requireContext(),
                newFav ? "Ajouté aux favoris" : "Retiré des favoris",
                Toast.LENGTH_SHORT).show();
    }
}
