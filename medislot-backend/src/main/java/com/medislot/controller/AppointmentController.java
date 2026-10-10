package com.medislot.controller;

import com.medislot.dto.ApiResponse;
import com.medislot.dto.AppointmentDtos.*;
import com.medislot.security.AuthUser;
import com.medislot.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Patient booking endpoints. Staff-side appointment endpoints arrive with Phase 6. */
@RestController
@RequestMapping("/api/appointments")
@PreAuthorize("hasRole('PATIENT')")
public class AppointmentController {

    private final AppointmentService service;

    public AppointmentController(AppointmentService service) {
        this.service = service;
    }

    @PostMapping("/hold")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AppointmentView> hold(@AuthenticationPrincipal AuthUser actor,
                                             @Valid @RequestBody HoldRequest request) {
        return ApiResponse.ok("Slot held. Confirm before the hold expires.", service.hold(actor, request));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AppointmentView> confirm(@AuthenticationPrincipal AuthUser actor,
                                                @Valid @RequestBody ConfirmRequest request) {
        return ApiResponse.ok("Appointment confirmed.", service.confirm(actor, request));
    }

    @GetMapping("/my")
    public ApiResponse<List<AppointmentView>> mine(@AuthenticationPrincipal AuthUser actor,
                                                   @RequestParam(defaultValue = "upcoming") String scope) {
        return ApiResponse.ok("OK", service.mine(actor, scope));
    }

    @GetMapping("/{id}")
    public ApiResponse<AppointmentView> get(@AuthenticationPrincipal AuthUser actor, @PathVariable Long id) {
        return ApiResponse.ok("OK", service.get(actor, id));
    }

    @PutMapping("/{id}/cancel")
    public ApiResponse<AppointmentView> cancel(@AuthenticationPrincipal AuthUser actor, @PathVariable Long id,
                                               @Valid @RequestBody(required = false) CancelRequest request) {
        return ApiResponse.ok("Appointment cancelled.",
                service.cancel(actor, id, request == null ? null : request.reason()));
    }

    @PutMapping("/{id}/reschedule")
    public ApiResponse<AppointmentView> reschedule(@AuthenticationPrincipal AuthUser actor, @PathVariable Long id,
                                                   @Valid @RequestBody RescheduleRequest request) {
        return ApiResponse.ok("Appointment rescheduled.", service.reschedule(actor, id, request));
    }
}