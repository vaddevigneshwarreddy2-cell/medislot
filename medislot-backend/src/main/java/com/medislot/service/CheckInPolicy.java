package com.medislot.service;

import com.medislot.entity.AppointmentStatus;
import com.medislot.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Pure check-in rules, kept separate so they are easy to test. */
public final class CheckInPolicy {

    private CheckInPolicy() {}

    public static void validate(AppointmentStatus status, LocalDate appointmentDate, LocalDate today,
                                Instant slotStart, Instant now, int earlyMinutes, boolean enforceEarly) {
        switch (status) {
            case CONFIRMED, ATTENDANCE_PENDING -> { }
            case CHECKED_IN, WAITING, IN_CONSULTATION, COMPLETED ->
                    throw conflict("ALREADY_CHECKED_IN", "This patient is already checked in or has been seen.");
            case RESCHEDULED ->
                    throw conflict("APPOINTMENT_RESCHEDULED", "This appointment was rescheduled. Use the new QR code.");
            case CANCELLED -> throw conflict("APPOINTMENT_CANCELLED", "This appointment was cancelled.");
            case NO_SHOW -> throw conflict("APPOINTMENT_NO_SHOW", "This appointment was marked as a no-show.");
            default -> throw conflict("APPOINTMENT_NOT_CONFIRMED", "This appointment is not confirmed.");
        }
        if (!appointmentDate.equals(today)) {
            throw conflict("WRONG_DATE", "This appointment is on " + appointmentDate + ", not today.");
        }
        if (enforceEarly && now.isBefore(slotStart.minus(earlyMinutes, ChronoUnit.MINUTES))) {
            throw conflict("TOO_EARLY", "Check-in opens " + earlyMinutes + " minutes before the appointment.");
        }
    }

    private static ApiException conflict(String code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message);
    }
}