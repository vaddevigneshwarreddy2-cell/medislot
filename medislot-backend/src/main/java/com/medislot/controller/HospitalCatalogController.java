package com.medislot.controller;

import com.medislot.dto.ApiResponse;
import com.medislot.dto.DoctorDtos.DoctorList;
import com.medislot.dto.HospitalDtos.DepartmentInfo;
import com.medislot.service.DepartmentService;
import com.medislot.service.DoctorService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Doctors and departments of one hospital (public). */
@RestController
@RequestMapping("/api/hospitals/{id}")
public class HospitalCatalogController {

    private final DoctorService doctors;
    private final DepartmentService departments;

    public HospitalCatalogController(DoctorService doctors, DepartmentService departments) {
        this.doctors = doctors;
        this.departments = departments;
    }

    @GetMapping("/doctors")
    public ApiResponse<DoctorList> doctors(@PathVariable Long id,
                                           @RequestParam(required = false) Long departmentId) {
        return ApiResponse.ok("OK", doctors.forHospital(id, departmentId));
    }

    @GetMapping("/departments")
    public ApiResponse<List<DepartmentInfo>> departments(@PathVariable Long id) {
        return ApiResponse.ok("OK", departments.publicList(id));
    }
}