package com.medislot.util;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Pure slot arithmetic. A slot is only produced if it fits completely inside the window. */
public final class SlotGenerator {

    private SlotGenerator() {}

    public static List<LocalTime> startTimes(LocalTime start, LocalTime end, int minutes) {
        List<LocalTime> out = new ArrayList<>();
        if (minutes <= 0) return out;
        int s = start.getHour() * 60 + start.getMinute();
        int e = end.getHour() * 60 + end.getMinute();
        for (int m = s; m + minutes <= e; m += minutes) {
            out.add(LocalTime.of(m / 60, m % 60));
        }
        return out;
    }

    public static String format(LocalTime t) {
        return String.format("%02d:%02d", t.getHour(), t.getMinute());
    }
}