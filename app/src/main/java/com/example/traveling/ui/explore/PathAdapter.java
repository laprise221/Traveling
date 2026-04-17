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
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class PathAdapter extends RecyclerView.Adapter<PathAdapter.PathViewHolder> {

    private List<TravelPath> paths = new ArrayList<>();
    private OnPathClickListener listener;

    public interface OnPathClickListener {
        void onPathClick(TravelPath path);
        void onLikeClick(TravelPath path, int position);
    }

    public PathAdapter(OnPathClickListener listener) {
        this.listener = listener;
    }

    public void setPaths(List<TravelPath> paths) {
        this.paths = paths;
        notifyDataSetChanged();
    }

    public void filterByQuery(List<TravelPath> allPaths, String query) {
        if (query == null || query.isEmpty()) {
            this.paths = new ArrayList<>(allPaths);
        } else {
            String lower = query.toLowerCase();
            this.paths = new ArrayList<>();
            for (TravelPath p : allPaths) {
                if (p.getTitle().toLowerCase().contains(lower)
                        || p.getCity().toLowerCase().contains(lower)
                        || p.getAuthor().toLowerCase().contains(lower)
                        || p.getDescription().toLowerCase().contains(lower)
                        || p.getType().toLowerCase().contains(lower)) {
                    this.paths.add(p);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PathViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_path, parent, false);
        return new PathViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PathViewHolder holder, int position) {
        TravelPath path = paths.get(position);
        holder.bind(path, position);
    }

    @Override
    public int getItemCount() {
        return paths.size();
    }

    class PathViewHolder extends RecyclerView.ViewHolder {
        ImageView pathImage;
        TextView pathTitle, pathCity, pathDescription, pathDuration;
        TextView pathBudget, pathDifficulty, pathStepsCount, pathAuthor, likeCount;
        Chip typeChip;
        ImageButton btnLike;

        PathViewHolder(@NonNull View itemView) {
            super(itemView);
            pathImage = itemView.findViewById(R.id.path_image);
            pathTitle = itemView.findViewById(R.id.path_title);
            pathCity = itemView.findViewById(R.id.path_city);
            pathDescription = itemView.findViewById(R.id.path_description);
            pathDuration = itemView.findViewById(R.id.path_duration);
            pathBudget = itemView.findViewById(R.id.path_budget);
            pathDifficulty = itemView.findViewById(R.id.path_difficulty);
            pathStepsCount = itemView.findViewById(R.id.path_steps_count);
            pathAuthor = itemView.findViewById(R.id.path_author);
            likeCount = itemView.findViewById(R.id.path_like_count);
            typeChip = itemView.findViewById(R.id.path_type_chip);
            btnLike = itemView.findViewById(R.id.btn_like_path);
        }

        void bind(TravelPath path, int position) {
            if (path.getImageResId() != 0) pathImage.setImageResource(path.getImageResId());
            else pathImage.setImageResource(R.drawable.sample_path_1); // fallback
            pathTitle.setText(path.getTitle());
            pathCity.setText(path.getCity());
            pathDescription.setText(path.getDescription());
            pathDuration.setText(path.getDuration());
            pathBudget.setText(path.getBudget());
            pathDifficulty.setText(path.getDifficulty());
            pathStepsCount.setText(String.valueOf(path.getStepsCount()));
            pathAuthor.setText(path.getAuthor());
            likeCount.setText(String.valueOf(path.getLikeCount()));
            typeChip.setText(path.getType());

            btnLike.setImageResource(path.isLiked()
                    ? R.drawable.ic_favorite_filled
                    : R.drawable.ic_favorite);

            btnLike.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onLikeClick(path, position);
                }
            });

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPathClick(path);
                }
            });
        }
    }
}
