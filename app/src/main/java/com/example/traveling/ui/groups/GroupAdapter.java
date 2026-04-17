package com.example.traveling.ui.groups;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.traveling.R;
import com.example.traveling.model.Group;

import java.util.ArrayList;
import java.util.List;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.VH> {

    public interface Listener {
        void onGroupClick(Group group);
    }

    private List<Group> groups = new ArrayList<>();
    private List<Group> allGroups = new ArrayList<>();
    private final Listener listener;

    public GroupAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setGroups(List<Group> groups) {
        this.allGroups = groups;
        this.groups = new ArrayList<>(groups);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        if (query == null || query.trim().isEmpty()) {
            groups = new ArrayList<>(allGroups);
        } else {
            String lower = query.toLowerCase();
            groups = new ArrayList<>();
            for (Group g : allGroups) {
                if (g.getName().toLowerCase().contains(lower)) {
                    groups.add(g);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_group, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Group g = groups.get(position);
        h.image.setImageResource(g.getImageResId());
        h.name.setText(g.getName());
        h.lastMessage.setText(g.getLastMessage());
        h.time.setText(g.getLastMessageTime());

        if (g.getUnreadCount() > 0) {
            h.badge.setVisibility(View.VISIBLE);
            h.badge.setText(String.valueOf(g.getUnreadCount()));
        } else {
            h.badge.setVisibility(View.GONE);
        }

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onGroupClick(g);
        });
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView image;
        TextView name, lastMessage, time, badge;

        VH(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.group_image);
            name = itemView.findViewById(R.id.group_name);
            lastMessage = itemView.findViewById(R.id.group_last_message);
            time = itemView.findViewById(R.id.group_time);
            badge = itemView.findViewById(R.id.group_unread_badge);
        }
    }
}
