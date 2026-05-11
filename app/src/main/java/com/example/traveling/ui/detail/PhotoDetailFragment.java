package com.example.traveling.ui.detail;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
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
import com.example.traveling.data.FirestoreRepository;
import com.example.traveling.data.ImageUtils;
import com.example.traveling.data.NotificationRepository;
import com.example.traveling.data.PhotoRegistry;
import com.example.traveling.model.Comment;
import com.example.traveling.model.Photo;
import com.example.traveling.session.SessionManager;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PhotoDetailFragment extends Fragment {

    private Photo photo;
    private CommentAdapter commentAdapter;
    private TextView likeCount, tvNoComments;
    private ImageButton btnLike, btnFavorite;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_photo_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        photo = PhotoRegistry.get();
        if (photo == null) {
            Navigation.findNavController(view).navigateUp();
            return;
        }

        boolean isAnonymous = SessionManager.get().isAnonymous();

        // Toolbar
        view.findViewById(R.id.btn_back).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());
        ((TextView) view.findViewById(R.id.toolbar_title)).setText(photo.getTitle());

        // Signaler
        view.findViewById(R.id.btn_report).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Contenu signalé", Toast.LENGTH_SHORT).show());

        // Image
        ImageView image = view.findViewById(R.id.detail_image);
        if (photo.getImageBitmap() != null) {
            image.setImageBitmap(photo.getImageBitmap());
        } else if (photo.getImageUri() != null) {
            image.setImageURI(photo.getImageUri());
        } else if (photo.getImageResId() != 0) {
            image.setImageResource(photo.getImageResId());
        } else if (photo.getImageBase64() != null && !photo.getImageBase64().isEmpty()) {
            android.graphics.Bitmap bmp = ImageUtils.base64ToBitmap(photo.getImageBase64());
            if (bmp != null) image.setImageBitmap(bmp);
        }

        // Infos
        ((TextView) view.findViewById(R.id.detail_title)).setText(photo.getTitle());
        ((TextView) view.findViewById(R.id.detail_author)).setText(photo.getAuthor());
        ((TextView) view.findViewById(R.id.detail_date)).setText(photo.getDate());
        ((TextView) view.findViewById(R.id.detail_location)).setText(photo.getLocationName());
        ((TextView) view.findViewById(R.id.detail_description)).setText(
                photo.getDescription().isEmpty() ? "Aucune description." : photo.getDescription());

        Chip typeChip = view.findViewById(R.id.detail_type_chip);
        typeChip.setText(photo.getLocationType().isEmpty() ? "autre" : photo.getLocationType());

        // Like (pouce)
        btnLike = view.findViewById(R.id.btn_like);
        likeCount = view.findViewById(R.id.detail_like_count);
        // Favori (cœur)
        btnFavorite = view.findViewById(R.id.btn_favorite);

        if (!isAnonymous && photo.getId() != null) {
            FirestoreRepository.get().isPhotoLiked(photo.getId(), liked -> {
                if (!isAdded()) return;
                photo.setLikedSilent(liked);
                updateLikeUI();
            });
            FirestoreRepository.get().isPhotoFavorited(photo.getId(), favorited -> {
                if (!isAdded()) return;
                photo.setFavoritedSilent(favorited);
                updateFavoriteUI();
            });
        }
        updateLikeUI();
        updateFavoriteUI();

        btnLike.setEnabled(!isAnonymous);
        btnLike.setAlpha(isAnonymous ? 0.4f : 1f);
        btnLike.setOnClickListener(v -> {
            boolean newLiked = !photo.isLiked();
            photo.setLiked(newLiked);
            updateLikeUI();
            if (photo.getId() != null) {
                FirestoreRepository.get().toggleLikePhoto(photo.getId(), newLiked, null);
                if (newLiked && photo.getAuthorId() != null) {
                    FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
                    if (me != null && !me.getUid().equals(photo.getAuthorId())) {
                        String n = me.getDisplayName() != null ? me.getDisplayName() : "Quelqu'un";
                        NotificationRepository.get().sendNotification(
                                photo.getAuthorId(), "like", me.getUid(), n,
                                photo.getId(), "photo", photo.getTitle(), null);
                    }
                }
            }
        });

        btnFavorite.setEnabled(!isAnonymous);
        btnFavorite.setAlpha(isAnonymous ? 0.4f : 1f);
        btnFavorite.setOnClickListener(v -> {
            boolean newFav = !photo.isFavorited();
            photo.setFavorited(newFav);
            updateFavoriteUI();
            if (photo.getId() != null) {
                FirestoreRepository.get().toggleFavoritePhoto(photo.getId(), newFav, null);
                if (newFav && photo.getAuthorId() != null) {
                    FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
                    if (me != null && !me.getUid().equals(photo.getAuthorId())) {
                        String n = me.getDisplayName() != null ? me.getDisplayName() : "Quelqu'un";
                        NotificationRepository.get().sendNotification(
                                photo.getAuthorId(), "favorite", me.getUid(), n,
                                photo.getId(), "photo", photo.getTitle(), null);
                    }
                }
            }
            Toast.makeText(requireContext(),
                    newFav ? "Ajouté aux favoris" : "Retiré des favoris",
                    Toast.LENGTH_SHORT).show();
        });

        // Commentaires
        RecyclerView recycler = view.findViewById(R.id.recycler_comments);
        tvNoComments = view.findViewById(R.id.tv_no_comments);
        commentAdapter = new CommentAdapter();
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(commentAdapter);

        // Load comments from Firestore
        if (photo.getId() != null) {
            FirestoreRepository.get().loadPhotoComments(photo.getId(), comments -> {
                if (!isAdded()) return;
                photo.getComments().clear();
                photo.getComments().addAll(comments);
                refreshComments();
            });
        } else {
            refreshComments();
        }

        // Saisie commentaire
        View inputLayout = view.findViewById(R.id.layout_comment_input);
        TextView tvAnon = view.findViewById(R.id.tv_anonymous_comment);
        EditText etComment = view.findViewById(R.id.et_comment);
        ImageButton btnSend = view.findViewById(R.id.btn_send_comment);

        if (isAnonymous) {
            inputLayout.setVisibility(View.GONE);
            tvAnon.setVisibility(View.VISIBLE);
        } else {
            btnSend.setOnClickListener(v -> {
                String text = etComment.getText().toString().trim();
                if (text.isEmpty()) return;

                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                String authorName = "Moi";
                String authorId = "";
                if (user != null) {
                    authorId = user.getUid();
                    String name = user.getDisplayName();
                    if (name != null && !name.isEmpty()) authorName = name;
                }

                String date = new SimpleDateFormat("dd MMM yyyy", Locale.FRENCH)
                        .format(new Date());
                Comment comment = new Comment(
                        "c_" + System.currentTimeMillis(), authorName, text, date);
                comment.setAuthorId(authorId);

                // Add locally for immediate UI feedback
                photo.addComment(comment);
                etComment.setText("");
                refreshComments();

                // Persist to Firestore + notify owner
                if (photo.getId() != null) {
                    FirestoreRepository.get().addPhotoComment(photo.getId(), comment, null);
                    if (photo.getAuthorId() != null && !photo.getAuthorId().equals(authorId)) {
                        NotificationRepository.get().sendNotification(
                                photo.getAuthorId(), "comment", authorId, authorName,
                                photo.getId(), "photo", photo.getTitle(), text);
                    }
                }
            });
        }
    }

    private void updateLikeUI() {
        likeCount.setText(photo.getLikeCount() + " j'aime");
        btnLike.setImageResource(photo.isLiked()
                ? R.drawable.ic_thumb_up_filled : R.drawable.ic_thumb_up);
    }

    private void updateFavoriteUI() {
        btnFavorite.setImageResource(photo.isFavorited()
                ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite);
    }

    private void refreshComments() {
        List<Comment> comments = photo.getComments();
        tvNoComments.setVisibility(comments.isEmpty() ? View.VISIBLE : View.GONE);
        commentAdapter.setComments(comments);
    }

    // ---- CommentAdapter ----
    static class CommentAdapter extends RecyclerView.Adapter<CommentVH> {
        private List<Comment> items = new java.util.ArrayList<>();

        void setComments(List<Comment> list) {
            this.items = list;
            notifyDataSetChanged();
        }

        @NonNull @Override
        public CommentVH onCreateViewHolder(@NonNull ViewGroup p, int v) {
            return new CommentVH(LayoutInflater.from(p.getContext())
                    .inflate(R.layout.item_comment, p, false));
        }

        @Override
        public void onBindViewHolder(@NonNull CommentVH h, int pos) {
            Comment c = items.get(pos);
            h.author.setText(c.getAuthor());
            h.text.setText(c.getText());
            h.date.setText(c.getDate());
            String initials = c.getAuthor().length() >= 2
                    ? c.getAuthor().substring(0, 2).toUpperCase()
                    : c.getAuthor().toUpperCase();
            h.avatar.setText(initials);
        }

        @Override public int getItemCount() { return items.size(); }
    }

    static class CommentVH extends RecyclerView.ViewHolder {
        TextView avatar, author, text, date;
        CommentVH(@NonNull View v) {
            super(v);
            avatar = v.findViewById(R.id.comment_avatar);
            author = v.findViewById(R.id.comment_author);
            text = v.findViewById(R.id.comment_text);
            date = v.findViewById(R.id.comment_date);
        }
    }
}
