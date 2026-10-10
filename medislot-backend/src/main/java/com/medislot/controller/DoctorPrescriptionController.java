package com.medislot.controller;

import com.medislot.dto.ApiResponse;
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

/** Prescriptions the calling doctor issued. */
@RestController
@RequestMapping("/api/doctor/prescriptions")
public class DoctorPrescriptionController {

    private final PrescriptionService service;

    public DoctorPrescriptionController(PrescriptionService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<PrescriptionSummary>> list(@AuthenticationPrincipal AuthUser actor) {
        return ApiResponse.ok("OK", service.listForDoctor(actor));
    }

    @GetMapping("/{id}")
    public ApiResponse<PrescriptionDetail> get(@AuthenticationPrincipal AuthUser actor, @PathVariable Long id) {
        return ApiResponse.ok("OK", service.detailForDoctor(actor, id));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@AuthenticationPrincipal AuthUser actor, @PathVariable Long id) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"prescription-" + id + ".pdf\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(service.pdfForDoctor(actor, id));
    }
}