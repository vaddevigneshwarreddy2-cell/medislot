package com.medislot.controller;

import com.medislot.dto.ApiResponse;
import com.medislot.dto.StaffDtos.PatientQueueStatus;
import com.medislot.dto.StaffDtos.QrResponse;
import com.medislot.security.AuthUser;
import com.medislot.service.AppointmentQrService;
import com.medislot.service.QueueService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** A patient's QR code and queue status for their own appointment. */
@RestController
@RequestMapping("/api/appointments/{id}")
@PreAuthorize("hasRole('PATIENT')")
public class AppointmentQrController {

    private final AppointmentQrService qr;
    private final QueueService queue;

    public AppointmentQrController(AppointmentQrService qr, QueueService queue) {
        this.qr = qr;
        this.queue = queue;
    }

    @GetMapping("/qr")
    public ApiResponse<QrResponse> qr(@AuthenticationPrincipal AuthUser actor, @PathVariable Long id) {
        return ApiResponse.ok("OK", qr.qr(actor, id));
    }

    @GetMapping("/queue")
    public ApiResponse<PatientQueueStatus> queue(@AuthenticationPrincipal AuthUser actor, @PathVariable Long id) {
        return ApiResponse.ok("OK", queue.patientStatus(actor, id));
    }
}