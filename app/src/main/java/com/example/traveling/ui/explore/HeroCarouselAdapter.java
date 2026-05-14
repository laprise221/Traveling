package com.example.traveling.ui.explore;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.traveling.R;

import java.util.List;

public class HeroCarouselAdapter extends RecyclerView.Adapter<HeroCarouselAdapter.VH> {

    private final List<Integer> imageResIds;

    public HeroCarouselAdapter(List<Integer> imageResIds) {
        this.imageResIds = imageResIds;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ImageView iv = new ImageView(parent.getContext());
        iv.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        return new VH(iv);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.imageView.setImageResource(imageResIds.get(position));
    }

    @Override
    public int getItemCount() {
        return imageResIds.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView imageView;
        VH(ImageView v) {
            super(v);
            imageView = v;
        }
    }
}
