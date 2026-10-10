package com.medislot.controller;

import com.medislot.dto.ApiResponse;
import com.medislot.dto.ClinicalDtos.ConsultationRequest;
import com.medislot.dto.ClinicalDtos.ConsultationView;
import com.medislot.security.AuthUser;
import com.medislot.service.ConsultationService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** The treating doctor's consultation. URL access is limited to the DOCTOR role in SecurityConfig. */
@RestController
@RequestMapping("/api/doctor/consultations")
public class DoctorConsultationController {

    private final ConsultationService service;

    public DoctorConsultationController(ConsultationService service) {
        this.service = service;
    }

    @GetMapping("/{appointmentId}")
    public ApiResponse<ConsultationView> get(@AuthenticationPrincipal AuthUser actor, @PathVariable Long appointmentId) {
        return ApiResponse.ok("OK", service.get(actor, appointmentId));
    }

    @PutMapping("/{appointmentId}")
    public ApiResponse<ConsultationView> save(@AuthenticationPrincipal AuthUser actor, @PathVariable Long appointmentId,
                                              @Valid @RequestBody ConsultationRequest request) {
        return ApiResponse.ok("Draft saved.", service.save(actor, appointmentId, request));
    }

    @PostMapping("/{appointmentId}/complete")
    public ApiResponse<ConsultationView> complete(@AuthenticationPrincipal AuthUser actor, @PathVariable Long appointmentId,
                                                  @Valid @RequestBody ConsultationRequest request) {
        return ApiResponse.ok("Consultation completed.", service.complete(actor, appointmentId, request));
    }
}