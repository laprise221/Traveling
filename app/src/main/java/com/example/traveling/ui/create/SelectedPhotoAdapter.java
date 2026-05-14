package com.example.traveling.ui.create;

import android.graphics.Bitmap;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.traveling.R;

import java.util.List;

public class SelectedPhotoAdapter extends RecyclerView.Adapter<SelectedPhotoAdapter.VH> {

    public interface OnRemoveListener {
        void onRemove(int position);
    }

    private final List<Object> images;
    private final OnRemoveListener removeListener;

    public SelectedPhotoAdapter(List<Object> images, OnRemoveListener removeListener) {
        this.images = images;
        this.removeListener = removeListener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_selected_photo, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Object img = images.get(position);
        if (img instanceof Bitmap) {
            holder.thumbnail.setImageBitmap((Bitmap) img);
        } else if (img instanceof Uri) {
            holder.thumbnail.setImageURI((Uri) img);
        }
        holder.btnRemove.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_ID && removeListener != null) {
                removeListener.onRemove(pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView thumbnail;
        final ImageButton btnRemove;

        VH(@NonNull View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.img_thumbnail);
            btnRemove = itemView.findViewById(R.id.btn_remove);
        }
    }
}
