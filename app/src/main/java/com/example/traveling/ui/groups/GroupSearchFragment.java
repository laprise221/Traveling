package com.example.traveling.ui.groups;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
import com.example.traveling.model.Group;
import com.example.traveling.session.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

public class GroupSearchFragment extends Fragment implements GroupAdapter.Listener {

    private GroupAdapter adapter;
    private RecyclerView recycler;
    private TextView tvEmpty;
    private EditText etSearch;
    private ChipGroup chipGroupThemes;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_group_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btn_back).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());

        etSearch = view.findViewById(R.id.et_search_group);
        chipGroupThemes = view.findViewById(R.id.chip_group_themes);
        recycler = view.findViewById(R.id.recycler_results);
        tvEmpty = view.findViewById(R.id.tv_empty_results);
        MaterialButton btnSearch = view.findViewById(R.id.btn_search);

        adapter = new GroupAdapter(this, true);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        btnSearch.setOnClickListener(v -> performSearch());

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            performSearch();
            return true;
        });
    }

    private void performSearch() {
        String query = etSearch.getText().toString().trim();
        String theme = getSelectedTheme();

        GroupRepository.get().searchPublicGroups(theme, query, groups -> {
            if (!isAdded()) return;
            if (groups.isEmpty()) {
                recycler.setVisibility(View.GONE);
                tvEmpty.setText("Aucun groupe trouvé");
                tvEmpty.setVisibility(View.VISIBLE);
            } else {
                tvEmpty.setVisibility(View.GONE);
                recycler.setVisibility(View.VISIBLE);
                adapter.setGroups(groups);
            }
        });
    }

    private String getSelectedTheme() {
        int checkedId = chipGroupThemes.getCheckedChipId();
        if (checkedId == R.id.chip_theme_all || checkedId == View.NO_ID) return "";
        Chip chip = chipGroupThemes.findViewById(checkedId);
        return chip != null ? chip.getText().toString() : "";
    }

    @Override
    public void onGroupClick(Group group) {
        GroupRegistry.set(group);
        Navigation.findNavController(requireView()).navigate(R.id.action_group_search_to_group_detail);
    }

    @Override
    public void onJoinClick(Group group) {
        if (SessionManager.get().isAnonymous()) {
            Toast.makeText(requireContext(), "Connectez-vous pour rejoindre un groupe", Toast.LENGTH_SHORT).show();
            return;
        }
        GroupRepository.get().joinGroup(group.getId(), success -> {
            if (!isAdded()) return;
            if (success) {
                Toast.makeText(requireContext(),
                        "Vous avez rejoint \"" + group.getName() + "\"", Toast.LENGTH_SHORT).show();
                performSearch();
            } else {
                Toast.makeText(requireContext(),
                        "Impossible de rejoindre ce groupe", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
