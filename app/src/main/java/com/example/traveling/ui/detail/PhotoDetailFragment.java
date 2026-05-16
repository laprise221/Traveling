package com.example.traveling.ui.detail;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.traveling.R;
import com.example.traveling.data.FirestoreRepository;
import com.example.traveling.data.NotificationRepository;
import com.example.traveling.data.PhotoRegistry;
import com.example.traveling.model.Comment;
import com.example.traveling.model.Photo;
import com.example.traveling.session.SessionManager;
import com.example.traveling.ui.explore.PhotoCarouselAdapter;
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

        boolean isGuest = SessionManager.get().isGuest();
        boolean hasSession = !SessionManager.get().isAnonymous();

        // Toolbar
        view.findViewById(R.id.btn_back).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());
        ((TextView) view.findViewById(R.id.toolbar_title)).setText(photo.getTitle());

        // Signaler
        view.findViewById(R.id.btn_report).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Contenu signalé", Toast.LENGTH_SHORT).show());

        // Image carousel
        ViewPager2 detailViewPager = view.findViewById(R.id.detail_view_pager);
        LinearLayout detailDotsIndicator = view.findViewById(R.id.detail_dots_indicator);

        java.util.List<Object> images = new java.util.ArrayList<>();
        java.util.List<String> base64s = photo.getImages();
        if (!base64s.isEmpty()) {
            images.addAll(base64s);
        } else if (photo.getImageBitmap() != null) {
            images.add(photo.getImageBitmap());
        } else if (photo.getImageUri() != null) {
            images.add(photo.getImageUri());
        }

        PhotoCarouselAdapter carouselAdapter = new PhotoCarouselAdapter();
        carouselAdapter.setImages(images);
        detailViewPager.setAdapter(carouselAdapter);

        if (images.size() > 1) {
            detailDotsIndicator.setVisibility(View.VISIBLE);
            setupDetailDots(detailDotsIndicator, images.size());
            detailViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    updateDetailDots(detailDotsIndicator, images.size(), position);
                }
            });
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

        if (hasSession && photo.getId() != null) {
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

        // Le like est autorisé même en anonyme (connexion anonyme Firebase au besoin)
        btnLike.setEnabled(true);
        btnLike.setAlpha(1f);
        btnLike.setOnClickListener(v -> {
            SessionManager.get().ensureFirebaseSession(() -> {
                if (!isAdded()) return;
                boolean newLiked = !photo.isLiked();
                photo.setLiked(newLiked);
                updateLikeUI();
                if (photo.getId() != null) {
                    FirestoreRepository.get().toggleLikePhoto(photo.getId(), newLiked, null);
                    FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
                    if (newLiked && me != null && !me.isAnonymous() && photo.getAuthorId() != null
                            && !me.getUid().equals(photo.getAuthorId())) {
                        String n = me.getDisplayName() != null ? me.getDisplayName() : "Quelqu'un";
                        NotificationRepository.get().sendNotification(
                                photo.getAuthorId(), "like", me.getUid(), n,
                                photo.getId(), "photo", photo.getTitle(), null);
                    }
                }
            });
        });

        btnFavorite.setEnabled(!isGuest);
        btnFavorite.setAlpha(isGuest ? 0.4f : 1f);
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

        if (isGuest) {
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

    private void setupDetailDots(LinearLayout container, int count) {
        container.removeAllViews();
        float density = getResources().getDisplayMetrics().density;
        int sizeSel = Math.round(8 * density);
        int margin = Math.round(4 * density);
        for (int i = 0; i < count; i++) {
            View dot = new View(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sizeSel, sizeSel);
            lp.setMargins(margin, 0, margin, 0);
            dot.setLayoutParams(lp);
            dot.setBackground(ContextCompat.getDrawable(requireContext(),
                    i == 0 ? R.drawable.dot_selected : R.drawable.dot_unselected));
            container.addView(dot);
        }
    }

    private void updateDetailDots(LinearLayout container, int count, int selected) {
        float density = getResources().getDisplayMetrics().density;
        for (int i = 0; i < container.getChildCount(); i++) {
            View dot = container.getChildAt(i);
            boolean isSel = (i == selected);
            int size = Math.round((isSel ? 8 : 6) * density);
            int margin = Math.round(4 * density);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMargins(margin, 0, margin, 0);
            dot.setLayoutParams(lp);
            dot.setBackground(ContextCompat.getDrawable(requireContext(),
                    isSel ? R.drawable.dot_selected : R.drawable.dot_unselected));
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
