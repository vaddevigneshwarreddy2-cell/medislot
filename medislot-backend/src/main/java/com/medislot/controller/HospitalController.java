package com.medislot.controller;

import com.medislot.dto.ApiResponse;
import com.medislot.dto.HospitalDtos.*;
import com.medislot.service.HospitalService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hospitals")
public class HospitalController {

    private final HospitalService hospitalService;

    public HospitalController(HospitalService hospitalService) {
        this.hospitalService = hospitalService;
    }

    @GetMapping("/config")
    public ApiResponse<SearchConfig> config() {
        return ApiResponse.ok("OK", hospitalService.searchConfig());
    }

    @GetMapping("/nearby")
    public ApiResponse<NearbyResponse> nearby(@RequestParam double latitude,
                                              @RequestParam double longitude,
                                              @RequestParam(required = false) Double radiusKm) {
        return ApiResponse.ok("OK", hospitalService.nearby(latitude, longitude, radiusKm));
    }

    @GetMapping("/search")
    public ApiResponse<NearbyResponse> search(@RequestParam(required = false) String name,
                                              @RequestParam(required = false) Double latitude,
                                              @RequestParam(required = false) Double longitude,
                                              @RequestParam(required = false) Double radiusKm) {
        return ApiResponse.ok("OK", hospitalService.search(name, latitude, longitude, radiusKm));
    }

    @GetMapping
    public ApiResponse<PageResponse<Summary>> list(@RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok("OK", hospitalService.list(page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<Detail> get(@PathVariable Long id,
                                   @RequestParam(required = false) Double latitude,
                                   @RequestParam(required = false) Double longitude) {
        return ApiResponse.ok("OK", hospitalService.get(id, latitude, longitude));
    }
}