package com.medislot.entity;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Every appointment state and the only moves allowed between them. AVAILABLE is a slot state, not stored. */
public enum AppointmentStatus {
    HELD, CONFIRMED, ATTENDANCE_PENDING, CHECKED_IN, WAITING, IN_CONSULTATION,
    COMPLETED, CANCELLED, NO_SHOW, EXPIRED, RESCHEDULED;

    /** Booked and not finished: these count towards the patient's active-appointment limit. */
    public static final Set<AppointmentStatus> ACTIVE_BOOKED =
            EnumSet.of(CONFIRMED, ATTENDANCE_PENDING, CHECKED_IN, WAITING, IN_CONSULTATION);

    /** Occupy a slot permanently (a HELD row occupies it only until it expires). Mirrors the unique index. */
    public static final Set<AppointmentStatus> BOOKED_OR_DONE =
            EnumSet.of(CONFIRMED, ATTENDANCE_PENDING, CHECKED_IN, WAITING, IN_CONSULTATION, COMPLETED);

    public static final Set<AppointmentStatus> OCCUPYING_SLOT =
            EnumSet.of(HELD, CONFIRMED, ATTENDANCE_PENDING, CHECKED_IN, WAITING, IN_CONSULTATION, COMPLETED);

    private static final Map<AppointmentStatus, Set<AppointmentStatus>> NEXT = new EnumMap<>(AppointmentStatus.class);

    static {
        NEXT.put(HELD, EnumSet.of(CONFIRMED, EXPIRED, CANCELLED));
        NEXT.put(CONFIRMED, EnumSet.of(ATTENDANCE_PENDING, CHECKED_IN, CANCELLED, RESCHEDULED, NO_SHOW));
        NEXT.put(ATTENDANCE_PENDING, EnumSet.of(CONFIRMED, CHECKED_IN, CANCELLED, RESCHEDULED, NO_SHOW));
        NEXT.put(CHECKED_IN, EnumSet.of(WAITING, CANCELLED));
        NEXT.put(WAITING, EnumSet.of(IN_CONSULTATION, CANCELLED, NO_SHOW));
        NEXT.put(IN_CONSULTATION, EnumSet.of(COMPLETED));
        // COMPLETED, CANCELLED, NO_SHOW, EXPIRED and RESCHEDULED are final.
    }

    public boolean canTransitionTo(AppointmentStatus to) {
        return NEXT.getOrDefault(this, EnumSet.noneOf(AppointmentStatus.class)).contains(to);
    }
}