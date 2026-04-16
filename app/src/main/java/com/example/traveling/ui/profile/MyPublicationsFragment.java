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
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.traveling.R;
import com.example.traveling.data.PhotoRegistry;
import com.example.traveling.data.UserRepository;
import com.example.traveling.model.Photo;
import com.example.traveling.model.TravelPath;
import com.google.android.material.tabs.TabLayout;

import java.util.List;

public class MyPublicationsFragment extends Fragment {

    private RecyclerView recycler;
    private TextView tvEmpty;
    private TabLayout tabLayout;
    private boolean showingPhotos = true;

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
        recycler = view.findViewById(R.id.recycler_publications);
        tvEmpty = view.findViewById(R.id.tv_empty);

        view.findViewById(R.id.btn_back).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());

        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));

        tabLayout.addTab(tabLayout.newTab().setText("Photos"));
        tabLayout.addTab(tabLayout.newTab().setText("Parcours"));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                showingPhotos = tab.getPosition() == 0;
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
        if (showingPhotos) {
            List<Photo> photos = UserRepository.get().getMyPhotos();
            PhotoPublicationAdapter adapter = new PhotoPublicationAdapter(photos, this::onDeletePhoto);
            adapter.setOnClickListener(photo -> {
                PhotoRegistry.set(photo);
                Navigation.findNavController(requireView()).navigate(R.id.navigation_photo_detail);
            });
            recycler.setAdapter(adapter);
            setEmpty(photos.isEmpty());
        } else {
            List<TravelPath> paths = UserRepository.get().getMyPaths();
            recycler.setAdapter(new PathPublicationAdapter(paths, this::onDeletePath));
            setEmpty(paths.isEmpty());
        }
    }

    private void setEmpty(boolean empty) {
        recycler.setVisibility(empty ? View.GONE : View.VISIBLE);
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void onDeletePhoto(Photo photo) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Supprimer")
                .setMessage("Supprimer \"" + photo.getTitle() + "\" ?")
                .setPositiveButton("Supprimer", (d, w) -> {
                    UserRepository.get().removePhoto(photo);
                    refreshList();
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void onDeletePath(TravelPath path) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Supprimer")
                .setMessage("Supprimer \"" + path.getTitle() + "\" ?")
                .setPositiveButton("Supprimer", (d, w) -> {
                    UserRepository.get().removePath(path);
                    refreshList();
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    // ---- Adapter photos ----
    static class PhotoPublicationAdapter
            extends RecyclerView.Adapter<PublicationVH> {
        interface OnDelete { void onDelete(Photo p); }
        interface OnClick { void onClick(Photo p); }
        private final List<Photo> items;
        private final OnDelete listener;
        private OnClick clickListener;
        PhotoPublicationAdapter(List<Photo> items, OnDelete l) { this.items = items; this.listener = l; }
        void setOnClickListener(OnClick c) { this.clickListener = c; }

        @NonNull @Override
        public PublicationVH onCreateViewHolder(@NonNull ViewGroup p, int v) {
            return new PublicationVH(LayoutInflater.from(p.getContext())
                    .inflate(R.layout.item_publication, p, false));
        }
        @Override public void onBindViewHolder(@NonNull PublicationVH h, int pos) {
            Photo photo = items.get(pos);
            if (photo.getImageBitmap() != null) h.image.setImageBitmap(photo.getImageBitmap());
            else if (photo.getImageUri() != null) h.image.setImageURI(photo.getImageUri());
            else if (photo.getImageResId() != 0) h.image.setImageResource(photo.getImageResId());
            h.title.setText(photo.getTitle());
            h.subtitle.setText(photo.getLocationName());
            h.date.setText(photo.getDate());
            h.delete.setOnClickListener(v -> listener.onDelete(photo));
            h.itemView.setOnClickListener(v -> { if (clickListener != null) clickListener.onClick(photo); });
        }
        @Override public int getItemCount() { return items.size(); }
    }

    // ---- Adapter parcours ----
    static class PathPublicationAdapter
            extends RecyclerView.Adapter<PublicationVH> {
        interface OnDelete { void onDelete(TravelPath p); }
        private final List<TravelPath> items;
        private final OnDelete listener;
        PathPublicationAdapter(List<TravelPath> items, OnDelete l) { this.items = items; this.listener = l; }

        @NonNull @Override
        public PublicationVH onCreateViewHolder(@NonNull ViewGroup p, int v) {
            return new PublicationVH(LayoutInflater.from(p.getContext())
                    .inflate(R.layout.item_publication, p, false));
        }
        @Override public void onBindViewHolder(@NonNull PublicationVH h, int pos) {
            TravelPath path = items.get(pos);
            h.image.setImageResource(path.getImageResId());
            h.title.setText(path.getTitle());
            h.subtitle.setText(path.getCity() + " · " + path.getDuration());
            h.date.setText(path.getBudget() + " · " + path.getDifficulty());
            h.delete.setOnClickListener(v -> listener.onDelete(path));
        }
        @Override public int getItemCount() { return items.size(); }
    }

    // ---- ViewHolder partagé ----
    static class PublicationVH extends RecyclerView.ViewHolder {
        ImageView image; TextView title, subtitle, date; ImageButton delete;
        PublicationVH(@NonNull View v) {
            super(v);
            image = v.findViewById(R.id.pub_image);
            title = v.findViewById(R.id.pub_title);
            subtitle = v.findViewById(R.id.pub_subtitle);
            date = v.findViewById(R.id.pub_date);
            delete = v.findViewById(R.id.btn_delete);
        }
    }
}
