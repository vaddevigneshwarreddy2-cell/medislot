package com.medislot.service;

import com.medislot.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Validation of a weekly schedule. */
public final class ScheduleRules {

    public static final int MAX_WINDOWS_PER_DAY = 3;

    public record Window(int dayOfWeek, LocalTime start, LocalTime end) {}

    private ScheduleRules() {}

    public static void validate(List<Window> windows, int durationMin) {
        Map<Integer, List<Window>> byDay = new TreeMap<>();
        for (Window w : windows) {
            if (w.dayOfWeek() < 1 || w.dayOfWeek() > 7) {
                throw bad("Day of week must be 1 (Monday) to 7 (Sunday).");
            }
            if (!w.start().isBefore(w.end())) {
                throw bad("Each working window must end after it starts.");
            }
            if (Duration.between(w.start(), w.end()).toMinutes() < durationMin) {
                throw bad("A working window is shorter than the " + durationMin + "-minute appointment length.");
            }
            byDay.computeIfAbsent(w.dayOfWeek(), k -> new ArrayList<>()).add(w);
        }
        for (List<Window> list : byDay.values()) {
            if (list.size() > MAX_WINDOWS_PER_DAY) {
                throw bad("At most " + MAX_WINDOWS_PER_DAY + " working windows are allowed per day.");
            }
            list.sort(Comparator.comparing(Window::start));
            for (int i = 1; i < list.size(); i++) {
                if (list.get(i).start().isBefore(list.get(i - 1).end())) {
                    throw bad("Working windows on the same day must not overlap.");
                }
            }
        }
    }

    private static ApiException bad(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_SCHEDULE", message);
    }
}