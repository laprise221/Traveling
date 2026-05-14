package com.example.traveling.ui.explore;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.traveling.R;
import com.example.traveling.data.ImageUtils;

import java.util.ArrayList;
import java.util.List;

public class PhotoCarouselAdapter extends RecyclerView.Adapter<PhotoCarouselAdapter.ImageVH> {

    private final List<Object> images = new ArrayList<>();

    public void setImages(List<Object> items) {
        images.clear();
        images.addAll(items);
        notifyDataSetChanged();
    }

    public void setBase64Images(List<String> base64List) {
        images.clear();
        images.addAll(base64List);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ImageVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_carousel_image, parent, false);
        return new ImageVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageVH holder, int position) {
        Object img = images.get(position);
        if (img instanceof Bitmap) {
            holder.imageView.setImageBitmap((Bitmap) img);
        } else if (img instanceof Uri) {
            holder.imageView.setImageURI((Uri) img);
        } else if (img instanceof String) {
            Bitmap bmp = ImageUtils.base64ToBitmap((String) img);
            if (bmp != null) holder.imageView.setImageBitmap(bmp);
        }
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    static class ImageVH extends RecyclerView.ViewHolder {
        final ImageView imageView;

        ImageVH(@NonNull View itemView) {
            super(itemView);
            imageView = (ImageView) itemView;
        }
    }
}
