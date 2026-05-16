package com.example.traveling.ui.explore;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.traveling.R;
import com.example.traveling.data.FilterRegistry;
import com.example.traveling.data.FirestoreRepository;
import com.example.traveling.data.NotificationRepository;
import com.example.traveling.data.PathRegistry;
import com.example.traveling.data.PhotoRegistry;
import com.example.traveling.data.SearchFilters;
import com.example.traveling.model.Photo;
import com.example.traveling.model.TravelPath;
import com.example.traveling.session.SessionManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExploreFragment extends Fragment
        implements PhotoCardAdapter.Listener, PathCardAdapter.Listener {

    private PhotoCardAdapter photoAdapter1, photoAdapter2;
    private PathCardAdapter pathAdapter1, pathAdapter2;
    private RecyclerView recycler1, recycler2;
    private TabLayout tabLayout;
    private boolean showingPhotos = true;

    private List<Photo> loadedPhotos = new ArrayList<>();
    private List<TravelPath> loadedPaths = new ArrayList<>();
    private List<String> followedTags = new ArrayList<>();

    private ViewPager2 heroViewPager;
    private LinearLayout heroDotsContainer;
    private final Handler autoScrollHandler = new Handler(Looper.getMainLooper());
    private Runnable autoScrollRunnable;
    private static final int AUTO_SCROLL_DELAY_MS = 3000;
    private static final int[] HERO_IMAGES = {
            R.drawable.montagne,
            R.drawable.plage,
            R.drawable.lac,
            R.drawable.tokyo,
            R.drawable.cactus
    };

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
        tabLayout = view.findViewById(R.id.tab_layout_explore);
        recycler1 = view.findViewById(R.id.recycler_photos);
        recycler2 = view.findViewById(R.id.recycler_paths);

        heroViewPager = view.findViewById(R.id.hero_view_pager);
        heroDotsContainer = view.findViewById(R.id.hero_dots_container);
        setupHeroCarousel();

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
        loadFollowedTagsThenPhotos();
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

    private void setupHeroCarousel() {
        List<Integer> images = new ArrayList<>();
        for (int resId : HERO_IMAGES) images.add(resId);

        HeroCarouselAdapter adapter = new HeroCarouselAdapter(images);
        heroViewPager.setAdapter(adapter);

        setupHeroDots(images.size());

        heroViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateHeroDots(position);
            }
        });

        autoScrollRunnable = new Runnable() {
            @Override
            public void run() {
                if (heroViewPager == null) return;
                int next = (heroViewPager.getCurrentItem() + 1) % HERO_IMAGES.length;
                heroViewPager.setCurrentItem(next, true);
                autoScrollHandler.postDelayed(this, AUTO_SCROLL_DELAY_MS);
            }
        };
    }

    private void setupHeroDots(int count) {
        heroDotsContainer.removeAllViews();
        int sizePx = (int) (8 * getResources().getDisplayMetrics().density);
        int marginPx = (int) (4 * getResources().getDisplayMetrics().density);
        for (int i = 0; i < count; i++) {
            ImageView dot = new ImageView(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(sizePx, sizePx);
            params.setMargins(marginPx, 0, marginPx, 0);
            dot.setLayoutParams(params);
            dot.setImageResource(i == 0 ? R.drawable.dot_selected : R.drawable.dot_unselected);
            heroDotsContainer.addView(dot);
        }
    }

    private void updateHeroDots(int selectedPosition) {
        for (int i = 0; i < heroDotsContainer.getChildCount(); i++) {
            ImageView dot = (ImageView) heroDotsContainer.getChildAt(i);
            dot.setImageResource(i == selectedPosition ? R.drawable.dot_selected : R.drawable.dot_unselected);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        autoScrollHandler.removeCallbacks(autoScrollRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();
        autoScrollHandler.postDelayed(autoScrollRunnable, AUTO_SCROLL_DELAY_MS);
        if (tabLayout != null) {
            tabLayout.selectTab(tabLayout.getTabAt(showingPhotos ? 0 : 1));
        }
        // Ré-appliquer seulement si des photos sont chargées
        if (!loadedPhotos.isEmpty()) {
            List<Photo> filtered = applyFilters(loadedPhotos);
            photoAdapter1.setPhotos(sortByFollowedTags(filtered));
            photoAdapter2.setPhotos(sortByPopularity(filtered));
        }
    }

    /** Charge d'abord les tags suivis (si connecté), puis les photos. */
    private void loadFollowedTagsThenPhotos() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && !user.isAnonymous()) {
            FirestoreRepository.get().loadFollowedTags(user.getUid(), tags -> {
                followedTags = tags != null ? tags : new ArrayList<>();
                loadPhotosFromFirestore();
            });
        } else {
            followedTags = new ArrayList<>();
            loadPhotosFromFirestore();
        }
    }

    private void loadPhotosFromFirestore() {
        FirestoreRepository.get().loadPublicPhotos(publicPhotos -> {
            if (!isAdded()) return;
            if (!SessionManager.get().isAnonymous()) {
                FirestoreRepository.get().loadPrivatePhotos(privatePhotos -> {
                    if (!isAdded()) return;
                    List<Photo> all = new ArrayList<>(publicPhotos);
                    all.addAll(privatePhotos);
                    loadedPhotos = all;
                    FirestoreRepository.get().checkLikedPhotos(all, () -> {
                        if (!isAdded()) return;
                        FirestoreRepository.get().checkFavoritedPhotos(all, () -> {
                            if (!isAdded()) return;
                            List<Photo> filtered = applyFilters(loadedPhotos);
                            photoAdapter1.setPhotos(sortByFollowedTags(filtered));
                            photoAdapter2.setPhotos(sortByPopularity(filtered));
                        });
                    });
                });
            } else {
                loadedPhotos = publicPhotos;
                List<Photo> filtered = applyFilters(loadedPhotos);
                photoAdapter1.setPhotos(filtered);
                photoAdapter2.setPhotos(sortByPopularity(filtered));
            }
        });
    }

    /**
     * Trie les photos : celles dont le type correspond à un tag suivi apparaissent en premier
     * (ordre chronologique inversé conservé dans chaque groupe).
     * Si aucun tag suivi, retourne la liste telle quelle (déjà triée par date desc).
     */
    private List<Photo> sortByFollowedTags(List<Photo> photos) {
        if (followedTags.isEmpty()) return photos;
        List<Photo> matched = new ArrayList<>();
        List<Photo> rest = new ArrayList<>();
        for (Photo p : photos) {
            String type = p.getLocationType() != null ? p.getLocationType().toLowerCase() : "";
            if (followedTags.contains(type)) {
                matched.add(p);
            } else {
                rest.add(p);
            }
        }
        matched.addAll(rest);
        return matched;
    }

    private List<Photo> sortByPopularity(List<Photo> photos) {
        List<Photo> sorted = new ArrayList<>(photos);
        Collections.sort(sorted, (a, b) -> {
            int scoreA = a.getLikeCount() + a.getCommentCount() + a.getFavoriteCount();
            int scoreB = b.getLikeCount() + b.getCommentCount() + b.getFavoriteCount();
            return Integer.compare(scoreB, scoreA);
        });
        return sorted;
    }

    private List<TravelPath> sortPathsByPopularity(List<TravelPath> paths) {
        List<TravelPath> sorted = new ArrayList<>(paths);
        Collections.sort(sorted, (a, b) -> {
            int scoreA = a.getLikeCount() + a.getCommentCount() + a.getFavoriteCount();
            int scoreB = b.getLikeCount() + b.getCommentCount() + b.getFavoriteCount();
            return Integer.compare(scoreB, scoreA);
        });
        return sorted;
    }

    private List<Photo> applyFilters(List<Photo> photos) {
        SearchFilters filters = FilterRegistry.get();
        if (filters.isEmpty()) return photos;

        List<Photo> result = new ArrayList<>();
        for (Photo p : photos) {
            // Filtre texte (titre, description, lieu, auteur)
            if (!filters.query.isEmpty()) {
                String q = filters.query.toLowerCase();
                boolean match =
                        (p.getTitle() != null && p.getTitle().toLowerCase().contains(q))
                        || (p.getDescription() != null && p.getDescription().toLowerCase().contains(q))
                        || (p.getLocationName() != null && p.getLocationName().toLowerCase().contains(q))
                        || (p.getAuthorName() != null && p.getAuthorName().toLowerCase().contains(q));
                if (!match) continue;
            }
            // Filtre type de lieu (insensible à la casse)
            if (!filters.locationTypes.isEmpty()) {
                if (p.getLocationType() == null) continue;
                String pType = p.getLocationType().toLowerCase();
                boolean typeMatch = false;
                for (String t : filters.locationTypes) {
                    if (t.toLowerCase().equals(pType)) { typeMatch = true; break; }
                }
                if (!typeMatch) continue;
            }
            // Filtre auteur
            if (!filters.authorName.isEmpty()) {
                if (p.getAuthorName() == null
                        || !p.getAuthorName().toLowerCase()
                                .contains(filters.authorName.toLowerCase())) continue;
            }
            // Filtre période
            if (filters.dateFromMs > 0 || filters.dateToMs > 0) {
                long photoMs = -1;
                if (p.getCreatedAt() != null) {
                    photoMs = p.getCreatedAt().toDate().getTime();
                }
                if (photoMs < 0) continue;
                if (filters.dateFromMs > 0 && photoMs < filters.dateFromMs) continue;
                if (filters.dateToMs   > 0 && photoMs > filters.dateToMs)   continue;
            }
            result.add(p);
        }
        return result;
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
                        pathAdapter2.setPaths(sortPathsByPopularity(loadedPaths));
                    });
                });
            } else {
                pathAdapter1.setPaths(loadedPaths);
                pathAdapter2.setPaths(sortPathsByPopularity(loadedPaths));
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
