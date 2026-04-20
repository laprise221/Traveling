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
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder> {

    private List<Photo> photos = new ArrayList<>();
    private OnPhotoClickListener listener;

    public interface OnPhotoClickListener {
        void onPhotoClick(Photo photo);
        void onLikeClick(Photo photo, int position);
        void onFavoriteClick(Photo photo, int position);
    }

    public PhotoAdapter(OnPhotoClickListener listener) {
        this.listener = listener;
    }

    public void setPhotos(List<Photo> photos) {
        this.photos = photos;
        notifyDataSetChanged();
    }

    public void filterByQuery(List<Photo> allPhotos, String query) {
        if (query == null || query.isEmpty()) {
            this.photos = new ArrayList<>(allPhotos);
        } else {
            String lower = query.toLowerCase();
            this.photos = new ArrayList<>();
            for (Photo p : allPhotos) {
                if (p.getTitle().toLowerCase().contains(lower)
                        || p.getLocationName().toLowerCase().contains(lower)
                        || p.getAuthor().toLowerCase().contains(lower)
                        || p.getLocationType().toLowerCase().contains(lower)
                        || p.getDescription().toLowerCase().contains(lower)) {
                    this.photos.add(p);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_photo, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        Photo photo = photos.get(position);
        holder.bind(photo, position);
    }

    @Override
    public int getItemCount() {
        return photos.size();
    }

    class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView photoImage;
        TextView photoTitle, photoLocation, photoDescription, photoAuthor, photoDate, likeCount;
        Chip typeChip;
        ImageButton btnLike, btnFavorite;

        PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            photoImage = itemView.findViewById(R.id.photo_image);
            photoTitle = itemView.findViewById(R.id.photo_title);
            photoLocation = itemView.findViewById(R.id.photo_location);
            photoDescription = itemView.findViewById(R.id.photo_description);
            photoAuthor = itemView.findViewById(R.id.photo_author);
            photoDate = itemView.findViewById(R.id.photo_date);
            likeCount = itemView.findViewById(R.id.photo_like_count);
            typeChip = itemView.findViewById(R.id.photo_type_chip);
            btnLike = itemView.findViewById(R.id.btn_like_photo);
            btnFavorite = itemView.findViewById(R.id.btn_favorite_photo);
        }

        void bind(Photo photo, int position) {
            if (photo.getImageBitmap() != null) photoImage.setImageBitmap(photo.getImageBitmap());
            else if (photo.getImageUri() != null) photoImage.setImageURI(photo.getImageUri());
            else photoImage.setImageResource(photo.getImageResId());
            photoTitle.setText(photo.getTitle());
            photoLocation.setText(photo.getLocationName());
            photoDescription.setText(photo.getDescription());
            photoAuthor.setText(photo.getAuthor());
            photoDate.setText(photo.getDate());
            likeCount.setText(String.valueOf(photo.getLikeCount()));
            typeChip.setText(photo.getLocationType());

            btnLike.setImageResource(photo.isLiked()
                    ? R.drawable.ic_thumb_up_filled
                    : R.drawable.ic_thumb_up);
            btnFavorite.setImageResource(photo.isFavorited()
                    ? R.drawable.ic_favorite_filled
                    : R.drawable.ic_favorite);

            btnLike.setOnClickListener(v -> {
                if (listener != null) listener.onLikeClick(photo, getBindingAdapterPosition());
            });
            btnFavorite.setOnClickListener(v -> {
                if (listener != null) listener.onFavoriteClick(photo, getBindingAdapterPosition());
            });

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onPhotoClick(photo);
            });
        }
    }
}
