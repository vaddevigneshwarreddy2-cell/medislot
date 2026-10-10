package com.medislot.controller;

import com.medislot.dto.ApiResponse;
import com.medislot.dto.HospitalDtos.*;
import com.medislot.security.AuthUser;
import com.medislot.service.HospitalManagementService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** A HOSPITAL_ADMIN managing their own hospital. */
@RestController
@RequestMapping("/api/admin/hospital")
public class HospitalAdminController {

    private final HospitalManagementService service;

    public HospitalAdminController(HospitalManagementService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<Detail> get(@AuthenticationPrincipal AuthUser actor) {
        return ApiResponse.ok("OK", service.getOwn(actor));
    }

    @PutMapping
    public ApiResponse<Detail> updateProfile(@AuthenticationPrincipal AuthUser actor,
                                             @Valid @RequestBody ProfileRequest request) {
        return ApiResponse.ok("Hospital profile updated.", service.updateOwnProfile(actor, request));
    }

    @PutMapping("/location")
    public ApiResponse<Detail> updateLocation(@AuthenticationPrincipal AuthUser actor,
                                              @Valid @RequestBody LocationRequest request) {
        return ApiResponse.ok("Hospital location updated.", service.updateOwnLocation(actor, request));
    }
}