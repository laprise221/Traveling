package com.example.traveling.ui.groups;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.traveling.R;
import com.example.traveling.data.GroupRepository;
import com.example.traveling.data.ImageUtils;
import com.example.traveling.data.NotificationRepository;
import com.example.traveling.data.PhotoRegistry;
import com.example.traveling.model.Group;
import com.example.traveling.model.Photo;
import com.example.traveling.session.SessionManager;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GroupDetailFragment extends Fragment {

    private Group group;
    private RecyclerView recycler;
    private TextView tvEmpty;
    private EditText etMessage;
    private PostAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_group_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        group = GroupRegistry.get();
        if (group == null) {
            Navigation.findNavController(view).navigateUp();
            return;
        }

        view.findViewById(R.id.btn_back).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());

        ((TextView) view.findViewById(R.id.tv_group_name)).setText(group.getName());
        ((TextView) view.findViewById(R.id.tv_members_count))
                .setText(group.getMembersCount() + " membre(s)");

        recycler = view.findViewById(R.id.recycler_posts);
        tvEmpty = view.findViewById(R.id.tv_empty_posts);
        etMessage = view.findViewById(R.id.et_message);

        FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
        String myId = me != null ? me.getUid() : "";

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true);
        recycler.setLayoutManager(layoutManager);

        adapter = new PostAdapter(myId, photo -> {
            PhotoRegistry.set(photo);
            Navigation.findNavController(requireView()).navigate(R.id.navigation_photo_detail);
        });
        recycler.setAdapter(adapter);

        view.findViewById(R.id.btn_send).setOnClickListener(v -> sendMessage());

        loadPosts();
    }

    private void sendMessage() {
        if (SessionManager.get().isAnonymous()) {
            Toast.makeText(requireContext(), "Connectez-vous pour envoyer un message",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        String text = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
        String myId = me != null ? me.getUid() : "";
        String myName = (me != null && me.getDisplayName() != null)
                ? me.getDisplayName() : "Quelqu'un";

        etMessage.setText("");

        GroupRepository.get().addGroupPost(group.getId(), null, text, id -> {
            if (!isAdded()) return;
            loadPosts();
            for (String memberId : group.getMemberIds()) {
                if (!memberId.equals(myId)) {
                    NotificationRepository.get().sendNotification(
                            memberId, "group_post", myId, myName,
                            group.getId(), "group", group.getName(), text);
                }
            }
        });
    }

    private void loadPosts() {
        GroupRepository.get().loadGroupPosts(group.getId(), (QuerySnapshot snapshots) -> {
            if (!isAdded()) return;
            if (snapshots == null || snapshots.isEmpty()) {
                recycler.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.VISIBLE);
                return;
            }

            SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());
            SimpleDateFormat dayFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            List<PostItem> posts = new ArrayList<>();

            for (DocumentSnapshot doc : snapshots) {
                PostItem item = new PostItem();
                item.message = doc.getString("message");
                item.authorName = doc.getString("authorName");
                item.authorId = doc.getString("authorId");
                item.photoId = doc.getString("photoId");
                com.google.firebase.Timestamp ts = doc.getTimestamp("createdAt");
                if (ts != null) {
                    Date d = ts.toDate();
                    item.time = timeFmt.format(d);
                    item.dateKey = dayFmt.format(d);
                } else {
                    item.time = "";
                    item.dateKey = "";
                }
                posts.add(item);
            }

            // Collecter les photoIds uniques
            List<String> photoIds = new ArrayList<>();
            for (PostItem item : posts) {
                if (item.photoId != null && !item.photoId.isEmpty()
                        && !photoIds.contains(item.photoId)) {
                    photoIds.add(item.photoId);
                }
            }

            if (photoIds.isEmpty()) {
                showPosts(posts);
                return;
            }

            // Batch query : max 10 par requête (limite Firestore whereIn)
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            List<com.google.android.gms.tasks.Task<QuerySnapshot>> tasks = new ArrayList<>();
            for (int i = 0; i < photoIds.size(); i += 10) {
                List<String> batch = photoIds.subList(i, Math.min(i + 10, photoIds.size()));
                tasks.add(db.collection("photos")
                        .whereIn(FieldPath.documentId(), batch)
                        .get());
            }

            Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
                Map<String, Photo> photoMap = new HashMap<>();
                for (Object result : results) {
                    for (DocumentSnapshot doc : (QuerySnapshot) result) {
                        Photo p = doc.toObject(Photo.class);
                        if (p != null) {
                            p.setId(doc.getId());
                            photoMap.put(doc.getId(), p);
                        }
                    }
                }
                for (PostItem item : posts) {
                    if (item.photoId != null) {
                        Photo p = photoMap.get(item.photoId);
                        if (p != null) {
                            item.photo = p;
                            List<String> images = p.getImages();
                            item.imageBase64 = images.isEmpty() ? null : images.get(0);
                        }
                    }
                }
                if (isAdded()) showPosts(posts);
            }).addOnFailureListener(e -> {
                if (isAdded()) showPosts(posts);
            });
        });
    }

    private void showPosts(List<PostItem> posts) {
        recycler.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        adapter.setItems(buildDisplayList(posts));
        if (!posts.isEmpty()) {
            recycler.scrollToPosition(adapter.getItemCount() - 1);
        }
    }

    /** Insère des séparateurs de date entre les messages de jours différents. */
    private List<ListItem> buildDisplayList(List<PostItem> posts) {
        SimpleDateFormat dayFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat displayFmt = new SimpleDateFormat("EEEE d MMMM yyyy", Locale.FRENCH);
        String today = dayFmt.format(new Date());

        List<ListItem> list = new ArrayList<>();
        String lastDateKey = null;

        for (PostItem post : posts) {
            String key = post.dateKey;
            if (key != null && !key.isEmpty() && !key.equals(lastDateKey)) {
                String label;
                if (key.equals(today)) {
                    label = "Aujourd'hui";
                } else {
                    try {
                        Date d = dayFmt.parse(key);
                        String raw = displayFmt.format(d);
                        label = raw.substring(0, 1).toUpperCase(Locale.FRENCH) + raw.substring(1);
                    } catch (ParseException e) {
                        label = key;
                    }
                }
                list.add(new DateSeparator(label));
                lastDateKey = key;
            }
            list.add(post);
        }
        return list;
    }

    // ─── Types de liste ──────────────────────────────────────────────────────

    interface ListItem {
        int getType();
    }

    static class DateSeparator implements ListItem {
        final String label;
        DateSeparator(String label) { this.label = label; }
        @Override public int getType() { return 0; }
    }

    static class PostItem implements ListItem {
        String authorId, authorName, message, photoId, imageBase64, time, dateKey;
        Photo photo;
        @Override public int getType() { return 1; }
    }

    // ─── Adapter ─────────────────────────────────────────────────────────────

    interface OnPhotoClickListener {
        void onPhotoClick(Photo photo);
    }

    static class PostAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private List<ListItem> items = new ArrayList<>();
        private final String myId;
        private final OnPhotoClickListener photoClickListener;

        PostAdapter(String myId, OnPhotoClickListener listener) {
            this.myId = myId;
            this.photoClickListener = listener;
        }

        void setItems(List<ListItem> items) {
            this.items = items;
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position).getType();
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inf = LayoutInflater.from(parent.getContext());
            if (viewType == 0) {
                View v = inf.inflate(R.layout.item_date_separator, parent, false);
                return new DateVH(v);
            }
            View v = inf.inflate(R.layout.item_chat_message, parent, false);
            return new PostVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int pos) {
            ListItem item = items.get(pos);
            if (item instanceof DateSeparator) {
                ((DateVH) holder).tvDate.setText(((DateSeparator) item).label);
                return;
            }

            PostItem post = (PostItem) item;
            PostVH h = (PostVH) holder;
            boolean isMine = myId != null && !myId.isEmpty() && myId.equals(post.authorId);

            // Alignement gauche / droite
            h.spacerStart.setVisibility(isMine ? View.VISIBLE : View.GONE);
            h.spacerEnd.setVisibility(isMine ? View.GONE : View.VISIBLE);

            // Couleur de la bulle
            h.bubble.setBackgroundResource(isMine
                    ? R.drawable.bg_chat_mine
                    : R.drawable.bg_chat_other);

            // Nom de l'auteur (uniquement pour les messages des autres)
            if (!isMine && post.authorName != null && !post.authorName.isEmpty()) {
                h.tvAuthor.setVisibility(View.VISIBLE);
                h.tvAuthor.setText(post.authorName);
            } else {
                h.tvAuthor.setVisibility(View.GONE);
            }

            // Texte du message
            if (post.message != null && !post.message.isEmpty()) {
                h.tvMessage.setVisibility(View.VISIBLE);
                h.tvMessage.setText(post.message);
                h.tvMessage.setTextColor(isMine ? 0xFFFFFFFF : 0xFF222222);
            } else {
                h.tvMessage.setVisibility(View.GONE);
            }

            // Photo
            if (post.imageBase64 != null && !post.imageBase64.isEmpty()) {
                Bitmap bmp = ImageUtils.base64ToBitmap(post.imageBase64);
                if (bmp != null) {
                    h.ivPhoto.setVisibility(View.VISIBLE);
                    h.ivPhoto.setImageBitmap(bmp);
                    // Clic sur la photo → ouvrir la publication
                    if (post.photo != null) {
                        h.ivPhoto.setOnClickListener(v -> photoClickListener.onPhotoClick(post.photo));
                    }
                } else {
                    h.ivPhoto.setVisibility(View.GONE);
                }
            } else if (post.photoId != null && !post.photoId.isEmpty()) {
                h.ivPhoto.setVisibility(View.VISIBLE);
                h.ivPhoto.setImageResource(android.R.drawable.ic_menu_gallery);
                h.ivPhoto.setOnClickListener(null);
            } else {
                h.ivPhoto.setVisibility(View.GONE);
                h.ivPhoto.setOnClickListener(null);
            }

            // Heure
            h.tvTime.setText(post.time != null ? post.time : "");
            h.tvTime.setTextColor(isMine ? 0xCCFFFFFF : 0xFFAAAAAA);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    // ─── ViewHolders ─────────────────────────────────────────────────────────

    static class DateVH extends RecyclerView.ViewHolder {
        TextView tvDate;
        DateVH(@NonNull View v) {
            super(v);
            tvDate = v.findViewById(R.id.tv_date_label);
        }
    }

    static class PostVH extends RecyclerView.ViewHolder {
        View spacerStart, spacerEnd, bubble;
        TextView tvAuthor, tvMessage, tvTime;
        ImageView ivPhoto;

        PostVH(@NonNull View v) {
            super(v);
            spacerStart = v.findViewById(R.id.spacer_start);
            spacerEnd = v.findViewById(R.id.spacer_end);
            bubble = v.findViewById(R.id.bubble);
            tvAuthor = v.findViewById(R.id.tv_author_name);
            tvMessage = v.findViewById(R.id.tv_message);
            tvTime = v.findViewById(R.id.tv_time);
            ivPhoto = v.findViewById(R.id.iv_photo);
        }
    }
}
