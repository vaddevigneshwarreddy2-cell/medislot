package com.medislot.controller;

import com.medislot.dto.ApiResponse;
import com.medislot.dto.DoctorDtos.*;
import com.medislot.dto.HospitalDtos.ActiveRequest;
import com.medislot.security.AuthUser;
import com.medislot.service.DoctorManagementService;
import com.medislot.service.ScheduleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** A HOSPITAL_ADMIN managing the doctors of their own hospital. */
@RestController
@RequestMapping("/api/admin/doctors")
public class AdminDoctorController {

    private final DoctorManagementService doctors;
    private final ScheduleService schedule;

    public AdminDoctorController(DoctorManagementService doctors, ScheduleService schedule) {
        this.doctors = doctors;
        this.schedule = schedule;
    }

    @GetMapping
    public ApiResponse<List<AdminDoctor>> list(@AuthenticationPrincipal AuthUser actor) {
        return ApiResponse.ok("OK", doctors.list(actor));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AdminDoctor> create(@AuthenticationPrincipal AuthUser actor,
                                           @Valid @RequestBody CreateDoctorRequest request) {
        return ApiResponse.ok("Doctor created.", doctors.create(actor, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AdminDoctor> update(@AuthenticationPrincipal AuthUser actor, @PathVariable Long id,
                                           @Valid @RequestBody UpdateDoctorRequest request) {
        return ApiResponse.ok("Doctor updated.", doctors.update(actor, id, request));
    }

    @PatchMapping("/{id}/active")
    public ApiResponse<AdminDoctor> setActive(@AuthenticationPrincipal AuthUser actor, @PathVariable Long id,
                                              @Valid @RequestBody ActiveRequest request) {
        return ApiResponse.ok("Doctor status updated.", doctors.setActive(actor, id, request.active()));
    }

    @GetMapping("/{id}/availability")
    public ApiResponse<List<ScheduleWindow>> availability(@AuthenticationPrincipal AuthUser actor,
                                                          @PathVariable Long id) {
        return ApiResponse.ok("OK", schedule.getSchedule(actor, id));
    }

    @PutMapping("/{id}/availability")
    public ApiResponse<List<ScheduleWindow>> setAvailability(@AuthenticationPrincipal AuthUser actor,
                                                             @PathVariable Long id,
                                                             @Valid @RequestBody ScheduleRequest request) {
        return ApiResponse.ok("Availability updated.", schedule.replaceSchedule(actor, id, request));
    }

    @GetMapping("/{id}/leave")
    public ApiResponse<List<LeaveResponse>> leave(@AuthenticationPrincipal AuthUser actor, @PathVariable Long id) {
        return ApiResponse.ok("OK", schedule.listLeave(actor, id));
    }

    @PostMapping("/{id}/leave")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LeaveResponse> addLeave(@AuthenticationPrincipal AuthUser actor, @PathVariable Long id,
                                               @Valid @RequestBody LeaveRequest request) {
        return ApiResponse.ok("Leave added.", schedule.addLeave(actor, id, request));
    }

    @DeleteMapping("/{id}/leave/{leaveId}")
    public ApiResponse<Void> deleteLeave(@AuthenticationPrincipal AuthUser actor, @PathVariable Long id,
                                         @PathVariable Long leaveId) {
        schedule.deleteLeave(actor, id, leaveId);
        return ApiResponse.ok("Leave removed.", null);
    }
}