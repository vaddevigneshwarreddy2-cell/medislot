package com.medislot.repository;

import java.time.LocalDate;

public record FollowUpRow(Long appointmentId, LocalDate followUpDate, Long doctorId, String doctorName,
                          String specialization, String hospitalName) {}