package com.medislot.service;

import com.medislot.exception.ApiException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ClinicalRulesTest {

    private final LocalDate day = LocalDate.of(2026, 10, 12);

    @Test
    void noFollowUpIsFine() {
        assertDoesNotThrow(() -> ClinicalRules.validateFollowUp(day, null));
    }

    @Test
    void followUpMustBeAfterTheAppointment() {
        assertEquals("INVALID_FOLLOW_UP", assertThrows(ApiException.class,
                () -> ClinicalRules.validateFollowUp(day, day)).getCode());
        assertThrows(ApiException.class, () -> ClinicalRules.validateFollowUp(day, day.minusDays(1)));
        assertDoesNotThrow(() -> ClinicalRules.validateFollowUp(day, day.plusDays(1)));
    }

    @Test
    void followUpMustBeWithinAYear() {
        assertDoesNotThrow(() -> ClinicalRules.validateFollowUp(day, day.plusDays(365)));
        assertThrows(ApiException.class, () -> ClinicalRules.validateFollowUp(day, day.plusDays(366)));
    }

    @Test
    void completionNeedsNotesOrDiagnosis() {
        assertEquals("CONSULTATION_CONTENT_REQUIRED", assertThrows(ApiException.class,
                () -> ClinicalRules.requireContent("  ", null)).getCode());
        assertDoesNotThrow(() -> ClinicalRules.requireContent("Fever", null));
        assertDoesNotThrow(() -> ClinicalRules.requireContent(null, "Viral fever"));
    }
}