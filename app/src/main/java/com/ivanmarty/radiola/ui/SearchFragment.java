package com.ivanmarty.radiola.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.*;
import android.util.Log;
import android.view.*;
import android.widget.EditText;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.ivanmarty.radiola.R;
import com.ivanmarty.radiola.ads.AdsManager;
import com.ivanmarty.radiola.data.FavoriteStation;
import com.ivanmarty.radiola.model.Station;
import com.ivanmarty.radiola.ui.adapter.StationAdapter;
import java.util.*;

public class SearchFragment extends Fragment implements StationAdapter.StationListener {

    private SearchViewModel vm;
    private StationAdapter adapter;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup container, @Nullable Bundle saved) {
        View v = inf.inflate(R.layout.fragment_search, container, false);

        RecyclerView rv = v.findViewById(R.id.recyclerSearch);
        rv.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        adapter = new StationAdapter(this);
        rv.setAdapter(adapter);

        EditText et = v.findViewById(R.id.etSearch);
        vm = new ViewModelProvider(this).get(SearchViewModel.class);

        // Observa favoritos para marcar corazones
        vm.getRepo().getFavorites().observe(getViewLifecycleOwner(), favs -> {
            Set<String> ids = new HashSet<>();
            if (favs != null) for (FavoriteStation f : favs) if (f.stationuuid != null) ids.add(f.stationuuid);
            adapter.updateFavorites(ids);
        });

        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                String q = s.toString().trim();
                Log.d("SearchFragment", "Query text changed to: " + q);
                if (q.length() < 2) { adapter.submitList(new ArrayList<>()); return; }
                vm.search(q).observe(getViewLifecycleOwner(), list -> {
                    if (list != null) {
                        Log.d("SearchFragment", "Received " + list.size() + " stations for query: " + q);
                        adapter.submitList(list);
                    }
                });
            }
        });

        return v;
    }

    @Override public void onStationSelected(Station s) {
        AdsManager.get().onStationChangeTrigger();
        AdsManager.get().runWithMaybeAd(requireActivity(), () -> {
            Intent i = new Intent(requireContext(), PlayerActivity.class);
            i.putExtra(PlayerActivity.EXTRA_URL, s.url);
            i.putExtra(PlayerActivity.EXTRA_NAME, s.name);
            i.putExtra(PlayerActivity.EXTRA_LOGO, s.favicon);
            startActivity(i);
        });
    }

    @Override public void onFavoriteToggle(Station s, boolean makeFav) { vm.getRepo().toggleFavorite(s, makeFav); }
}
