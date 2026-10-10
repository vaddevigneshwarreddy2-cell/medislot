package com.medislot.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/** Consultation and prescription shapes. The doctor's private notes appear only in ConsultationView. */
public final class ClinicalDtos {

    private ClinicalDtos() {}

    public record MedicineInput(
            @NotBlank @Size(max = 150) String medicineName,
            @NotBlank @Size(max = 100) String dosage,
            @NotBlank @Size(max = 100) String frequency,
            @NotBlank @Size(max = 100) String duration,
            @Size(max = 255) String instructions) {}

    public record ConsultationRequest(
            @Size(max = 5000) String notes,
            @Size(max = 2000) String diagnosis,
            @Size(max = 2000) String instructions,
            LocalDate followUpDate,
            @Size(max = 20) List<@Valid MedicineInput> medicines) {}

    public record MedicineView(String medicineName, String dosage, String frequency, String duration,
                               String instructions) {}

    public record ConsultationView(Long appointmentId, String status, String patientName, String reason,
                                   LocalDate appointmentDate, String appointmentTime, String notes,
                                   String diagnosis, String instructions, LocalDate followUpDate,
                                   List<MedicineView> medicines, boolean finalized, boolean editable,
                                   Long prescriptionId) {}

    public record PrescriptionSummary(Long id, Long appointmentId, LocalDate date, String doctorName,
                                      String specialization, String hospitalName, int medicineCount,
                                      LocalDate followUpDate, String patientName) {}

    public record PrescriptionDetail(Long id, Long appointmentId, LocalDate date, String doctorName,
                                     String specialization, String qualification, String hospitalName,
                                     String hospitalAddress, String patientName, String diagnosis,
                                     String instructions, LocalDate followUpDate, List<MedicineView> medicines) {}

    public record FollowUp(Long appointmentId, LocalDate followUpDate, Long doctorId, String doctorName,
                           String specialization, String hospitalName) {}
}