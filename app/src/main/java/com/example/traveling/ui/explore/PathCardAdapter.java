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
import com.example.traveling.model.TravelPath;

import java.util.ArrayList;
import java.util.List;

public class PathCardAdapter extends RecyclerView.Adapter<PathCardAdapter.VH> {

    public interface Listener {
        void onPathClick(TravelPath path);
        void onLikeClick(TravelPath path, int position);
        void onFavoriteClick(TravelPath path, int position);
    }

    private List<TravelPath> paths = new ArrayList<>();
    private final Listener listener;

    public PathCardAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setPaths(List<TravelPath> paths) {
        this.paths = paths;
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
        TravelPath p = paths.get(position);
        if (p.getImageResId() != 0) h.image.setImageResource(p.getImageResId());
        else h.image.setImageResource(R.drawable.sample_path_1); // fallback
        h.title.setText(p.getTitle());
        h.likes.setText(p.getLikeCount() + " \uD83D\uDC4D");
        h.like.setImageResource(p.isLiked()
                ? R.drawable.ic_thumb_up_filled : R.drawable.ic_thumb_up);
        h.favorite.setImageResource(p.isFavorited()
                ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite);
        h.like.setOnClickListener(v -> {
            if (listener != null) listener.onLikeClick(p, h.getBindingAdapterPosition());
        });
        h.favorite.setOnClickListener(v -> {
            if (listener != null) listener.onFavoriteClick(p, h.getBindingAdapterPosition());
        });
        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onPathClick(p);
        });
    }

    @Override
    public int getItemCount() {
        return paths.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title, likes;
        ImageButton like, favorite;

        VH(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.card_image);
            title = itemView.findViewById(R.id.card_title);
            likes = itemView.findViewById(R.id.card_likes);
            like = itemView.findViewById(R.id.card_like);
            favorite = itemView.findViewById(R.id.card_favorite);
        }
    }
}
