package com.medislot.service;

import com.medislot.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;

/** Pure validation rules for consultations, kept separate so they are easy to test. */
public final class ClinicalRules {

    public static final int MAX_FOLLOW_UP_DAYS = 365;

    private ClinicalRules() {}

    public static void validateFollowUp(LocalDate appointmentDate, LocalDate followUp) {
        if (followUp == null) return;
        if (!followUp.isAfter(appointmentDate)) {
            throw bad("INVALID_FOLLOW_UP", "The follow-up date must be after the appointment date.");
        }
        if (followUp.isAfter(appointmentDate.plusDays(MAX_FOLLOW_UP_DAYS))) {
            throw bad("INVALID_FOLLOW_UP", "The follow-up date must be within a year of the appointment.");
        }
    }

    public static void requireContent(String notes, String diagnosis) {
        if (blank(notes) && blank(diagnosis)) {
            throw bad("CONSULTATION_CONTENT_REQUIRED", "Add consultation notes or a diagnosis before completing.");
        }
    }

    public static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static ApiException bad(String code, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, message);
    }
}