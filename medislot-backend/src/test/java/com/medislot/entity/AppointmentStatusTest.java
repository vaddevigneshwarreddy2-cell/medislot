package com.medislot.entity;

import org.junit.jupiter.api.Test;

import static com.medislot.entity.AppointmentStatus.*;
import static org.junit.jupiter.api.Assertions.*;

class AppointmentStatusTest {

    @Test
    void happyPathIsAllowed() {
        assertTrue(HELD.canTransitionTo(CONFIRMED));
        assertTrue(CONFIRMED.canTransitionTo(CHECKED_IN));
        assertTrue(CHECKED_IN.canTransitionTo(WAITING));
        assertTrue(WAITING.canTransitionTo(IN_CONSULTATION));
        assertTrue(IN_CONSULTATION.canTransitionTo(COMPLETED));
    }

    @Test
    void skippingStepsIsRejected() {
        assertFalse(HELD.canTransitionTo(CHECKED_IN));
        assertFalse(CONFIRMED.canTransitionTo(COMPLETED));
        assertFalse(CONFIRMED.canTransitionTo(IN_CONSULTATION));
        assertFalse(WAITING.canTransitionTo(COMPLETED));
    }

    @Test
    void finalStatesHaveNoExits() {
        for (AppointmentStatus from : new AppointmentStatus[]{COMPLETED, CANCELLED, NO_SHOW, EXPIRED, RESCHEDULED}) {
            for (AppointmentStatus to : AppointmentStatus.values()) {
                assertFalse(from.canTransitionTo(to), from + " must not move to " + to);
            }
        }
    }

    @Test
    void cancelledOrExpiredAppointmentsDoNotOccupyASlot() {
        assertTrue(OCCUPYING_SLOT.contains(HELD));
        assertTrue(OCCUPYING_SLOT.contains(CONFIRMED));
        assertFalse(OCCUPYING_SLOT.contains(CANCELLED));
        assertFalse(OCCUPYING_SLOT.contains(EXPIRED));
        assertFalse(OCCUPYING_SLOT.contains(RESCHEDULED));
        assertFalse(OCCUPYING_SLOT.contains(NO_SHOW));
    }

    @Test
    void activeBookedExcludesHoldsAndCompletedVisits() {
        assertFalse(ACTIVE_BOOKED.contains(HELD));
        assertFalse(ACTIVE_BOOKED.contains(COMPLETED));
        assertTrue(ACTIVE_BOOKED.contains(CONFIRMED));
        assertTrue(ACTIVE_BOOKED.contains(WAITING));
    }
}