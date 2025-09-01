package com.ivanmarty.rovecast.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.ivanmarty.rovecast.R;
import com.ivanmarty.rovecast.ads.AdsManager;
import com.ivanmarty.rovecast.billing.PremiumManager;
import com.ivanmarty.rovecast.data.FavoriteStation;
import com.ivanmarty.rovecast.model.Station;
import com.ivanmarty.rovecast.player.PlaybackService;
import com.ivanmarty.rovecast.ui.adapter.StationAdapter;
import java.util.HashSet;
import java.util.Set;

public class HomeFragment extends Fragment implements StationAdapter.StationListener {

    private HomeViewModel vm;
    private StationAdapter adapter;
    private boolean premium = false;
    private AdView adView;
    private android.widget.ProgressBar progressBar;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup container, @Nullable Bundle saved) {
        AdsManager.init(requireContext());
        View v = inf.inflate(R.layout.fragment_home, container, false);
        RecyclerView rv = v.findViewById(R.id.recycler);
        rv.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        adapter = new StationAdapter(this);
        rv.setAdapter(adapter);

        premium = PremiumManager.isPremium(requireContext());

        progressBar = v.findViewById(R.id.progressBar);
        adView = v.findViewById(R.id.adView);
        if (premium) {
            adView.setVisibility(View.GONE);
        } else {
            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
        }

        vm = new ViewModelProvider(this).get(HomeViewModel.class);

        progressBar.setVisibility(View.VISIBLE);
        vm.refreshStations(); // Lanza la actualización en segundo plano

        // El observador de estaciones ahora recibe la caché al instante y luego el refresh de la red
        vm.getStations().observe(getViewLifecycleOwner(), stations -> {
            progressBar.setVisibility(View.GONE);
            adapter.submitList(stations);
        });

        // Observador de favoritos para actualizar los corazones.
        vm.getFavRepo().getFavorites().observe(getViewLifecycleOwner(), favs -> {
            Set<String> ids = new HashSet<>();
            if (favs != null) for (FavoriteStation f : favs) if (f.stationuuid != null) ids.add(f.stationuuid);
            adapter.updateFavorites(ids);
        });

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { requireActivity().finish(); }
        });
        return v;
    }

    @Override public void onStationSelected(Station s) {
        // 1. Envía el comando al servicio para que empiece a reproducir
        Intent serviceIntent = new Intent(requireContext(), PlaybackService.class);
        serviceIntent.setAction(PlaybackService.ACTION_PLAY);
        serviceIntent.putExtra(PlaybackService.EXTRA_URL, s.url_resolved);
        serviceIntent.putExtra(PlaybackService.EXTRA_NAME, s.name);
        serviceIntent.putExtra(PlaybackService.EXTRA_LOGO, s.favicon);
        serviceIntent.putExtra(PlaybackService.EXTRA_STATION_UUID, s.stationuuid);
        serviceIntent.putExtra(PlaybackService.EXTRA_METADATA, buildMeta(s));
        ContextCompat.startForegroundService(requireContext(), serviceIntent);

        // 2. Abre la PlayerActivity (que ahora está vacía y se conecta al servicio)
        Intent playerIntent = new Intent(requireContext(), PlayerActivity.class);
        startActivity(playerIntent);
    }

    @Override
    public void onResume() {
        super.onResume();
        AdsManager.get().runWithMaybeAd(requireActivity(), null);
    }

    private String buildMeta(Station s) {
        StringBuilder sb = new StringBuilder();
        if (s.country != null && !s.country.isEmpty()) sb.append(s.country).append(" • ");
        if (s.language != null && !s.language.isEmpty()) sb.append(s.language).append(" • ");
        if (s.bitrate > 0) sb.append(s.bitrate).append("kbps").append(" • ");
        if (s.codec != null && !s.codec.isEmpty()) sb.append(s.codec).append(" • ");
        if (s.tags != null && !s.tags.isEmpty()) sb.append(s.tags);
        String meta = sb.toString();
        if (meta.endsWith(" • ")) meta = meta.substring(0, meta.length() - 3);
        return meta;
    }

    @Override public void onFavoriteToggle(Station s, boolean makeFav) { vm.getFavRepo().toggleFavorite(s, makeFav); }
}
