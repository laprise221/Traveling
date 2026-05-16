package com.example.traveling.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.traveling.R;
import com.example.traveling.data.FirestoreRepository;
import com.example.traveling.data.PhotoRegistry;
import com.example.traveling.model.Photo;
import com.example.traveling.model.TravelPath;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MyPublicationsFragment extends Fragment {

    private RecyclerView recycler;
    private TextView tvEmpty;
    private TabLayout tabLayout;
    private ImageButton btnFilter;
    private int currentTab = 0;

    // Données chargées depuis Firebase (non filtrées)
    private final List<Photo> allPhotos = new ArrayList<>();
    private final List<TravelPath> allPaths = new ArrayList<>();

    // État des filtres actifs (null = pas de filtre)
    private String activeVisibility = null;
    private String activeType = null;

    // Valeurs réelles stockées dans Firestore (voir SharePhotoFragment.getSelectedLocationType)
    private static final String[] PHOTO_TYPE_VALUES  = {"nature", "musée", "rue", "restaurant", "autre"};
    // Labels affichés dans le dialog
    private static final String[] PHOTO_TYPE_LABELS  = {"Nature", "Musée", "Rue / Architecture", "Restaurant", "Autre"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_publications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tabLayout  = view.findViewById(R.id.tab_layout_publications);
        recycler   = view.findViewById(R.id.recycler_publications);
        tvEmpty    = view.findViewById(R.id.tv_empty);
        btnFilter  = view.findViewById(R.id.btn_filter);

        view.findViewById(R.id.btn_back).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());

        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));

        tabLayout.addTab(tabLayout.newTab().setText("Photos"));
        tabLayout.addTab(tabLayout.newTab().setText("Parcours"));
        tabLayout.addTab(tabLayout.newTab().setText("Brouillons"));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                // Réinitialise les filtres au changement d'onglet
                activeVisibility = null;
                activeType = null;
                updateFilterButtonVisibility();
                loadData();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        btnFilter.setOnClickListener(v -> showFilterDialog());

        updateFilterButtonVisibility();
        loadData();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    /** Affiche le bouton filtre uniquement sur Photos et Parcours. */
    private void updateFilterButtonVisibility() {
        btnFilter.setVisibility(currentTab == 2 ? View.GONE : View.VISIBLE);
        // Teinte le bouton si un filtre est actif
        if (activeVisibility != null || activeType != null) {
            btnFilter.setColorFilter(0xFFFFD54F); // ambre = filtre actif
        } else {
            btnFilter.clearColorFilter();
        }
    }

    /** Charge les données depuis Firebase puis applique les filtres. */
    private void loadData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { setEmpty(true); return; }
        String uid = user.getUid();

        if (currentTab == 0) {
            FirestoreRepository.get().loadUserPhotos(uid, photos -> {
                if (!isAdded()) return;
                allPhotos.clear();
                for (Photo p : photos) {
                    if (!"draft".equals(p.getVisibility())) allPhotos.add(p);
                }
                sortPhotos(allPhotos);
                applyAndDisplay();
            });

        } else if (currentTab == 1) {
            FirestoreRepository.get().loadUserPaths(uid, paths -> {
                if (!isAdded()) return;
                allPaths.clear();
                allPaths.addAll(paths);
                sortPaths(allPaths);
                applyAndDisplay();
            });

        } else {
            // Brouillons — pas de filtre
            FirestoreRepository.get().loadUserPhotos(uid, photos -> {
                if (!isAdded()) return;
                List<Photo> drafts = new ArrayList<>();
                for (Photo p : photos) {
                    if ("draft".equals(p.getVisibility())) drafts.add(p);
                }
                sortPhotos(drafts);
                recycler.setAdapter(new PhotoPublicationAdapter(drafts, this::onDeletePhoto, this::onEditPhoto));
                setEmpty(drafts.isEmpty());
            });
        }
    }

    /** Applique les filtres actifs et met à jour l'adapteur. */
    private void applyAndDisplay() {
        if (currentTab == 0) {
            List<Photo> filtered = new ArrayList<>();
            for (Photo p : allPhotos) {
                if (activeVisibility != null && !matchesVisibility(p.getVisibility(), p.getIsPublic(), activeVisibility)) continue;
                if (activeType != null && !activeType.equals(p.getLocationType())) continue;
                filtered.add(p);
            }
            recycler.setAdapter(new PhotoPublicationAdapter(filtered, this::onDeletePhoto, this::onEditPhoto));
            setEmpty(filtered.isEmpty());

        } else if (currentTab == 1) {
            List<TravelPath> filtered = new ArrayList<>();
            for (TravelPath p : allPaths) {
                if (activeVisibility != null && !matchesVisibility(p.getVisibility(), p.getIsPublic(), activeVisibility)) continue;
                if (activeType != null && !activeType.equals(p.getType())) continue;
                filtered.add(p);
            }
            recycler.setAdapter(new PathPublicationAdapter(filtered, this::onDeletePath));
            setEmpty(filtered.isEmpty());
        }
    }

    /** Ouvre le dialog de filtre selon l'onglet actif. */
    private void showFilterDialog() {
        if (getContext() == null) return;

        int dp8  = (int) (8  * getResources().getDisplayMetrics().density);
        int dp16 = (int) (16 * getResources().getDisplayMetrics().density);

        ScrollView scroll = new ScrollView(requireContext());
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp16, dp16, dp16, dp16);
        scroll.addView(root);

        // --- Section Visibilité ---
        TextView labelVis = new TextView(requireContext());
        labelVis.setText("Visibilité");
        labelVis.setTextSize(14f);
        labelVis.setTextColor(0xFF5B5CF6);
        labelVis.setPadding(0, 0, 0, dp8);
        labelVis.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(labelVis);

        RadioGroup rgVis = new RadioGroup(requireContext());
        rgVis.setOrientation(RadioGroup.VERTICAL);

        String[] visLabels = {"Toutes", "Publique", "Privée", "Planifiée"};
        String[] visValues = {null, "public", "private", "scheduled"};
        int checkedVisId = 0;
        for (int i = 0; i < visLabels.length; i++) {
            RadioButton rb = new RadioButton(requireContext());
            rb.setId(1000 + i);
            rb.setText(visLabels[i]);
            rb.setPadding(0, dp8 / 2, 0, dp8 / 2);
            if (visValues[i] == null && activeVisibility == null) rb.setChecked(true);
            else if (visValues[i] != null && visValues[i].equals(activeVisibility)) rb.setChecked(true);
            rgVis.addView(rb);
        }
        root.addView(rgVis);

        // --- Section Type ---
        TextView labelType = new TextView(requireContext());
        labelType.setText(currentTab == 0 ? "Type de lieu" : "Type de parcours");
        labelType.setTextSize(14f);
        labelType.setTextColor(0xFF5B5CF6);
        labelType.setTypeface(null, android.graphics.Typeface.BOLD);
        labelType.setPadding(0, dp16, 0, dp8);
        root.addView(labelType);

        RadioGroup rgType = new RadioGroup(requireContext());
        rgType.setOrientation(RadioGroup.VERTICAL);

        String[] typeValues = buildTypeValues();
        RadioButton rbAll = new RadioButton(requireContext());
        rbAll.setId(2000);
        rbAll.setText("Tous");
        rbAll.setPadding(0, dp8 / 2, 0, dp8 / 2);
        if (activeType == null) rbAll.setChecked(true);
        rgType.addView(rbAll);

        // Pour les photos : labels ≠ valeurs stockées → utiliser PHOTO_TYPE_LABELS pour l'affichage
        String[] typeLabels = (currentTab == 0) ? PHOTO_TYPE_LABELS : typeValues;
        for (int i = 0; i < typeValues.length; i++) {
            RadioButton rb = new RadioButton(requireContext());
            rb.setId(2001 + i);
            rb.setText(typeLabels[i]);
            rb.setPadding(0, dp8 / 2, 0, dp8 / 2);
            if (typeValues[i].equals(activeType)) rb.setChecked(true);
            rgType.addView(rb);
        }
        root.addView(rgType);

        new AlertDialog.Builder(requireContext())
                .setTitle("Filtrer")
                .setView(scroll)
                .setPositiveButton("Appliquer", (d, w) -> {
                    // Lire visibilité
                    int visChecked = rgVis.getCheckedRadioButtonId() - 1000;
                    activeVisibility = (visChecked >= 0 && visChecked < visValues.length)
                            ? visValues[visChecked] : null;

                    // Lire type
                    int typeChecked = rgType.getCheckedRadioButtonId();
                    if (typeChecked == 2000) {
                        activeType = null;
                    } else {
                        int idx = typeChecked - 2001;
                        activeType = (idx >= 0 && idx < typeValues.length) ? typeValues[idx] : null;
                    }

                    updateFilterButtonVisibility();
                    applyAndDisplay();
                })
                .setNegativeButton("Réinitialiser", (d, w) -> {
                    activeVisibility = null;
                    activeType = null;
                    updateFilterButtonVisibility();
                    applyAndDisplay();
                })
                .setNeutralButton("Annuler", null)
                .show();
    }

    /**
     * Comparaison robuste de visibilité : gère les anciens documents où visibility
     * est null mais isPublic=true (publiés avant l'ajout du champ visibility).
     */
    private boolean matchesVisibility(String visibility, boolean isPublic, String filter) {
        switch (filter) {
            case "public":
                return "public".equals(visibility) || (visibility == null && isPublic);
            case "private":
                return "private".equals(visibility) || (visibility == null && !isPublic);
            default:
                return filter.equals(visibility);
        }
    }

    /** Retourne les valeurs de type disponibles selon l'onglet. */
    private String[] buildTypeValues() {
        if (currentTab == 0) {
            return PHOTO_TYPE_VALUES;
        } else {
            // Types extraits dynamiquement des parcours chargés
            Set<String> types = new LinkedHashSet<>();
            for (TravelPath p : allPaths) {
                if (p.getType() != null && !p.getType().isEmpty()) types.add(p.getType());
            }
            return types.toArray(new String[0]);
        }
    }

    private void sortPhotos(List<Photo> photos) {
        Collections.sort(photos, (a, b) -> {
            Timestamp ta = a.getCreatedAt();
            Timestamp tb = b.getCreatedAt();
            if (ta == null && tb == null) return 0;
            if (ta == null) return 1;
            if (tb == null) return -1;
            return tb.compareTo(ta);
        });
    }

    private void sortPaths(List<TravelPath> paths) {
        Collections.sort(paths, (a, b) -> {
            java.util.Date da = a.getCreatedAt();
            java.util.Date db = b.getCreatedAt();
            if (da == null && db == null) return 0;
            if (da == null) return 1;
            if (db == null) return -1;
            return db.compareTo(da);
        });
    }

    private void setEmpty(boolean empty) {
        recycler.setVisibility(empty ? View.GONE : View.VISIBLE);
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void onEditPhoto(Photo photo) {
        PhotoRegistry.set(photo);
        Navigation.findNavController(requireView())
                .navigate(R.id.action_publications_to_share_photo);
    }

    private void onDeletePhoto(Photo photo) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Supprimer")
                .setMessage("Supprimer \"" + photo.getTitle() + "\" ?")
                .setPositiveButton("Supprimer", (d, w) -> {
                    if (photo.getId() != null)
                        FirestoreRepository.get().deletePhoto(photo.getId(), v -> loadData());
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void onDeletePath(TravelPath path) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Supprimer")
                .setMessage("Supprimer \"" + path.getTitle() + "\" ?")
                .setPositiveButton("Supprimer", (d, w) -> {
                    if (path.getId() != null)
                        FirestoreRepository.get().deletePath(path.getId(), v -> loadData());
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    // ---- Adapter photos ----
    static class PhotoPublicationAdapter extends RecyclerView.Adapter<PublicationVH> {
        interface OnDelete { void onDelete(Photo p); }
        interface OnEdit   { void onEdit(Photo p); }

        private final List<Photo> items;
        private final OnDelete deleteListener;
        private final OnEdit editListener;

        PhotoPublicationAdapter(List<Photo> items, OnDelete del, OnEdit edit) {
            this.items = items;
            this.deleteListener = del;
            this.editListener = edit;
        }

        @NonNull @Override
        public PublicationVH onCreateViewHolder(@NonNull ViewGroup p, int v) {
            return new PublicationVH(LayoutInflater.from(p.getContext())
                    .inflate(R.layout.item_publication, p, false));
        }

        @Override
        public void onBindViewHolder(@NonNull PublicationVH h, int pos) {
            Photo photo = items.get(pos);
            if (photo.getImageBitmap() != null)      h.image.setImageBitmap(photo.getImageBitmap());
            else if (photo.getImageUri() != null)    h.image.setImageURI(photo.getImageUri());
            else if (photo.getImageResId() != 0)     h.image.setImageResource(photo.getImageResId());
            h.title.setText(photo.getTitle());
            h.subtitle.setText(photo.getLocationName());
            h.date.setText(photo.getDate());
            h.edit.setOnClickListener(v -> editListener.onEdit(photo));
            h.delete.setOnClickListener(v -> deleteListener.onDelete(photo));
            h.itemView.setOnClickListener(v -> editListener.onEdit(photo));
        }

        @Override public int getItemCount() { return items.size(); }
    }

    // ---- Adapter parcours ----
    static class PathPublicationAdapter extends RecyclerView.Adapter<PublicationVH> {
        interface OnDelete { void onDelete(TravelPath p); }
        private final List<TravelPath> items;
        private final OnDelete listener;
        PathPublicationAdapter(List<TravelPath> items, OnDelete l) {
            this.items = items; this.listener = l;
        }

        @NonNull @Override
        public PublicationVH onCreateViewHolder(@NonNull ViewGroup p, int v) {
            return new PublicationVH(LayoutInflater.from(p.getContext())
                    .inflate(R.layout.item_publication, p, false));
        }

        @Override
        public void onBindViewHolder(@NonNull PublicationVH h, int pos) {
            TravelPath path = items.get(pos);
            if (path.getImageResId() != 0) h.image.setImageResource(path.getImageResId());
            h.title.setText(path.getTitle());
            h.subtitle.setText(path.getCity() + " · " + path.getDuration());
            h.date.setText(path.getBudget() + " · " + path.getDifficulty());
            h.edit.setVisibility(View.GONE);
            h.delete.setOnClickListener(v -> listener.onDelete(path));
        }

        @Override public int getItemCount() { return items.size(); }
    }

    // ---- ViewHolder partagé ----
    static class PublicationVH extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title, subtitle, date;
        ImageButton edit, delete;

        PublicationVH(@NonNull View v) {
            super(v);
            image    = v.findViewById(R.id.pub_image);
            title    = v.findViewById(R.id.pub_title);
            subtitle = v.findViewById(R.id.pub_subtitle);
            date     = v.findViewById(R.id.pub_date);
            edit     = v.findViewById(R.id.btn_edit);
            delete   = v.findViewById(R.id.btn_delete);
        }
    }
}
