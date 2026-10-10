package com.medislot.repository;

import java.time.LocalDate;

/** A finalized prescription joined with doctor, hospital and patient. */
public record PrescriptionRow(Long id, Long appointmentId, LocalDate date, String doctorName, String specialization,
                              String qualification, String hospitalName, String hospitalAddress, String hospitalCity,
                              String patientName, String diagnosis, String instructions, LocalDate followUpDate,
                              Long doctorId, Long patientId) {}