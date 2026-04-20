package com.example.traveling.ui.groups;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.traveling.R;
import com.example.traveling.model.Group;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.VH> {

    public interface Listener {
        void onGroupClick(Group group);
        default void onJoinClick(Group group) {}
    }

    private List<Group> groups = new ArrayList<>();
    private List<Group> allGroups = new ArrayList<>();
    private final Listener listener;
    private final boolean showJoinButton;

    public GroupAdapter(Listener listener) {
        this(listener, false);
    }

    public GroupAdapter(Listener listener, boolean showJoinButton) {
        this.listener = listener;
        this.showJoinButton = showJoinButton;
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
        if (g.getImageResId() != 0) h.image.setImageResource(g.getImageResId());
        else h.image.setImageResource(android.R.drawable.ic_menu_manage);
        h.name.setText(g.getName());
        String sub = g.getDescription() != null && !g.getDescription().isEmpty()
                ? g.getDescription() : (g.getTheme() != null && !g.getTheme().isEmpty() ? g.getTheme() : "");
        h.lastMessage.setText(sub);
        h.time.setText(g.getMembersCount() > 0 ? g.getMembersCount() + " membre(s)" : "");

        if (g.getUnreadCount() > 0) {
            h.badge.setVisibility(View.VISIBLE);
            h.badge.setText(String.valueOf(g.getUnreadCount()));
        } else {
            h.badge.setVisibility(View.GONE);
        }

        h.joinBtn.setVisibility(showJoinButton ? View.VISIBLE : View.GONE);
        h.joinBtn.setOnClickListener(v -> {
            if (listener != null) listener.onJoinClick(g);
        });

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
        MaterialButton joinBtn;

        VH(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.group_image);
            name = itemView.findViewById(R.id.group_name);
            lastMessage = itemView.findViewById(R.id.group_last_message);
            time = itemView.findViewById(R.id.group_time);
            badge = itemView.findViewById(R.id.group_unread_badge);
            joinBtn = itemView.findViewById(R.id.btn_join_group_item);
        }
    }
}
