package com.medislot.controller;

import com.medislot.dto.ApiResponse;
import com.medislot.dto.DoctorDtos.AdminDepartment;
import com.medislot.dto.DoctorDtos.DepartmentRequest;
import com.medislot.dto.HospitalDtos.ActiveRequest;
import com.medislot.security.AuthUser;
import com.medislot.service.DepartmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/departments")
public class AdminDepartmentController {

    private final DepartmentService service;

    public AdminDepartmentController(DepartmentService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<AdminDepartment>> list(@AuthenticationPrincipal AuthUser actor) {
        return ApiResponse.ok("OK", service.list(actor));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AdminDepartment> create(@AuthenticationPrincipal AuthUser actor,
                                               @Valid @RequestBody DepartmentRequest request) {
        return ApiResponse.ok("Department created.", service.create(actor, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AdminDepartment> update(@AuthenticationPrincipal AuthUser actor, @PathVariable Long id,
                                               @Valid @RequestBody DepartmentRequest request) {
        return ApiResponse.ok("Department updated.", service.update(actor, id, request));
    }

    @PatchMapping("/{id}/active")
    public ApiResponse<AdminDepartment> setActive(@AuthenticationPrincipal AuthUser actor, @PathVariable Long id,
                                                  @Valid @RequestBody ActiveRequest request) {
        return ApiResponse.ok("Department status updated.", service.setActive(actor, id, request.active()));
    }
}