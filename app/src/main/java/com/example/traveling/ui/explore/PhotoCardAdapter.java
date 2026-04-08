package com.example.traveling.ui.explore;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.traveling.R;
import com.example.traveling.model.Photo;

import java.util.ArrayList;
import java.util.List;

public class PhotoCardAdapter extends RecyclerView.Adapter<PhotoCardAdapter.VH> {

    public interface Listener {
        void onPhotoClick(Photo photo);
        void onLikeClick(Photo photo, int position);
    }

    private List<Photo> photos = new ArrayList<>();
    private final Listener listener;

    public PhotoCardAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setPhotos(List<Photo> photos) {
        this.photos = photos;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_explore_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Photo p = photos.get(position);
        h.image.setImageResource(p.getImageResId());
        h.title.setText(p.getTitle());
        h.likes.setText(p.getLikeCount() + " \u2665");
        h.like.setImageResource(p.isLiked()
                ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite);
        h.like.setOnClickListener(v -> {
            if (listener != null) listener.onLikeClick(p, h.getBindingAdapterPosition());
        });
        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onPhotoClick(p);
        });
    }

    @Override
    public int getItemCount() {
        return photos.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title, likes;
        ImageButton like;

        VH(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.card_image);
            title = itemView.findViewById(R.id.card_title);
            likes = itemView.findViewById(R.id.card_likes);
            like = itemView.findViewById(R.id.card_like);
        }
    }
}
