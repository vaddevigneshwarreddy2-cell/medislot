package com.medislot.controller;

import com.medislot.dto.ApiResponse;
import com.medislot.dto.StaffDtos.DoctorQueueResponse;
import com.medislot.dto.StaffDtos.StaffAppointment;
import com.medislot.security.AuthUser;
import com.medislot.service.QueueService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** A DOCTOR working through today's queue. */
@RestController
@RequestMapping("/api/doctor/queue")
public class DoctorQueueController {

    private final QueueService queue;

    public DoctorQueueController(QueueService queue) {
        this.queue = queue;
    }

    @GetMapping
    public ApiResponse<DoctorQueueResponse> today(@AuthenticationPrincipal AuthUser actor) {
        return ApiResponse.ok("OK", queue.doctorQueue(actor));
    }

    @PostMapping("/call-next")
    public ApiResponse<StaffAppointment> callNext(@AuthenticationPrincipal AuthUser actor) {
        return ApiResponse.ok("Next patient called.", queue.callNext(actor));
    }

    @PostMapping("/{id}/call")
    public ApiResponse<StaffAppointment> call(@AuthenticationPrincipal AuthUser actor, @PathVariable Long id) {
        return ApiResponse.ok("Patient called.", queue.call(actor, id));
    }

    @PostMapping("/{id}/start")
    public ApiResponse<StaffAppointment> start(@AuthenticationPrincipal AuthUser actor, @PathVariable Long id) {
        return ApiResponse.ok("Consultation started.", queue.start(actor, id));
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<StaffAppointment> complete(@AuthenticationPrincipal AuthUser actor, @PathVariable Long id) {
        return ApiResponse.ok("Consultation completed.", queue.complete(actor, id));
    }
}