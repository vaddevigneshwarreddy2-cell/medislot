package com.medislot.controller;

import com.medislot.dto.ApiResponse;
import com.medislot.dto.NotificationDtos.AttendanceResult;
import com.medislot.security.AuthUser;
import com.medislot.service.AttendanceService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/appointments/{id}/attendance")
@PreAuthorize("hasRole('PATIENT')")
public class AttendanceController {

    private final AttendanceService service;

    public AttendanceController(AttendanceService service) {
        this.service = service;
    }

    @PutMapping
    public ApiResponse<AttendanceResult> confirm(@AuthenticationPrincipal AuthUser actor, @PathVariable Long id) {
        return ApiResponse.ok("Attendance confirmed.", service.confirm(actor, id));
    }
}