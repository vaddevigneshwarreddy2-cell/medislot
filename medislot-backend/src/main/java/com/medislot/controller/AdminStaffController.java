package com.medislot.controller;

import com.medislot.dto.AccountDtos.CreateAccountRequest;
import com.medislot.dto.AccountDtos.StaffMember;
import com.medislot.dto.ApiResponse;
import com.medislot.dto.HospitalDtos.ActiveRequest;
import com.medislot.entity.Role;
import com.medislot.security.AuthUser;
import com.medislot.service.AdminScope;
import com.medislot.service.StaffAccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** A HOSPITAL_ADMIN managing the receptionists of their own hospital. */
@RestController
@RequestMapping("/api/admin/staff")
public class AdminStaffController {

    private final StaffAccountService service;
    private final AdminScope scope;

    public AdminStaffController(StaffAccountService service, AdminScope scope) {
        this.service = service;
        this.scope = scope;
    }

    @GetMapping
    public ApiResponse<List<StaffMember>> list(@AuthenticationPrincipal AuthUser actor) {
        return ApiResponse.ok("OK", service.list(scope.hospitalId(actor), Role.RECEPTIONIST));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StaffMember> create(@AuthenticationPrincipal AuthUser actor,
                                           @Valid @RequestBody CreateAccountRequest request) {
        return ApiResponse.ok("Receptionist created.",
                service.create(actor, scope.hospitalId(actor), Role.RECEPTIONIST, request));
    }

    @PatchMapping("/{id}/active")
    public ApiResponse<StaffMember> setActive(@AuthenticationPrincipal AuthUser actor, @PathVariable Long id,
                                              @Valid @RequestBody ActiveRequest request) {
        return ApiResponse.ok("Status updated.",
                service.setActive(actor, scope.hospitalId(actor), Role.RECEPTIONIST, id, request.active()));
    }
}