package com.medislot.service;

import com.medislot.config.AppProperties;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class QrCodeServiceTest {

    private final QrCodeService qr = new QrCodeService(new AppProperties(null, null, null,
            new AppProperties.Jwt("test-secret-test-secret-test-secret-123", 15, 7), null));
    private final Instant slot = Instant.parse("2026-10-12T04:30:00Z");

    @Test
    void codeRoundTripsAndContainsNoPersonalData() {
        String code = qr.code(42, slot);
        assertTrue(code.matches("^MS1\\.42\\.[A-Za-z0-9_-]{22}$"));
        QrCodeService.Parsed p = qr.parse(code).orElseThrow();
        assertEquals(42L, p.appointmentId());
        assertTrue(qr.valid(p, slot));
    }

    @Test
    void differentSlotInvalidatesTheCode() {
        QrCodeService.Parsed p = qr.parse(qr.code(42, slot)).orElseThrow();
        assertFalse(qr.valid(p, slot.plusSeconds(1)));
    }

    @Test
    void editedIdOrSignatureIsRejected() {
        String code = qr.code(42, slot);
        QrCodeService.Parsed other = qr.parse(code.replace("MS1.42.", "MS1.43.")).orElseThrow();
        assertFalse(qr.valid(other, slot));
        String last = code.substring(code.length() - 1);
        String tampered = code.substring(0, code.length() - 1) + (last.equals("A") ? "B" : "A");
        assertFalse(qr.valid(qr.parse(tampered).orElseThrow(), slot));
    }

    @Test
    void garbageDoesNotParse() {
        assertTrue(qr.parse(null).isEmpty());
        assertTrue(qr.parse("hello").isEmpty());
        assertTrue(qr.parse("MS1.abc.def").isEmpty());
        assertTrue(qr.parse("MS1.42.short").isEmpty());
    }

    @Test
    void pngDataUrlIsProduced() {
        assertTrue(qr.pngDataUrl("MS1.42.abcdefghijklmnopqrstuv").startsWith("data:image/png;base64,"));
    }
}