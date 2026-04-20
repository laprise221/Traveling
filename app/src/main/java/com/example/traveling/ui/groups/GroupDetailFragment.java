package com.example.traveling.ui.groups;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.traveling.R;
import com.example.traveling.data.GroupRepository;
import com.example.traveling.data.NotificationRepository;
import com.example.traveling.model.Group;
import com.example.traveling.session.SessionManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class GroupDetailFragment extends Fragment {

    private Group group;
    private RecyclerView recycler;
    private TextView tvEmpty;
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
        adapter = new PostAdapter();
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        FloatingActionButton fab = view.findViewById(R.id.fab_share_photo);
        fab.setOnClickListener(v -> {
            if (SessionManager.get().isAnonymous()) {
                Toast.makeText(requireContext(), "Connectez-vous pour partager", Toast.LENGTH_SHORT).show();
                return;
            }
            showShareDialog();
        });

        loadPosts();
    }

    private void showShareDialog() {
        EditText input = new EditText(requireContext());
        input.setHint("Votre message...");
        input.setPadding(48, 32, 48, 16);

        new AlertDialog.Builder(requireContext())
                .setTitle("Partager dans " + group.getName())
                .setView(input)
                .setPositiveButton("Partager", (d, w) -> {
                    String message = input.getText().toString().trim();
                    if (message.isEmpty()) return;
                    FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
                    String myId = me != null ? me.getUid() : "";
                    String myName = (me != null && me.getDisplayName() != null)
                            ? me.getDisplayName() : "Quelqu'un";
                    GroupRepository.get().addGroupPost(group.getId(), null, message, id -> {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), "Message partagé !", Toast.LENGTH_SHORT).show();
                        loadPosts();
                        // Notify all other group members
                        for (String memberId : group.getMemberIds()) {
                            if (!memberId.equals(myId)) {
                                NotificationRepository.get().sendNotification(
                                        memberId, "group_post", myId, myName,
                                        group.getId(), "group", group.getName(), message);
                            }
                        }
                    });
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void loadPosts() {
        GroupRepository.get().loadGroupPosts(group.getId(), (QuerySnapshot snapshots) -> {
            if (!isAdded()) return;
            if (snapshots == null || snapshots.isEmpty()) {
                recycler.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.VISIBLE);
                return;
            }
            List<PostItem> posts = new ArrayList<>();
            for (DocumentSnapshot doc : snapshots) {
                PostItem item = new PostItem();
                item.message = doc.getString("message");
                item.authorName = doc.getString("authorName");
                item.photoId = doc.getString("photoId");
                posts.add(item);
            }
            recycler.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.GONE);
            adapter.setPosts(posts);
        });
    }

    static class PostItem {
        String authorName, message, photoId;
    }

    static class PostAdapter extends RecyclerView.Adapter<PostVH> {
        private List<PostItem> items = new ArrayList<>();

        void setPosts(List<PostItem> posts) {
            this.items = posts;
            notifyDataSetChanged();
        }

        @NonNull @Override
        public PostVH onCreateViewHolder(@NonNull ViewGroup p, int v) {
            View view = LayoutInflater.from(p.getContext())
                    .inflate(android.R.layout.two_line_list_item, p, false);
            return new PostVH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PostVH h, int pos) {
            PostItem item = items.get(pos);
            h.text1.setText(item.authorName != null ? item.authorName : "");
            h.text2.setText(item.message != null ? item.message : "");
        }

        @Override public int getItemCount() { return items.size(); }
    }

    static class PostVH extends RecyclerView.ViewHolder {
        TextView text1, text2;
        PostVH(@NonNull View v) {
            super(v);
            text1 = v.findViewById(android.R.id.text1);
            text2 = v.findViewById(android.R.id.text2);
        }
    }
}
