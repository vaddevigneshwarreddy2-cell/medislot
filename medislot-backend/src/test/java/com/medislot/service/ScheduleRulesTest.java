package com.medislot.service;

import com.medislot.exception.ApiException;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScheduleRulesTest {

    private ScheduleRules.Window w(int day, String s, String e) {
        return new ScheduleRules.Window(day, LocalTime.parse(s), LocalTime.parse(e));
    }

    @Test
    void validScheduleWithAdjacentWindowsPasses() {
        assertDoesNotThrow(() -> ScheduleRules.validate(
                List.of(w(1, "10:00", "12:00"), w(1, "12:00", "14:00"), w(2, "09:00", "13:00")), 30));
    }

    @Test
    void endBeforeStartIsRejected() {
        assertEquals("INVALID_SCHEDULE", assertThrows(ApiException.class,
                () -> ScheduleRules.validate(List.of(w(1, "13:00", "10:00")), 30)).getCode());
    }

    @Test
    void overlappingWindowsAreRejected() {
        assertThrows(ApiException.class, () -> ScheduleRules.validate(
                List.of(w(1, "10:00", "13:00"), w(1, "12:30", "15:00")), 30));
    }

    @Test
    void windowShorterThanAppointmentIsRejected() {
        assertThrows(ApiException.class, () -> ScheduleRules.validate(List.of(w(3, "10:00", "10:20")), 30));
    }
}