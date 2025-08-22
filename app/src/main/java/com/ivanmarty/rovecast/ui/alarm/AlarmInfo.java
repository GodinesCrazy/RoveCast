package com.ivanmarty.rovecast.ui.alarm;

import java.util.List;

public class AlarmInfo {
    private final boolean enabled;
    private final int hour;
    private final int minute;
    private final String stationName;
    private final String stationUrl;
    private final String stationLogo;
    private final List<Integer> days;

    public AlarmInfo(boolean enabled, int hour, int minute, String stationName, String stationUrl, String stationLogo, List<Integer> days) {
        this.enabled = enabled;
        this.hour = hour;
        this.minute = minute;
        this.stationName = stationName;
        this.stationUrl = stationUrl;
        this.stationLogo = stationLogo;
        this.days = days;
    }

    public boolean isEnabled() { return enabled; }
    public int getHour() { return hour; }
    public int getMinute() { return minute; }
    public String getStationName() { return stationName; }
    public String getStationUrl() { return stationUrl; }
    public String getStationLogo() { return stationLogo; }
    public List<Integer> getDays() { return days; }
}