package com.ivanmarty.rovecast.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ivanmarty.rovecast.R;
import com.ivanmarty.rovecast.data.SleepTimerPreset;

import java.util.ArrayList;
import java.util.List;

public class SleepTimerPresetAdapter extends RecyclerView.Adapter<SleepTimerPresetAdapter.PresetViewHolder> {

    public interface OnPresetClickListener {
        void onPresetClick(SleepTimerPreset preset);
        void onPresetLongClick(SleepTimerPreset preset);
    }

    private List<SleepTimerPreset> presets = new ArrayList<>();
    private final OnPresetClickListener listener;

    public SleepTimerPresetAdapter(OnPresetClickListener listener) {
        this.listener = listener;
    }

    public void setPresets(List<SleepTimerPreset> presets) {
        this.presets = presets;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PresetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new PresetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PresetViewHolder holder, int position) {
        SleepTimerPreset preset = presets.get(position);
        holder.bind(preset, listener);
    }

    @Override
    public int getItemCount() {
        return presets.size();
    }

    static class PresetViewHolder extends RecyclerView.ViewHolder {

        private final TextView textView;

        public PresetViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
        }

        public void bind(final SleepTimerPreset preset, final OnPresetClickListener listener) {
            textView.setText(preset.name + " (" + preset.durationMinutes + " min)");
            itemView.setOnClickListener(v -> listener.onPresetClick(preset));
            itemView.setOnLongClickListener(v -> {
                listener.onPresetLongClick(preset);
                return true;
            });
        }
    }
}
