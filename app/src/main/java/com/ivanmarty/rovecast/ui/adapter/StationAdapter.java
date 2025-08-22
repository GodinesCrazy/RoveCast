package com.ivanmarty.rovecast.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.ivanmarty.rovecast.R;
import com.ivanmarty.rovecast.model.Station;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StationAdapter extends RecyclerView.Adapter<StationAdapter.StationVH> {

    public interface StationListener {
        void onStationSelected(Station s);
        void onFavoriteToggle(Station s, boolean makeFav);
    }

    private final StationListener listener;
    private final List<Station> items = new ArrayList<>();
    private final Set<String> favoriteIds = new HashSet<>();

    public StationAdapter(StationListener l) {
        this.listener = l;
    }

    public void submitList(List<Station> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    public void updateFavorites(Set<String> ids) {
        favoriteIds.clear();
        if (ids != null) favoriteIds.addAll(ids);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public StationVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        View v = inf.inflate(R.layout.item_station, parent, false);
        return new StationVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull StationVH holder, int position) {
        holder.bind(items.get(position));
    }

    @Override public int getItemCount() { return items.size(); }

    class StationVH extends RecyclerView.ViewHolder {
        ImageView logo; TextView name; TextView meta; ImageButton heart;

        StationVH(@NonNull View v) {
            super(v);
            logo = v.findViewById(R.id.logo);
            name = v.findViewById(R.id.title);
            meta = v.findViewById(R.id.subtitle);
            heart = v.findViewById(R.id.btnFav);
        }

        void bind(Station s) {
            name.setText(s.name != null ? s.name : "—");
            String m = "";
            if (s.country != null && !s.country.isEmpty()) m += s.country;
            if (s.language != null && !s.language.isEmpty()) m += (m.isEmpty() ? "" : " • ") + s.language;
            if (s.bitrate > 0) m += (m.isEmpty() ? "" : " • ") + s.bitrate + " kbps";
            meta.setText(m);

            if (s.favicon != null && !s.favicon.isEmpty()) {
                Glide.with(logo.getContext())
                        .load(s.favicon)
                        .placeholder(R.drawable.ic_radio_placeholder)
                        .error(R.drawable.ic_radio_placeholder)
                        .into(logo);
            } else {
                logo.setImageResource(R.drawable.ic_radio_placeholder);
            }

            String id = s.stationuuid != null ? s.stationuuid : s.getUrl();
            boolean isFav = favoriteIds.contains(id);
            heart.setImageResource(isFav ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);

            itemView.setOnClickListener(v -> { if (listener != null) listener.onStationSelected(s); });
            heart.setOnClickListener(v -> {
                boolean next = !favoriteIds.contains(id);
                if (listener != null) listener.onFavoriteToggle(s, next);
            });
        }
    }
}