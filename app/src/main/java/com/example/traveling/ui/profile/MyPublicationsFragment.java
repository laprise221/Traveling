package com.example.traveling.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
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
import java.util.List;

public class MyPublicationsFragment extends Fragment {

    private RecyclerView recycler;
    private TextView tvEmpty;
    private TabLayout tabLayout;
    private int currentTab = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_publications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tabLayout = view.findViewById(R.id.tab_layout_publications);
        recycler  = view.findViewById(R.id.recycler_publications);
        tvEmpty   = view.findViewById(R.id.tv_empty);

        view.findViewById(R.id.btn_back).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());

        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));

        tabLayout.addTab(tabLayout.newTab().setText("Photos"));
        tabLayout.addTab(tabLayout.newTab().setText("Parcours"));
        tabLayout.addTab(tabLayout.newTab().setText("Brouillons"));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                refreshList();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        refreshList();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshList();
    }

    private void refreshList() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { setEmpty(true); return; }
        String uid = user.getUid();

        if (currentTab == 0) {
            // Photos publiées / privées (hors brouillons)
            FirestoreRepository.get().loadUserPhotos(uid, photos -> {
                if (!isAdded()) return;
                List<Photo> published = new ArrayList<>();
                for (Photo p : photos) {
                    if (!"draft".equals(p.getVisibility())) published.add(p);
                }
                sortPhotos(published);
                PhotoPublicationAdapter adapter =
                        new PhotoPublicationAdapter(published, this::onDeletePhoto, this::onEditPhoto);
                recycler.setAdapter(adapter);
                setEmpty(published.isEmpty());
            });

        } else if (currentTab == 1) {
            // Parcours
            FirestoreRepository.get().loadUserPaths(uid, paths -> {
                if (!isAdded()) return;
                sortPaths(paths);
                recycler.setAdapter(new PathPublicationAdapter(paths, this::onDeletePath));
                setEmpty(paths.isEmpty());
            });

        } else {
            // Brouillons
            FirestoreRepository.get().loadUserPhotos(uid, photos -> {
                if (!isAdded()) return;
                List<Photo> drafts = new ArrayList<>();
                for (Photo p : photos) {
                    if ("draft".equals(p.getVisibility())) drafts.add(p);
                }
                sortPhotos(drafts);
                PhotoPublicationAdapter adapter =
                        new PhotoPublicationAdapter(drafts, this::onDeletePhoto, this::onEditPhoto);
                recycler.setAdapter(adapter);
                setEmpty(drafts.isEmpty());
            });
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
                        FirestoreRepository.get().deletePhoto(photo.getId(), v -> refreshList());
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
                        FirestoreRepository.get().deletePath(path.getId(), v -> refreshList());
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
