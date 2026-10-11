package com.medislot.controller;

import com.medislot.dto.AccountDtos.CreateAccountRequest;
import com.medislot.dto.AccountDtos.HospitalRow;
import com.medislot.dto.AccountDtos.StaffMember;
import com.medislot.dto.ApiResponse;
import com.medislot.dto.HospitalDtos.ActiveRequest;
import com.medislot.entity.Role;
import com.medislot.security.AuthUser;
import com.medislot.service.StaffAccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** SUPER_ADMIN: all hospitals and their hospital-admin accounts. URL access is limited in SecurityConfig. */
@RestController
@RequestMapping("/api/super-admin")
public class SuperAdminDirectoryController {

    private final StaffAccountService service;

    public SuperAdminDirectoryController(StaffAccountService service) {
        this.service = service;
    }

    @GetMapping("/hospitals")
    public ApiResponse<List<HospitalRow>> hospitals() {
        return ApiResponse.ok("OK", service.allHospitals());
    }

    @GetMapping("/hospitals/{id}/admins")
    public ApiResponse<List<StaffMember>> admins(@PathVariable Long id) {
        return ApiResponse.ok("OK", service.list(id, Role.HOSPITAL_ADMIN));
    }

    @PostMapping("/hospitals/{id}/admins")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StaffMember> createAdmin(@AuthenticationPrincipal AuthUser actor, @PathVariable Long id,
                                                @Valid @RequestBody CreateAccountRequest request) {
        return ApiResponse.ok("Hospital admin created.", service.create(actor, id, Role.HOSPITAL_ADMIN, request));
    }

    @PatchMapping("/hospitals/{id}/admins/{staffId}/active")
    public ApiResponse<StaffMember> setAdminActive(@AuthenticationPrincipal AuthUser actor, @PathVariable Long id,
                                                   @PathVariable Long staffId,
                                                   @Valid @RequestBody ActiveRequest request) {
        return ApiResponse.ok("Status updated.",
                service.setActive(actor, id, Role.HOSPITAL_ADMIN, staffId, request.active()));
    }
}