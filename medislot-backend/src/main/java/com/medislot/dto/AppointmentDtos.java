package com.medislot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class AppointmentDtos {

    private AppointmentDtos() {}

    public record HoldRequest(
            @NotNull Long doctorId,
            @NotNull LocalDate date,
            @NotBlank @Pattern(regexp = DoctorDtos.TIME, message = "must be HH:mm") String time) {}

    public record ConfirmRequest(@NotNull Long appointmentId, @Size(max = 500) String reason) {}

    public record CancelRequest(@Size(max = 255) String reason) {}

    public record RescheduleRequest(
            @NotNull LocalDate date,
            @NotBlank @Pattern(regexp = DoctorDtos.TIME, message = "must be HH:mm") String time) {}

    public record HospitalInfo(Long id, String name, String address, BigDecimal latitude, BigDecimal longitude) {}

    public record AppointmentView(Long appointmentId, String status, LocalDate appointmentDate,
                                  String appointmentTime, String endTime, Instant holdExpiresAt,
                                  String doctorName, String specialization, String department,
                                  HospitalInfo hospital, int rescheduleCount, Long rescheduledFromId,
                                  String reason, boolean canCancel, boolean canReschedule) {}
}