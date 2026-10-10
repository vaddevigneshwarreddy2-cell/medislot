package com.medislot.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

/** Doctor, schedule, leave and department request/response shapes. */
public final class DoctorDtos {

    public static final String TIME = "^([01]\\d|2[0-3]):[0-5]\\d$";

    private DoctorDtos() {}

    public record ScheduleWindow(int dayOfWeek, String start, String end) {}

    public record DoctorSummary(Long id, String fullName, String specialization, String qualification,
                                Integer experienceYears, Integer consultationDurationMin,
                                Long hospitalId, String hospitalName, String hospitalCity,
                                Long departmentId, String departmentName, Double distanceKm) {}

    public record DoctorList(int count, List<DoctorSummary> doctors) {}

    public record DoctorDetail(Long id, String fullName, String specialization, String qualification,
                               Integer experienceYears, Integer consultationDurationMin, String bio,
                               Long hospitalId, String hospitalName, String hospitalCity,
                               Long departmentId, String departmentName, List<ScheduleWindow> schedule) {}

    public record AdminDoctor(Long id, String fullName, String email, String specialization, String qualification,
                              Integer experienceYears, Integer consultationDurationMin, String bio, boolean active,
                              Long hospitalId, Long departmentId, String departmentName) {}

    public record Slot(String start, String end) {}

    public record AvailabilityResponse(Long doctorId, LocalDate date, int durationMinutes,
                                       List<Slot> slots, String unavailableReason) {}

    public record DaySummary(LocalDate date, int slotCount, String unavailableReason) {}

    public record WindowRequest(
            @Min(1) @Max(7) int dayOfWeek,
            @NotBlank @Pattern(regexp = TIME, message = "must be HH:mm") String start,
            @NotBlank @Pattern(regexp = TIME, message = "must be HH:mm") String end) {}

    public record ScheduleRequest(@NotNull @Size(max = 21) List<@jakarta.validation.Valid WindowRequest> windows) {}

    public record LeaveRequest(@NotNull LocalDate leaveDate, @Size(max = 255) String reason) {}

    public record LeaveResponse(Long id, LocalDate leaveDate, String reason) {}

    public record HolidayRequest(@NotNull LocalDate holidayDate, @NotBlank @Size(max = 255) String description) {}

    public record HolidayResponse(Long id, LocalDate holidayDate, String description) {}

    public record CreateDoctorRequest(
            @NotBlank @Size(max = 150) String fullName,
            @NotBlank @Email @Size(max = 255) String email,
            @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "must be 10-15 digits") String mobile,
            @NotBlank @Size(min = 8, max = 72)
            @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                     message = "must contain an uppercase letter, a lowercase letter and a digit") String password,
            @NotNull Long departmentId,
            @NotBlank @Size(max = 100) String specialization,
            @Size(max = 200) String qualification,
            @Min(0) @Max(70) Integer experienceYears,
            @Min(5) @Max(240) Integer consultationDurationMin,
            @Size(max = 1000) String bio) {}

    public record UpdateDoctorRequest(
            @NotNull Long departmentId,
            @NotBlank @Size(max = 100) String specialization,
            @Size(max = 200) String qualification,
            @Min(0) @Max(70) Integer experienceYears,
            @Min(5) @Max(240) Integer consultationDurationMin,
            @Size(max = 1000) String bio) {}

    public record SelfProfileRequest(
            @Size(max = 200) String qualification,
            @Min(0) @Max(70) Integer experienceYears,
            @Size(max = 1000) String bio) {}

    public record DepartmentRequest(@NotBlank @Size(max = 100) String name, @Size(max = 500) String description) {}

    public record AdminDepartment(Long id, String name, String description, boolean active) {}
}