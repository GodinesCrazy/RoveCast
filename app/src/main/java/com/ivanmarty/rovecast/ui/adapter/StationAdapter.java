package com.ivanmarty.rovecast.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.ivanmarty.rovecast.R;
import com.ivanmarty.rovecast.model.Station;

public class StationAdapter extends ListAdapter<Station, StationAdapter.StationViewHolder> {

    private final StationListener listener;

    public StationAdapter(StationListener listener) {
        super(new StationDiffCallback());
        this.listener = listener;
    }

    @NonNull
    @Override
    public StationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_station, parent, false);
        return new StationViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull StationViewHolder holder, int position) {
        Station station = getItem(position);
        holder.name.setText(station.name);
        holder.country.setText(station.country);
        Glide.with(holder.itemView.getContext())
                .load(station.favicon)
                .placeholder(R.drawable.ic_radio_placeholder)
                .error(R.drawable.ic_radio_placeholder)
                .fallback(R.drawable.ic_radio_placeholder)
                .centerCrop()
                .into(holder.logo);

        holder.fav.setSelected(station.isFavorite);

        holder.itemView.setOnClickListener(v -> listener.onStationSelected(station));
        holder.fav.setOnClickListener(v -> {
            boolean isNowFavorite = !v.isSelected();
            v.setSelected(isNowFavorite);
            listener.onFavoriteToggle(station, isNowFavorite);
        });
    }

    // Interfaz que coincide con la implementación existente en los Fragments
    public interface StationListener {
        void onStationSelected(Station station);
        void onFavoriteToggle(Station station, boolean makeFav);
    }

    static class StationViewHolder extends RecyclerView.ViewHolder {
        ImageView logo;
        TextView name, country;
        ImageView fav;

        public StationViewHolder(@NonNull View itemView) {
            super(itemView);
            logo = itemView.findViewById(R.id.logo);
            name = itemView.findViewById(R.id.title);
            country = itemView.findViewById(R.id.subtitle);
            fav = itemView.findViewById(R.id.btnFav);
        }
    }

    private static class StationDiffCallback extends DiffUtil.ItemCallback<Station> {
        @Override
        public boolean areItemsTheSame(@NonNull Station oldItem, @NonNull Station newItem) {
            return oldItem.stationuuid.equals(newItem.stationuuid);
        }

        @Override
        public boolean areContentsTheSame(@NonNull Station oldItem, @NonNull Station newItem) {
            return oldItem.equals(newItem);
        }
    }
}
