package com.example.traveling.ui.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.traveling.R;
import com.example.traveling.data.FirestoreRepository;
import com.example.traveling.data.NotificationRepository;
import com.example.traveling.data.PathRegistry;
import com.example.traveling.data.PhotoRegistry;
import com.example.traveling.model.AppNotification;
import com.example.traveling.model.Group;
import com.example.traveling.model.Photo;
import com.example.traveling.model.TravelPath;
import com.example.traveling.ui.groups.GroupRegistry;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class NotificationsFragment extends Fragment {

    private NotificationAdapter adapter;
    private RecyclerView recycler;
    private TextView tvEmpty;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btn_back).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());

        recycler = view.findViewById(R.id.recycler_notifications);
        tvEmpty = view.findViewById(R.id.tv_empty_notifications);

        adapter = new NotificationAdapter(this::onNotifClick);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.addItemDecoration(
                new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
        recycler.setAdapter(adapter);

        MaterialButton btnMarkAll = view.findViewById(R.id.btn_mark_all_read);
        btnMarkAll.setOnClickListener(v ->
                NotificationRepository.get().markAllAsRead(() -> {
                    if (!isAdded()) return;
                    adapter.markAllRead();
                }));

        loadNotifications();
    }

    private void loadNotifications() {
        NotificationRepository.get().loadNotifications(list -> {
            if (!isAdded()) return;
            adapter.setItems(list);
            boolean empty = list.isEmpty();
            recycler.setVisibility(empty ? View.GONE : View.VISIBLE);
            tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        });
    }

    private void onNotifClick(AppNotification notif) {
        NotificationRepository.get().markAsRead(notif.getId());
        notif.setRead(true);
        adapter.notifyDataSetChanged();

        String contentId = notif.getContentId();
        String contentType = notif.getContentType();

        if ("photo".equals(contentType) && contentId != null && !contentId.isEmpty()) {
            FirebaseFirestore.getInstance().collection("photos").document(contentId).get()
                    .addOnSuccessListener(doc -> {
                        if (!isAdded() || !doc.exists()) return;
                        Photo photo = doc.toObject(Photo.class);
                        if (photo != null) {
                            photo.setId(doc.getId());
                            if (photo.getImageBase64() != null) {
                                photo.setImageBitmap(
                                        com.example.traveling.data.ImageUtils.base64ToBitmap(
                                                photo.getImageBase64()));
                            }
                            PhotoRegistry.set(photo);
                            Navigation.findNavController(requireView())
                                    .navigate(R.id.navigation_photo_detail);
                        }
                    });
        } else if ("path".equals(contentType) && contentId != null && !contentId.isEmpty()) {
            FirebaseFirestore.getInstance().collection("paths").document(contentId).get()
                    .addOnSuccessListener(doc -> {
                        if (!isAdded() || !doc.exists()) return;
                        TravelPath path = doc.toObject(TravelPath.class);
                        if (path != null) {
                            path.setId(doc.getId());
                            PathRegistry.set(path);
                            Navigation.findNavController(requireView())
                                    .navigate(R.id.navigation_path_detail);
                        }
                    });
        } else if ("group".equals(contentType) && contentId != null && !contentId.isEmpty()) {
            FirebaseFirestore.getInstance().collection("groups").document(contentId).get()
                    .addOnSuccessListener(doc -> {
                        if (!isAdded() || !doc.exists()) return;
                        Group group = doc.toObject(Group.class);
                        if (group != null) {
                            group.setId(doc.getId());
                            GroupRegistry.set(group);
                            Navigation.findNavController(requireView())
                                    .navigate(R.id.navigation_group_detail);
                        }
                    });
        }
    }

    // ===================== ADAPTER =====================

    interface OnClick { void onClick(AppNotification n); }

    static class NotificationAdapter extends RecyclerView.Adapter<NotifVH> {
        private List<AppNotification> items = new ArrayList<>();
        private final OnClick listener;

        NotificationAdapter(OnClick listener) { this.listener = listener; }

        void setItems(List<AppNotification> list) {
            this.items = list;
            notifyDataSetChanged();
        }

        void markAllRead() {
            for (AppNotification n : items) n.setRead(true);
            notifyDataSetChanged();
        }

        @NonNull @Override
        public NotifVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_notification, parent, false);
            return new NotifVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull NotifVH h, int pos) {
            AppNotification n = items.get(pos);

            h.root.setBackgroundColor(n.isRead()
                    ? 0xFFFFFFFF
                    : 0xFFE3F2FD);

            h.text.setText(n.buildText());

            h.unreadDot.setVisibility(n.isRead() ? View.GONE : View.VISIBLE);

            if (n.getCreatedAt() != null) {
                String time = new SimpleDateFormat("dd MMM, HH:mm", Locale.FRENCH)
                        .format(n.getCreatedAt());
                h.time.setText(time);
            } else {
                h.time.setText("");
            }

            switch (n.getType() != null ? n.getType() : "") {
                case "like":
                    h.icon.setImageResource(R.drawable.ic_thumb_up_filled);
                    break;
                case "favorite":
                    h.icon.setImageResource(R.drawable.ic_favorite_filled);
                    break;
                case "comment":
                    h.icon.setImageResource(android.R.drawable.ic_dialog_info);
                    break;
                case "group_post":
                    h.icon.setImageResource(android.R.drawable.ic_menu_share);
                    break;
                default:
                    h.icon.setImageResource(android.R.drawable.ic_popup_reminder);
            }

            h.itemView.setOnClickListener(v -> { if (listener != null) listener.onClick(n); });
        }

        @Override public int getItemCount() { return items.size(); }
    }

    static class NotifVH extends RecyclerView.ViewHolder {
        View root, unreadDot;
        android.widget.ImageView icon;
        TextView text, time;

        NotifVH(@NonNull View v) {
            super(v);
            root = v.findViewById(R.id.notif_root);
            icon = v.findViewById(R.id.notif_icon);
            text = v.findViewById(R.id.notif_text);
            time = v.findViewById(R.id.notif_time);
            unreadDot = v.findViewById(R.id.notif_unread_dot);
        }
    }
}
