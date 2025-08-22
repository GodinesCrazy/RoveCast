package com.ivanmarty.rovecast.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import androidx.annotation.*;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.ivanmarty.rovecast.R;
import com.ivanmarty.rovecast.ads.AdsManager;
import com.ivanmarty.rovecast.data.FavoriteStation;
import com.ivanmarty.rovecast.model.Station;
import com.ivanmarty.rovecast.player.PlaybackService;
import com.ivanmarty.rovecast.ui.adapter.StationAdapter;
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
                    s.stationuuid = f.stationuuid; s.name = f.name; s.url_resolved = f.url; s.favicon = f.favicon;
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
        Intent serviceIntent = new Intent(requireContext(), PlaybackService.class);
        serviceIntent.setAction(PlaybackService.ACTION_PLAY);
        serviceIntent.putExtra(PlaybackService.EXTRA_URL, s.getUrl());
        serviceIntent.putExtra(PlaybackService.EXTRA_NAME, s.name);
        serviceIntent.putExtra(PlaybackService.EXTRA_LOGO, s.favicon);
        // Los favoritos no tienen la metadata extra, pasamos null
        serviceIntent.putExtra(PlaybackService.EXTRA_METADATA, (String) null);
        ContextCompat.startForegroundService(requireContext(), serviceIntent);

        Intent playerIntent = new Intent(requireContext(), PlayerActivity.class);
        startActivity(playerIntent);
    }

    @Override public void onFavoriteToggle(Station s, boolean makeFav) { vm.getRepo().toggleFavorite(s, makeFav); }
}
