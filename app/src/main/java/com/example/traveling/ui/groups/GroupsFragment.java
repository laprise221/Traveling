package com.example.traveling.ui.groups;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.traveling.R;
import com.example.traveling.data.GroupRepository;
import com.example.traveling.model.Group;
import com.example.traveling.session.SessionManager;
import com.google.android.material.button.MaterialButton;

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
        MaterialButton btnDiscover = view.findViewById(R.id.btn_discover_groups);
        MaterialButton btnJoin = view.findViewById(R.id.btn_join_group);

        adapter = new GroupAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.addItemDecoration(
                new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
        recyclerView.setAdapter(adapter);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int i, int b, int c) {
                adapter.filter(s.toString());
            }
        });

        btnDiscover.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_groups_to_group_search));

        btnCreate.setOnClickListener(v -> {
            if (SessionManager.get().isAnonymous()) {
                Toast.makeText(requireContext(), "Connectez-vous pour créer un groupe", Toast.LENGTH_SHORT).show();
                return;
            }
            showCreateGroupDialog();
        });

        if (btnJoin != null) {
            btnJoin.setOnClickListener(v -> {
                if (SessionManager.get().isAnonymous()) {
                    Toast.makeText(requireContext(), "Connectez-vous pour rejoindre un groupe", Toast.LENGTH_SHORT).show();
                    return;
                }
                showJoinGroupDialog();
            });
        }

        loadGroups();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadGroups();
    }

    private void loadGroups() {
        if (SessionManager.get().isAnonymous()) {
            updateEmptyState(true);
            return;
        }
        GroupRepository.get().loadUserGroups(groups -> {
            if (!isAdded()) return;
            adapter.setGroups(groups);
            updateEmptyState(groups.isEmpty());
        });
    }

    private void updateEmptyState(boolean empty) {
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private static final String[] THEMES = {"", "Musée", "Nature", "Plage", "Monument", "Rue", "Voyage", "Gastronomie", "Architecture", "Photographie"};
    private static final String[] THEMES_LABELS = {"Aucun", "Musée", "Nature", "Plage", "Monument", "Rue", "Voyage", "Gastronomie", "Architecture", "Photographie"};

    private void showCreateGroupDialog() {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 16);

        EditText etName = new EditText(requireContext());
        etName.setHint("Nom du groupe");
        layout.addView(etName);

        EditText etDesc = new EditText(requireContext());
        etDesc.setHint("Description (optionnel)");
        layout.addView(etDesc);

        android.widget.Spinner spinnerTheme = new android.widget.Spinner(requireContext());
        android.widget.ArrayAdapter<String> themeAdapter = new android.widget.ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_dropdown_item, THEMES_LABELS);
        spinnerTheme.setAdapter(themeAdapter);
        android.widget.TextView tvTheme = new android.widget.TextView(requireContext());
        tvTheme.setText("Thème :");
        tvTheme.setPadding(0, 16, 0, 4);
        layout.addView(tvTheme);
        layout.addView(spinnerTheme);

        CheckBox cbPublic = new CheckBox(requireContext());
        cbPublic.setText("Groupe public (visible par tous)");
        cbPublic.setChecked(true);
        layout.addView(cbPublic);

        new AlertDialog.Builder(requireContext())
                .setTitle("Créer un groupe")
                .setView(layout)
                .setPositiveButton("Créer", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(requireContext(), "Nom requis", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String desc = etDesc.getText().toString().trim();
                    String theme = THEMES[spinnerTheme.getSelectedItemPosition()];
                    boolean isPublic = cbPublic.isChecked();
                    GroupRepository.get().createGroup(name, desc, theme, isPublic,
                            groupId -> {
                                if (!isAdded()) return;
                                Toast.makeText(requireContext(),
                                        "Groupe \"" + name + "\" créé", Toast.LENGTH_SHORT).show();
                                loadGroups();
                            },
                            e -> Toast.makeText(requireContext(),
                                    "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void showJoinGroupDialog() {
        EditText input = new EditText(requireContext());
        input.setHint("Nom du groupe public");
        input.setPadding(48, 32, 48, 16);

        new AlertDialog.Builder(requireContext())
                .setTitle("Rejoindre un groupe")
                .setView(input)
                .setPositiveButton("Rejoindre", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) return;
                    GroupRepository.get().joinGroupByName(name, found -> {
                        if (!isAdded()) return;
                        if (found) {
                            Toast.makeText(requireContext(),
                                    "Vous avez rejoint \"" + name + "\"", Toast.LENGTH_SHORT).show();
                            loadGroups();
                        } else {
                            Toast.makeText(requireContext(),
                                    "Groupe public introuvable", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    @Override
    public void onGroupClick(Group group) {
        GroupRegistry.set(group);
        Navigation.findNavController(requireView()).navigate(R.id.action_groups_to_group_detail);
    }
}
