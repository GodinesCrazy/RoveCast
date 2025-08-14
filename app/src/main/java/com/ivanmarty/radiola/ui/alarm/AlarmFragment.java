package com.ivanmarty.radiola.ui.alarm;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.ivanmarty.radiola.R;
import com.ivanmarty.radiola.data.FavoriteStation;
import com.ivanmarty.radiola.ui.HomeViewModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AlarmFragment extends Fragment {

    private TextView tvTime;
    private ChipGroup chipGroupDays;
    private SwitchMaterial switchAlarm;
    private Button btnSaveAlarm;
    private ImageView ivStationLogo;

    private AlarmScheduler alarmScheduler;
    private List<FavoriteStation> favoritesList = new ArrayList<>();
    private int alarmHour, alarmMinute;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_alarm, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        alarmScheduler = new AlarmScheduler(requireContext());

        tvTime = view.findViewById(R.id.tvTime);
        chipGroupDays = view.findViewById(R.id.chipGroupDays);
        switchAlarm = view.findViewById(R.id.switchAlarm);
        btnSaveAlarm = view.findViewById(R.id.btnSaveAlarm);
        ivStationLogo = view.findViewById(R.id.ivStationLogo);

        tvTime.setOnClickListener(v -> showTimePickerDialog());

        loadFavoriteStations();

        btnSaveAlarm.setOnClickListener(v -> saveAlarm());
    }

    private void showTimePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        int currentMinute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(), (view, hourOfDay, minute) -> {
            alarmHour = hourOfDay;
            alarmMinute = minute;
            tvTime.setText(String.format("%02d:%02d", alarmHour, alarmMinute));
        }, currentHour, currentMinute, true);

        timePickerDialog.show();
    }

    private void loadFavoriteStations() {
        HomeViewModel viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        viewModel.getRepo().getFavorites().observe(getViewLifecycleOwner(), favs -> {
            if (favs == null || favs.isEmpty()) {
                Toast.makeText(getContext(), "No hay emisoras favoritas para elegir.", Toast.LENGTH_LONG).show();
                btnSaveAlarm.setEnabled(false);
                return;
            }

            favoritesList = favs;
            // For now, we'll just use the first favorite station
            updateStationLogo(0);

            loadAlarmSettings();
        });
    }

    private void loadAlarmSettings() {
        AlarmInfo alarmInfo = alarmScheduler.load();
        switchAlarm.setChecked(alarmInfo.isEnabled());

        alarmHour = alarmInfo.getHour();
        alarmMinute = alarmInfo.getMinute();
        tvTime.setText(String.format("%02d:%02d", alarmHour, alarmMinute));

        List<Integer> selectedDays = alarmInfo.getDays();
        if (selectedDays != null) {
            for (int i = 0; i < chipGroupDays.getChildCount(); i++) {
                Chip chip = (Chip) chipGroupDays.getChildAt(i);
                chip.setChecked(selectedDays.contains(i + 1));
            }
        }

        if (alarmInfo.getStationUrl() != null) {
            for (int i = 0; i < favoritesList.size(); i++) {
                if (alarmInfo.getStationUrl().equals(favoritesList.get(i).url)) {
                    updateStationLogo(i);
                    break;
                }
            }
        }
    }

    private void updateStationLogo(int position) {
        if (getContext() != null && !favoritesList.isEmpty()) {
            FavoriteStation selectedStation = favoritesList.get(position);
            Glide.with(getContext())
                    .load(selectedStation.favicon)
                    .placeholder(R.drawable.ic_radio_placeholder)
                    .into(ivStationLogo);
        }
    }

    private void saveAlarm() {
        if (favoritesList.isEmpty()) {
            Toast.makeText(getContext(), "No se puede guardar la alarma sin emisoras favoritas.", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean enabled = switchAlarm.isChecked();

        // For now, we'll just use the first favorite station
        FavoriteStation selectedStation = favoritesList.get(0);

        // Get selected days from ChipGroup
        List<Integer> selectedDays = new ArrayList<>();
        for (int i = 0; i < chipGroupDays.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupDays.getChildAt(i);
            if (chip.isChecked()) {
                selectedDays.add(i + 1); // 1 for Monday, 2 for Tuesday, etc.
            }
        }

        AlarmInfo alarmInfo = new AlarmInfo(enabled, alarmHour, alarmMinute, selectedStation.name, selectedStation.url, selectedStation.favicon, selectedDays);
        alarmScheduler.schedule(alarmInfo);

        Toast.makeText(getContext(), "Alarma guardada", Toast.LENGTH_SHORT).show();
    }
}
