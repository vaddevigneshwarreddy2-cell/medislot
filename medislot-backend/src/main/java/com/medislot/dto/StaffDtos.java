package com.medislot.dto;

import jakarta.validation.constraints.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Reception, queue and QR shapes. Doctors never receive the mobile number; reception never receives the reason. */
public final class StaffDtos {

    private StaffDtos() {}

    public record StaffAppointment(Long appointmentId, String status, LocalDate appointmentDate,
                                   String appointmentTime, String endTime, Integer queueNumber, Integer position,
                                   boolean called, Instant checkedInAt, String source, String patientName,
                                   String patientMobile, Long doctorId, String doctorName, String specialization,
                                   String reason) {}

    public record DoctorQueueResponse(LocalDate date, int completedToday, List<StaffAppointment> entries) {}

    public record PatientQueueStatus(Long appointmentId, String status, Integer queueNumber, Integer patientsAhead,
                                     boolean called, boolean doctorBusy, Integer estimatedWaitMinutes) {}

    public record CheckInRequest(@NotBlank @Size(max = 100) String code) {}

    public record CheckInResult(Long appointmentId, String patientName, String doctorName, String appointmentTime,
                                Integer queueNumber, Integer position, String status) {}

    public record WalkInRequest(
            @NotBlank @Size(max = 150) String fullName,
            @NotBlank @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "must be 10-15 digits") String mobile,
            @Email @Size(max = 255) String email,
            @NotNull Long doctorId,
            @Pattern(regexp = DoctorDtos.TIME, message = "must be HH:mm") String time,
            @Size(max = 500) String reason) {}

    public record StatusChangeRequest(@NotBlank String status, @Size(max = 255) String note) {}

    public record QrResponse(Long appointmentId, String code, String imageDataUrl, String status,
                             LocalDate appointmentDate, String appointmentTime) {}
}