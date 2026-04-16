package com.example.traveling.ui.groups;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.traveling.R;
import com.example.traveling.model.Group;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class GroupsFragment extends Fragment implements GroupAdapter.Listener {

    private GroupAdapter adapter;
    private View layoutEmpty;
    private RecyclerView recyclerView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_groups, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recycler_groups);
        layoutEmpty = view.findViewById(R.id.layout_empty);
        EditText searchInput = view.findViewById(R.id.search_groups);
        MaterialButton btnCreate = view.findViewById(R.id.btn_create_group);
        MaterialButton btnJoin = view.findViewById(R.id.btn_join_group);

        adapter = new GroupAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.addItemDecoration(
                new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
        recyclerView.setAdapter(adapter);

        List<Group> groups = getSampleGroups();
        adapter.setGroups(groups);
        updateEmptyState(groups.isEmpty());

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int i, int b, int c) {
                adapter.filter(s.toString());
            }
        });

        btnCreate.setOnClickListener(v -> showCreateGroupDialog());
        if (btnJoin != null) {
            btnJoin.setOnClickListener(v -> showJoinGroupDialog());
        }
    }

    private void updateEmptyState(boolean empty) {
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void showCreateGroupDialog() {
        EditText input = new EditText(requireContext());
        input.setHint("Nom du groupe");
        input.setPadding(48, 32, 48, 16);

        new AlertDialog.Builder(requireContext())
                .setTitle("Créer un groupe")
                .setView(input)
                .setPositiveButton("Créer", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        Toast.makeText(requireContext(),
                                "Groupe \"" + name + "\" créé", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void showJoinGroupDialog() {
        EditText input = new EditText(requireContext());
        input.setHint("Code ou nom du groupe");
        input.setPadding(48, 32, 48, 16);

        new AlertDialog.Builder(requireContext())
                .setTitle("Rejoindre un groupe")
                .setView(input)
                .setPositiveButton("Rejoindre", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        Toast.makeText(requireContext(),
                                "Demande envoyée à \"" + name + "\"", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    @Override
    public void onGroupClick(Group group) {
        Toast.makeText(requireContext(), group.getName(), Toast.LENGTH_SHORT).show();
        // TODO: ouvrir le détail du groupe
    }

    private List<Group> getSampleGroups() {
        List<Group> list = new ArrayList<>();
        list.add(new Group("g1", "Voyageurs Paris",
                "Marie : Nouvelle photo du Louvre !",
                "14:32", 8, 3, R.drawable.sample_photo_1));
        list.add(new Group("g2", "Asie & Culture",
                "Thomas : On se retrouve à Kyoto ?",
                "11:05", 15, 1, R.drawable.sample_photo_3));
        list.add(new Group("g3", "Road Trip Maroc",
                "Vous : Super séjour, merci !",
                "Hier", 5, 0, R.drawable.sample_photo_4));
        list.add(new Group("g4", "Amis explorateurs",
                "Ahmed : Regardez ce coucher de soleil",
                "Lun.", 4, 0, R.drawable.sample_photo_2));
        return list;
    }
}
