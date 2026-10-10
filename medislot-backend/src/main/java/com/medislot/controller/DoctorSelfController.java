package com.medislot.controller;

import com.medislot.dto.ApiResponse;
import com.medislot.dto.DoctorDtos.*;
import com.medislot.security.AuthUser;
import com.medislot.service.DoctorManagementService;
import com.medislot.service.ScheduleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** A DOCTOR managing their own profile, working hours and leave. */
@RestController
@RequestMapping("/api/doctor")
public class DoctorSelfController {

    private final DoctorManagementService doctors;
    private final ScheduleService schedule;

    public DoctorSelfController(DoctorManagementService doctors, ScheduleService schedule) {
        this.doctors = doctors;
        this.schedule = schedule;
    }

    @GetMapping("/profile")
    public ApiResponse<AdminDoctor> profile(@AuthenticationPrincipal AuthUser actor) {
        return ApiResponse.ok("OK", doctors.ownProfile(actor));
    }

    @PutMapping("/profile")
    public ApiResponse<AdminDoctor> updateProfile(@AuthenticationPrincipal AuthUser actor,
                                                  @Valid @RequestBody SelfProfileRequest request) {
        return ApiResponse.ok("Profile updated.", doctors.updateOwnProfile(actor, request));
    }

    @GetMapping("/availability")
    public ApiResponse<List<ScheduleWindow>> availability(@AuthenticationPrincipal AuthUser actor) {
        return ApiResponse.ok("OK", schedule.getSchedule(actor, null));
    }

    @PutMapping("/availability")
    public ApiResponse<List<ScheduleWindow>> setAvailability(@AuthenticationPrincipal AuthUser actor,
                                                             @Valid @RequestBody ScheduleRequest request) {
        return ApiResponse.ok("Availability updated.", schedule.replaceSchedule(actor, null, request));
    }

    @GetMapping("/leave")
    public ApiResponse<List<LeaveResponse>> leave(@AuthenticationPrincipal AuthUser actor) {
        return ApiResponse.ok("OK", schedule.listLeave(actor, null));
    }

    @PostMapping("/leave")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LeaveResponse> addLeave(@AuthenticationPrincipal AuthUser actor,
                                               @Valid @RequestBody LeaveRequest request) {
        return ApiResponse.ok("Leave added.", schedule.addLeave(actor, null, request));
    }

    @DeleteMapping("/leave/{leaveId}")
    public ApiResponse<Void> deleteLeave(@AuthenticationPrincipal AuthUser actor, @PathVariable Long leaveId) {
        schedule.deleteLeave(actor, null, leaveId);
        return ApiResponse.ok("Leave removed.", null);
    }
}