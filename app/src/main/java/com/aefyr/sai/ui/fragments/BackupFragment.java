package com.aefyr.sai.ui.fragments;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aefyr.sai.R;
import com.aefyr.sai.adapters.BackupPackagesAdapter;
import com.aefyr.sai.backup.BackupRepository;
import com.aefyr.sai.model.backup.PackageMeta;
import com.aefyr.sai.ui.dialogs.BackupDialogFragment;

public class BackupFragment extends SaiBaseFragment implements BackupPackagesAdapter.OnItemInteractionListener {

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = findViewById(R.id.rv_packages);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setInitialPrefetchItemCount(16);
        recyclerView.setLayoutManager(linearLayoutManager);

        recyclerView.getRecycledViewPool().setMaxRecycledViews(0, 32);

        BackupPackagesAdapter adapter = new BackupPackagesAdapter(getContext());
        adapter.setInteractionListener(this);
        recyclerView.setAdapter(adapter);

        BackupRepository.getInstance(getContext()).getPackages().observe(this, adapter::setData);
    }

    @Override
    protected int layoutId() {
        return R.layout.fragment_backup;
    }

    @Override
    public void onBackupButtonClicked(PackageMeta packageMeta) {
        BackupDialogFragment.newInstance(packageMeta).show(getChildFragmentManager(), null);
    }
}
