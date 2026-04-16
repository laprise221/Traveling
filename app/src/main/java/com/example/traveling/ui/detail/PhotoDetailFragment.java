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
import com.example.traveling.data.PhotoRegistry;
import com.example.traveling.data.UserRepository;
import com.example.traveling.model.Comment;
import com.example.traveling.model.Photo;
import com.example.traveling.session.SessionManager;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PhotoDetailFragment extends Fragment {

    private Photo photo;
    private CommentAdapter commentAdapter;
    private TextView likeCount, tvNoComments;
    private ImageButton btnLike;

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
        if (photo.getImageBitmap() != null) image.setImageBitmap(photo.getImageBitmap());
        else if (photo.getImageUri() != null) image.setImageURI(photo.getImageUri());
        else if (photo.getImageResId() != 0) image.setImageResource(photo.getImageResId());

        // Infos
        ((TextView) view.findViewById(R.id.detail_title)).setText(photo.getTitle());
        ((TextView) view.findViewById(R.id.detail_author)).setText(photo.getAuthor());
        ((TextView) view.findViewById(R.id.detail_date)).setText(photo.getDate());
        ((TextView) view.findViewById(R.id.detail_location)).setText(photo.getLocationName());
        ((TextView) view.findViewById(R.id.detail_description)).setText(
                photo.getDescription().isEmpty() ? "Aucune description." : photo.getDescription());

        Chip typeChip = view.findViewById(R.id.detail_type_chip);
        typeChip.setText(photo.getLocationType().isEmpty() ? "autre" : photo.getLocationType());

        // Like
        btnLike = view.findViewById(R.id.btn_like);
        likeCount = view.findViewById(R.id.detail_like_count);
        updateLikeUI();

        btnLike.setEnabled(!isAnonymous);
        btnLike.setAlpha(isAnonymous ? 0.4f : 1f);
        btnLike.setOnClickListener(v -> {
            photo.setLiked(!photo.isLiked());
            if (photo.isLiked()) UserRepository.get().likePhoto(photo);
            else UserRepository.get().unlikePhoto(photo);
            updateLikeUI();
        });

        // Commentaires
        RecyclerView recycler = view.findViewById(R.id.recycler_comments);
        tvNoComments = view.findViewById(R.id.tv_no_comments);
        commentAdapter = new CommentAdapter();
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(commentAdapter);
        refreshComments();

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

                String author = "Moi";
                if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                    String name = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
                    if (name != null && !name.isEmpty()) author = name;
                }
                String date = new SimpleDateFormat("dd MMM yyyy", Locale.FRENCH)
                        .format(new Date());
                Comment comment = new Comment(
                        "c_" + System.currentTimeMillis(), author, text, date);
                photo.addComment(comment);
                etComment.setText("");
                refreshComments();
            });
        }
    }

    private void updateLikeUI() {
        likeCount.setText(photo.getLikeCount() + " j'aime");
        btnLike.setImageResource(photo.isLiked()
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
