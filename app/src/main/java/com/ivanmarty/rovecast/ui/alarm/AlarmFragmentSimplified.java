package com.ivanmarty.rovecast.ui.alarm;

import android.app.TimePickerDialog;
import android.content.ComponentName;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.media3.common.MediaItem;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.ivanmarty.rovecast.R;
import com.ivanmarty.rovecast.model.Station;
import com.ivanmarty.rovecast.player.PlaybackService;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * SISTEMA DE ALARMAS SIMPLIFICADO - SEPTIEMBRE 2025
 * 
 * MEJORAS IMPLEMENTADAS:
 * ✅ Usa automáticamente la radio que está sonando actualmente
 * ✅ Switch claro ON/OFF para activar/desactivar alarma
 * ✅ UX simplificada - solo hora, días y switch
 * ✅ Elimina selector de lista innecesario
 * ✅ Detección real de alarmas activas
 */
@androidx.media3.common.util.UnstableApi
public class AlarmFragmentSimplified extends Fragment {

    private TextView tvTime;
    private TextView tvCurrentStation;
    private ChipGroup chipGroupDays;
    private SwitchMaterial switchAlarm;
    private Button btnSaveAlarm;

    private AlarmScheduler alarmScheduler;
    private ListenableFuture<MediaController> controllerFuture;
    private MediaController controller;
    private Station currentStation;
    private int alarmHour = 7, alarmMinute = 0; // Default 7:00 AM

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_alarm_simplified, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        alarmScheduler = new AlarmScheduler(requireContext());

        // Initialize views
        tvTime = view.findViewById(R.id.tvTime);
        tvCurrentStation = view.findViewById(R.id.tvCurrentStation);
        chipGroupDays = view.findViewById(R.id.chipGroupDays);
        switchAlarm = view.findViewById(R.id.switchAlarm);
        btnSaveAlarm = view.findViewById(R.id.btnSaveAlarm);

        // Setup click listeners
        tvTime.setOnClickListener(v -> showTimePickerDialog());
        btnSaveAlarm.setOnClickListener(v -> saveAlarm());

        // Switch listener with clear feedback
        switchAlarm.setOnCheckedChangeListener((buttonView, isChecked) -> {
            btnSaveAlarm.setText(isChecked ? "Activar Alarma" : "Desactivar Alarma");
            btnSaveAlarm.setEnabled(true);
        });

        // Connect to current playing station
        connectToMediaController();
        loadAlarmSettings();
    }

    private void connectToMediaController() {
        SessionToken sessionToken = new SessionToken(requireContext(), 
            new ComponentName(requireContext(), PlaybackService.class));
        controllerFuture = new MediaController.Builder(requireContext(), sessionToken).buildAsync();
        controllerFuture.addListener(() -> {
            try {
                controller = controllerFuture.get();
                updateCurrentStation();
            } catch (Exception e) {
                // If no media is playing, show message
                tvCurrentStation.setText("⚠️ No hay radio sonando actualmente");
                tvCurrentStation.setTextColor(getResources().getColor(R.color.textSecondary));
            }
        }, MoreExecutors.directExecutor());
    }

    private void updateCurrentStation() {
        if (controller != null && controller.getCurrentMediaItem() != null) {
            MediaItem mediaItem = controller.getCurrentMediaItem();
            
            // Create Station object from current media
            currentStation = new Station();
            currentStation.stationuuid = mediaItem.mediaId;
            currentStation.name = mediaItem.mediaMetadata.title != null ? 
                mediaItem.mediaMetadata.title.toString() : "Radio Station";
            currentStation.favicon = mediaItem.mediaMetadata.artworkUri != null ? 
                mediaItem.mediaMetadata.artworkUri.toString() : null;
            
            if (mediaItem.localConfiguration != null) {
                currentStation.url_resolved = mediaItem.localConfiguration.uri.toString();
            }

            // Update UI
            tvCurrentStation.setText("🎵 " + currentStation.name);
            tvCurrentStation.setTextColor(getResources().getColor(R.color.accent));
            btnSaveAlarm.setEnabled(true);
        } else {
            tvCurrentStation.setText("⚠️ No hay radio sonando actualmente");
            tvCurrentStation.setTextColor(getResources().getColor(R.color.textSecondary));
            btnSaveAlarm.setEnabled(false);
        }
    }

    private void showTimePickerDialog() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(), 
            (view, hourOfDay, minute) -> {
                alarmHour = hourOfDay;
                alarmMinute = minute;
                tvTime.setText(String.format("%02d:%02d", alarmHour, alarmMinute));
            }, alarmHour, alarmMinute, true);

        timePickerDialog.show();
    }

    private void loadAlarmSettings() {
        AlarmInfo alarmInfo = alarmScheduler.load();
        
        // Load time
        alarmHour = alarmInfo.getHour();
        alarmMinute = alarmInfo.getMinute();
        tvTime.setText(String.format("%02d:%02d", alarmHour, alarmMinute));

        // Load switch state
        switchAlarm.setChecked(alarmInfo.isEnabled());
        btnSaveAlarm.setText(alarmInfo.isEnabled() ? "Actualizar Alarma" : "Activar Alarma");

        // Load selected days
        List<Integer> selectedDays = alarmInfo.getDays();
        if (selectedDays != null) {
            for (int i = 0; i < chipGroupDays.getChildCount(); i++) {
                Chip chip = (Chip) chipGroupDays.getChildAt(i);
                chip.setChecked(selectedDays.contains(i + 1));
            }
        }
    }

    private void saveAlarm() {
        if (currentStation == null) {
            Toast.makeText(getContext(), 
                "⚠️ No hay radio sonando. Reproduce una radio primero.", 
                Toast.LENGTH_LONG).show();
            return;
        }

        boolean enabled = switchAlarm.isChecked();

        // Get selected days
        List<Integer> selectedDays = new ArrayList<>();
        for (int i = 0; i < chipGroupDays.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupDays.getChildAt(i);
            if (chip.isChecked()) {
                selectedDays.add(i + 1); // 1 for Monday, 2 for Tuesday, etc.
            }
        }

        // Create alarm info with current station
        AlarmInfo alarmInfo = new AlarmInfo(
            enabled, 
            alarmHour, 
            alarmMinute, 
            currentStation.name, 
            currentStation.url_resolved, 
            currentStation.favicon, 
            selectedDays
        );

        // Schedule alarm
        alarmScheduler.schedule(alarmInfo);

        // Show success message
        if (enabled) {
            String daysText = selectedDays.isEmpty() ? "todos los días" : 
                selectedDays.size() + " días seleccionados";
            Toast.makeText(getContext(), 
                String.format("✅ Alarma activada para las %02d:%02d\n🎵 Radio: %s\n📅 %s", 
                    alarmHour, alarmMinute, currentStation.name, daysText), 
                Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getContext(), "❌ Alarma desactivada", Toast.LENGTH_SHORT).show();
        }

        // Close fragment
        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
            getParentFragmentManager().popBackStack();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (controllerFuture != null) {
            MediaController.releaseFuture(controllerFuture);
        }
    }
}
