package com.medislot.service;

import com.medislot.dto.ClinicalDtos.MedicineView;
import com.medislot.dto.ClinicalDtos.PrescriptionDetail;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PrescriptionPdfServiceTest {

    private final PrescriptionPdfService service = new PrescriptionPdfService();

    private PrescriptionDetail detail(List<MedicineView> meds) {
        return new PrescriptionDetail(7L, 21L, LocalDate.of(2026, 10, 12), "Dr. Arjun Rao", "General Physician",
                "MBBS, MD", "MediCare Hospital", "Road No. 12, Banjara Hills, Hyderabad", "Demo Patient",
                "Viral pharyngitis", "Rest and fluids", LocalDate.of(2026, 10, 19), meds);
    }

    @Test
    void producesAPdfDocument() {
        byte[] pdf = service.render(detail(List.of(
                new MedicineView("Paracetamol 500 mg", "1 tablet", "3 times a day", "5 days", "After food"))));
        assertTrue(pdf.length > 500);
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
    }

    @Test
    void worksWithoutMedicinesOrOptionalFields() {
        PrescriptionDetail d = new PrescriptionDetail(1L, 2L, LocalDate.of(2026, 10, 12), "Dr. A", "GP", null,
                "H", "Addr", "P", null, null, null, List.of());
        assertEquals("%PDF", new String(service.render(d), 0, 4, StandardCharsets.US_ASCII));
    }
}