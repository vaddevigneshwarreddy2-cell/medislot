package com.medislot.controller;

import com.medislot.dto.ApiResponse;
import com.medislot.dto.DashboardDtos.*;
import com.medislot.dto.StaffDtos.StaffAppointment;
import com.medislot.security.AuthUser;
import com.medislot.service.DashboardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Role-specific home-screen data. URL access per role is enforced in SecurityConfig. */
@RestController
@RequestMapping("/api")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/patient/dashboard")
    public ApiResponse<PatientDashboard> patient(@AuthenticationPrincipal AuthUser actor) {
        return ApiResponse.ok("OK", service.patient(actor));
    }

    @GetMapping("/doctor/dashboard")
    public ApiResponse<DoctorDashboard> doctor(@AuthenticationPrincipal AuthUser actor) {
        return ApiResponse.ok("OK", service.doctor(actor));
    }

    @GetMapping("/reception/attention")
    public ApiResponse<List<StaffAppointment>> attention(@AuthenticationPrincipal AuthUser actor) {
        return ApiResponse.ok("OK", service.attention(actor));
    }

    @GetMapping("/admin/dashboard")
    public ApiResponse<AdminDashboard> admin(@AuthenticationPrincipal AuthUser actor,
                                             @RequestParam(defaultValue = "today") String period) {
        return ApiResponse.ok("OK", service.admin(actor, period));
    }
}