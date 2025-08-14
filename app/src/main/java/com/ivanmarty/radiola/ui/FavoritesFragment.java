package com.ivanmarty.radiola.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
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

public class FavoritesFragment extends Fragment implements StationAdapter.StationListener {

    private FavoritesViewModel vm;
    private StationAdapter adapter;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup container, @Nullable Bundle saved) {
        View v = inf.inflate(R.layout.fragment_favorites, container, false);
        RecyclerView rv = v.findViewById(R.id.recyclerFav);
        rv.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        adapter = new StationAdapter(this);
        rv.setAdapter(adapter);

        vm = new ViewModelProvider(this).get(FavoritesViewModel.class);
        vm.getFavorites().observe(getViewLifecycleOwner(), favs -> {
            // Lista a mostrar
            List<Station> out = new ArrayList<>();
            Set<String> ids = new HashSet<>();
            if (favs != null) {
                for (FavoriteStation f : favs) {
                    Station s = new Station();
                    s.stationuuid = f.stationuuid; s.name = f.name; s.url = f.url; s.favicon = f.favicon;
                    out.add(s);
                    if (f.stationuuid != null) ids.add(f.stationuuid);
                }
            }
            adapter.submitList(out);
            adapter.updateFavorites(ids); // marca corazones
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
