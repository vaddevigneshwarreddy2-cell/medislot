package com.medislot.controller;

import com.medislot.dto.ApiResponse;
import com.medislot.dto.DoctorDtos.DoctorSummary;
import com.medislot.dto.StaffDtos.*;
import com.medislot.security.AuthUser;
import com.medislot.service.ReceptionService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Receptionist endpoints (URL access is limited to the RECEPTIONIST role in SecurityConfig). */
@RestController
@RequestMapping("/api/reception")
public class ReceptionController {

    private final ReceptionService service;

    public ReceptionController(ReceptionService service) {
        this.service = service;
    }

    @GetMapping("/doctors")
    public ApiResponse<List<DoctorSummary>> doctors(@AuthenticationPrincipal AuthUser actor) {
        return ApiResponse.ok("OK", service.doctors(actor));
    }

    @GetMapping("/appointments")
    public ApiResponse<List<StaffAppointment>> appointments(
            @AuthenticationPrincipal AuthUser actor,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok("OK", service.list(actor, date, doctorId, q, status));
    }

    @GetMapping("/queue")
    public ApiResponse<List<StaffAppointment>> queue(@AuthenticationPrincipal AuthUser actor,
                                                     @RequestParam(required = false) Long doctorId) {
        return ApiResponse.ok("OK", service.queue(actor, doctorId));
    }

    @PostMapping("/check-in")
    public ApiResponse<CheckInResult> checkIn(@AuthenticationPrincipal AuthUser actor,
                                              @Valid @RequestBody CheckInRequest request) {
        return ApiResponse.ok("Patient checked in.", service.checkInByCode(actor, request.code()));
    }

    @PostMapping("/appointments/{id}/check-in")
    public ApiResponse<CheckInResult> checkInManual(@AuthenticationPrincipal AuthUser actor, @PathVariable Long id) {
        return ApiResponse.ok("Patient checked in.", service.checkInById(actor, id));
    }

    @PostMapping("/walk-ins")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StaffAppointment> walkIn(@AuthenticationPrincipal AuthUser actor,
                                                @Valid @RequestBody WalkInRequest request) {
        return ApiResponse.ok("Walk-in registered and added to the queue.", service.walkIn(actor, request));
    }

    @PutMapping("/appointments/{id}/status")
    public ApiResponse<StaffAppointment> status(@AuthenticationPrincipal AuthUser actor, @PathVariable Long id,
                                                @Valid @RequestBody StatusChangeRequest request) {
        return ApiResponse.ok("Status updated.", service.changeStatus(actor, id, request));
    }
}