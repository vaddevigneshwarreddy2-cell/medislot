package com.medislot.repository;

import com.medislot.entity.AppointmentStatus;

import java.time.Instant;
import java.time.LocalDate;

/** One appointment joined with patient and doctor names, for reception and doctor screens. */
public record StaffRow(Long id, AppointmentStatus status, LocalDate date, Instant slotStart, Instant slotEnd,
                       Integer queueNumber, Instant checkedInAt, Instant calledAt, String source, String reason,
                       String patientName, String patientMobile, Long doctorId, String doctorName,
                       String specialization, Integer durationMin) {}