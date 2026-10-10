package com.medislot.util;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SlotGeneratorTest {

    @Test
    void tenToOneEveryThirtyMinutesGivesSixSlots() {
        List<LocalTime> s = SlotGenerator.startTimes(LocalTime.of(10, 0), LocalTime.of(13, 0), 30);
        assertEquals(6, s.size());
        assertEquals(LocalTime.of(10, 0), s.get(0));
        assertEquals(LocalTime.of(12, 30), s.get(5));
    }

    @Test
    void lastSlotMustFitInsideTheWindow() {
        List<LocalTime> s = SlotGenerator.startTimes(LocalTime.of(10, 0), LocalTime.of(12, 0), 45);
        assertEquals(List.of(LocalTime.of(10, 0), LocalTime.of(10, 45)), s);
    }

    @Test
    void durationLongerThanWindowGivesNoSlots() {
        assertTrue(SlotGenerator.startTimes(LocalTime.of(10, 0), LocalTime.of(10, 20), 30).isEmpty());
    }

    @Test
    void nonPositiveDurationGivesNoSlots() {
        assertTrue(SlotGenerator.startTimes(LocalTime.of(10, 0), LocalTime.of(13, 0), 0).isEmpty());
    }
}