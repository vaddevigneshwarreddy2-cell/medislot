package com.medislot.controller;

import com.medislot.dto.ApiResponse;
import com.medislot.dto.HospitalDtos.*;
import com.medislot.security.AuthUser;
import com.medislot.service.HospitalManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Platform-level hospital management for SUPER_ADMIN. */
@RestController
@RequestMapping("/api/super-admin/hospitals")
public class SuperAdminHospitalController {

    private final HospitalManagementService service;

    public SuperAdminHospitalController(HospitalManagementService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Detail> create(@AuthenticationPrincipal AuthUser actor,
                                      @Valid @RequestBody CreateRequest request) {
        return ApiResponse.ok("Hospital created.", service.create(actor, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Detail> updateProfile(@AuthenticationPrincipal AuthUser actor, @PathVariable Long id,
                                             @Valid @RequestBody ProfileRequest request) {
        return ApiResponse.ok("Hospital profile updated.", service.updateProfile(actor, id, request));
    }

    @PutMapping("/{id}/location")
    public ApiResponse<Detail> updateLocation(@AuthenticationPrincipal AuthUser actor, @PathVariable Long id,
                                              @Valid @RequestBody LocationRequest request) {
        return ApiResponse.ok("Hospital location updated.", service.updateLocation(actor, id, request));
    }

    @PatchMapping("/{id}/active")
    public ApiResponse<Detail> setActive(@AuthenticationPrincipal AuthUser actor, @PathVariable Long id,
                                         @Valid @RequestBody ActiveRequest request) {
        return ApiResponse.ok("Hospital status updated.", service.setActive(actor, id, request.active()));
    }
}