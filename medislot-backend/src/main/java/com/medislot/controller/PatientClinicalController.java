package com.medislot.controller;

import com.medislot.dto.ApiResponse;
import com.medislot.dto.ClinicalDtos.FollowUp;
import com.medislot.dto.ClinicalDtos.PrescriptionDetail;
import com.medislot.dto.ClinicalDtos.PrescriptionSummary;
import com.medislot.security.AuthUser;
import com.medislot.service.PrescriptionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** A patient's own prescriptions and follow-ups. URL access is limited to PATIENT in SecurityConfig. */
@RestController
@RequestMapping("/api/patient")
public class PatientClinicalController {

    private final PrescriptionService service;

    public PatientClinicalController(PrescriptionService service) {
        this.service = service;
    }

    @GetMapping("/prescriptions")
    public ApiResponse<List<PrescriptionSummary>> list(@AuthenticationPrincipal AuthUser actor) {
        return ApiResponse.ok("OK", service.listForPatient(actor));
    }

    @GetMapping("/prescriptions/{id}")
    public ApiResponse<PrescriptionDetail> get(@AuthenticationPrincipal AuthUser actor, @PathVariable Long id) {
        return ApiResponse.ok("OK", service.detailForPatient(actor, id));
    }

    @GetMapping("/prescriptions/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@AuthenticationPrincipal AuthUser actor, @PathVariable Long id) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"prescription-" + id + ".pdf\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(service.pdfForPatient(actor, id));
    }

    @GetMapping("/follow-ups")
    public ApiResponse<List<FollowUp>> followUps(@AuthenticationPrincipal AuthUser actor) {
        return ApiResponse.ok("OK", service.followUps(actor));
    }
}