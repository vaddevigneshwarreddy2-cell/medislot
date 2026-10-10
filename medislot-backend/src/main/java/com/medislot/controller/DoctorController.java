package com.medislot.controller;

import com.medislot.dto.ApiResponse;
import com.medislot.dto.DoctorDtos.*;
import com.medislot.service.DoctorService;
import com.medislot.service.SlotService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Public doctor discovery. Availability now excludes booked and held slots. */
@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    private final DoctorService service;
    private final SlotService slots;

    public DoctorController(DoctorService service, SlotService slots) {
        this.service = service;
        this.slots = slots;
    }

    @GetMapping
    public ApiResponse<DoctorList> search(@RequestParam(required = false) String name,
                                          @RequestParam(required = false) String specialization,
                                          @RequestParam(required = false) Long hospitalId,
                                          @RequestParam(required = false) Long departmentId,
                                          @RequestParam(required = false) Double latitude,
                                          @RequestParam(required = false) Double longitude,
                                          @RequestParam(required = false) Double radiusKm) {
        return ApiResponse.ok("OK", service.search(name, specialization, hospitalId, departmentId,
                latitude, longitude, radiusKm));
    }

    @GetMapping("/{id}")
    public ApiResponse<DoctorDetail> get(@PathVariable Long id) {
        return ApiResponse.ok("OK", service.get(id));
    }

    @GetMapping("/{id}/availability")
    public ApiResponse<AvailabilityResponse> availability(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok("OK", slots.availability(id, date));
    }

    @GetMapping("/{id}/availability/upcoming")
    public ApiResponse<List<DaySummary>> upcoming(@PathVariable Long id,
                                                  @RequestParam(defaultValue = "14") int days) {
        return ApiResponse.ok("OK", slots.upcoming(id, days));
    }
}