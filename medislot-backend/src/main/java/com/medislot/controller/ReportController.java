package com.medislot.controller;

import com.medislot.dto.ApiResponse;
import com.medislot.dto.ReportDtos.Report;
import com.medislot.security.AuthUser;
import com.medislot.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/** Hospital admin reports (URL access is limited to admins in SecurityConfig; the service limits the hospital). */
@RestController
@RequestMapping("/api/admin/reports")
public class ReportController {

    private final ReportService service;

    public ReportController(ReportService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<Report> report(
            @AuthenticationPrincipal AuthUser actor,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String granularity) {
        return ApiResponse.ok("OK", service.build(actor, from, to, granularity));
    }

    @GetMapping("/export")
    public ResponseEntity<String> export(
            @AuthenticationPrincipal AuthUser actor,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String granularity) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"medislot-report.csv\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(service.export(actor, from, to, granularity));
    }
}