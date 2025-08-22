package com.ivanmarty.rovecast.ui.alarm;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.ivanmarty.rovecast.R;
import com.ivanmarty.rovecast.data.FavoriteStation;
import com.ivanmarty.rovecast.model.Station;
import com.ivanmarty.rovecast.ui.HomeViewModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AlarmFragment extends Fragment {

    private TextView tvTime;
    private AutoCompleteTextView stationAutoComplete;
    private ChipGroup chipGroupDays;
    private SwitchMaterial switchAlarm;
    private Button btnSaveAlarm;

    private AlarmScheduler alarmScheduler;
    private List<Station> stationList = new ArrayList<>();
    private int alarmHour, alarmMinute;
    private Station selectedStation;

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
        stationAutoComplete = view.findViewById(R.id.stationAutoComplete);
        chipGroupDays = view.findViewById(R.id.chipGroupDays);
        switchAlarm = view.findViewById(R.id.switchAlarm);
        btnSaveAlarm = view.findViewById(R.id.btnSaveAlarm);

        tvTime.setOnClickListener(v -> showTimePickerDialog());

        loadStations();

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

    private void loadStations() {
        HomeViewModel viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        viewModel.getFavRepo().getFavorites().observe(getViewLifecycleOwner(), favs -> {
            if (favs != null && !favs.isEmpty()) {
                List<Station> stations = new ArrayList<>();
                for (FavoriteStation fav : favs) {
                    Station station = new Station();
                    station.stationuuid = fav.stationuuid;
                    station.name = fav.name;
                    station.url_resolved = fav.url;
                    station.favicon = fav.favicon;
                    stations.add(station);
                }
                stationList = stations;
                setupStationSelector();
            } else {
                viewModel.getStations().observe(getViewLifecycleOwner(), stations -> {
                    stationList = stations;
                    setupStationSelector();
                });
            }
        });
    }

    private void setupStationSelector() {
        if (stationList == null || stationList.isEmpty()) {
            Toast.makeText(getContext(), "No hay emisoras para elegir.", Toast.LENGTH_LONG).show();
            btnSaveAlarm.setEnabled(false);
            return;
        }

        List<String> stationNames = new ArrayList<>();
        for (Station station : stationList) {
            stationNames.add(station.name);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), R.layout.list_item_station_alarm, stationNames);
        stationAutoComplete.setAdapter(adapter);
        stationAutoComplete.setOnItemClickListener((parent, view, position, id) -> {
            selectedStation = stationList.get(position);
        });

        loadAlarmSettings();
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
            for (int i = 0; i < stationList.size(); i++) {
                if (alarmInfo.getStationUrl().equals(stationList.get(i).url_resolved)) {
                    selectedStation = stationList.get(i);
                    stationAutoComplete.setText(selectedStation.name, false);
                    break;
                }
            }
        }
    }

    private void saveAlarm() {
        if (selectedStation == null) {
            Toast.makeText(getContext(), "Por favor, selecciona una emisora.", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean enabled = switchAlarm.isChecked();

        List<Integer> selectedDays = new ArrayList<>();
        for (int i = 0; i < chipGroupDays.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupDays.getChildAt(i);
            if (chip.isChecked()) {
                selectedDays.add(i + 1); // 1 for Monday, 2 for Tuesday, etc.
            }
        }

        AlarmInfo alarmInfo = new AlarmInfo(enabled, alarmHour, alarmMinute, selectedStation.name, selectedStation.url_resolved, selectedStation.favicon, selectedDays);
        alarmScheduler.schedule(alarmInfo);

        Toast.makeText(getContext(), "Alarma guardada", Toast.LENGTH_SHORT).show();
    }
}
