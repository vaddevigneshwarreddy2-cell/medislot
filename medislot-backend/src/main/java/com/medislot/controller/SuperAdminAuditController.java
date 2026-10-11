package com.medislot.controller;

import com.medislot.dto.ApiResponse;
import com.medislot.dto.AuditDtos.AuditView;
import com.medislot.dto.HospitalDtos.PageResponse;
import com.medislot.service.AuditQueryService;
import org.springframework.web.bind.annotation.*;

/** Platform audit trail. URL access is limited to SUPER_ADMIN in SecurityConfig. */
@RestController
@RequestMapping("/api/super-admin/audit-logs")
public class SuperAdminAuditController {

    private final AuditQueryService service;

    public SuperAdminAuditController(AuditQueryService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PageResponse<AuditView>> list(@RequestParam(required = false) String action,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.ok("OK", service.list(action, page, size));
    }
}