package com.ivanmarty.radiola.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.ivanmarty.radiola.R;
import com.ivanmarty.radiola.ads.AdsManager;
import com.ivanmarty.radiola.billing.PremiumManager;
import com.ivanmarty.radiola.data.FavoriteStation;
import com.ivanmarty.radiola.model.Station;
import com.ivanmarty.radiola.ui.adapter.StationAdapter;
import java.util.HashSet;
import java.util.Set;

public class HomeFragment extends Fragment implements StationAdapter.StationListener {

    private HomeViewModel vm;
    private StationAdapter adapter;
    private boolean premium = false;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup container, @Nullable Bundle saved) {
        View v = inf.inflate(R.layout.fragment_home, container, false);
        RecyclerView rv = v.findViewById(R.id.recycler);
        rv.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        adapter = new StationAdapter(this);
        rv.setAdapter(adapter);

        premium = PremiumManager.isPremium(requireContext());

        vm = new ViewModelProvider(this).get(HomeViewModel.class);
        vm.loadLocalStations();

        // Observador de estaciones: simplemente las pasa al adapter.
        vm.getStations().observe(getViewLifecycleOwner(), stations -> {
            if (!premium) {
                adapter.submitList(stations);
            }
        });

        // Observador de favoritos para actualizar los corazones.
        vm.getRepo().getFavorites().observe(getViewLifecycleOwner(), favs -> {
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
        // Si el usuario no es premium, intenta mostrar un anuncio antes de continuar.
        // Si es premium, la acción se ejecuta directamente.
        Runnable action = () -> {
            Intent i = new Intent(requireContext(), PlayerActivity.class);
            i.putExtra(PlayerActivity.EXTRA_URL, s.url);
            i.putExtra(PlayerActivity.EXTRA_NAME, s.name);
            i.putExtra(PlayerActivity.EXTRA_LOGO, s.favicon);
            i.putExtra(PlayerActivity.EXTRA_COUNTRY, s.country);
            i.putExtra(PlayerActivity.EXTRA_LANG, s.language);
            i.putExtra(PlayerActivity.EXTRA_BITRATE, s.bitrate);
            i.putExtra(PlayerActivity.EXTRA_CODEC, s.codec);
            i.putExtra(PlayerActivity.EXTRA_TAGS, s.tags);
            startActivity(i);
        };

        if (premium) {
            action.run();
        } else {
            AdsManager.get().runWithMaybeAd(requireActivity(), action);
        }
    }

    @Override public void onFavoriteToggle(Station s, boolean makeFav) { vm.getRepo().toggleFavorite(s, makeFav); }
}