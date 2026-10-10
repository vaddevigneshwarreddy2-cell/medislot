package com.medislot.controller;

import com.medislot.dto.ApiResponse;
import com.medislot.dto.DoctorDtos.HolidayRequest;
import com.medislot.dto.DoctorDtos.HolidayResponse;
import com.medislot.security.AuthUser;
import com.medislot.service.ScheduleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/holidays")
public class AdminHolidayController {

    private final ScheduleService schedule;

    public AdminHolidayController(ScheduleService schedule) {
        this.schedule = schedule;
    }

    @GetMapping
    public ApiResponse<List<HolidayResponse>> list(@AuthenticationPrincipal AuthUser actor) {
        return ApiResponse.ok("OK", schedule.listHolidays(actor));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<HolidayResponse> add(@AuthenticationPrincipal AuthUser actor,
                                            @Valid @RequestBody HolidayRequest request) {
        return ApiResponse.ok("Holiday added.", schedule.addHoliday(actor, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal AuthUser actor, @PathVariable Long id) {
        schedule.deleteHoliday(actor, id);
        return ApiResponse.ok("Holiday removed.", null);
    }
}