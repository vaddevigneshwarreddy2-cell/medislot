package com.medislot.service;

import com.medislot.dto.DoctorDtos.AdminDepartment;
import com.medislot.dto.DoctorDtos.DepartmentRequest;
import com.medislot.dto.HospitalDtos.DepartmentInfo;
import com.medislot.entity.Department;
import com.medislot.exception.ApiException;
import com.medislot.repository.DepartmentRepository;
import com.medislot.repository.DoctorRepository;
import com.medislot.repository.HospitalRepository;
import com.medislot.security.AuthUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DepartmentService {

    private final DepartmentRepository departments;
    private final DoctorRepository doctors;
    private final HospitalRepository hospitals;
    private final AdminScope scope;
    private final AuditService audit;

    public DepartmentService(DepartmentRepository departments, DoctorRepository doctors,
                             HospitalRepository hospitals, AdminScope scope, AuditService audit) {
        this.departments = departments;
        this.doctors = doctors;
        this.hospitals = hospitals;
        this.scope = scope;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<DepartmentInfo> publicList(Long hospitalId) {
        hospitals.findByIdAndActiveTrue(hospitalId).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "HOSPITAL_NOT_FOUND", "Hospital not found."));
        return departments.findByHospitalIdAndActiveTrueOrderByNameAsc(hospitalId).stream()
                .map(d -> new DepartmentInfo(d.getId(), d.getName(), d.getDescription())).toList();
    }

    @Transactional(readOnly = true)
    public List<AdminDepartment> list(AuthUser actor) {
        return departments.findByHospitalIdOrderByNameAsc(scope.hospitalId(actor)).stream().map(this::toDto).toList();
    }

    public AdminDepartment create(AuthUser actor, DepartmentRequest r) {
        Long hospitalId = scope.hospitalId(actor);
        String name = r.name().trim();
        if (departments.existsByHospitalIdAndNameIgnoreCase(hospitalId, name)) {
            throw new ApiException(HttpStatus.CONFLICT, "DEPARTMENT_EXISTS", "That department already exists.");
        }
        Department d = new Department();
        d.setHospitalId(hospitalId);
        d.setName(name);
        d.setDescription(blankToNull(r.description()));
        d = departments.save(d);
        audit.log(actor.id(), actor.email(), "DEPARTMENT_CREATED", "DEPARTMENT", String.valueOf(d.getId()), null, null);
        return toDto(d);
    }

    public AdminDepartment update(AuthUser actor, Long id, DepartmentRequest r) {
        Department d = own(actor, id);
        String name = r.name().trim();
        if (!d.getName().equalsIgnoreCase(name)
                && departments.existsByHospitalIdAndNameIgnoreCase(d.getHospitalId(), name)) {
            throw new ApiException(HttpStatus.CONFLICT, "DEPARTMENT_EXISTS", "That department already exists.");
        }
        d.setName(name);
        d.setDescription(blankToNull(r.description()));
        audit.log(actor.id(), actor.email(), "DEPARTMENT_UPDATED", "DEPARTMENT", String.valueOf(d.getId()), null, null);
        return toDto(d);
    }

    public AdminDepartment setActive(AuthUser actor, Long id, boolean active) {
        Department d = own(actor, id);
        if (!active && doctors.existsByDepartmentIdAndActiveTrue(id)) {
            throw new ApiException(HttpStatus.CONFLICT, "DEPARTMENT_HAS_DOCTORS",
                    "Move or disable this department's doctors first.");
        }
        d.setActive(active);
        audit.log(actor.id(), actor.email(), active ? "DEPARTMENT_ENABLED" : "DEPARTMENT_DISABLED",
                "DEPARTMENT", String.valueOf(d.getId()), null, null);
        return toDto(d);
    }

    private Department own(AuthUser actor, Long id) {
        Long hospitalId = scope.hospitalId(actor);
        return departments.findById(id).filter(d -> d.getHospitalId().equals(hospitalId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "DEPARTMENT_NOT_FOUND", "Department not found."));
    }

    private AdminDepartment toDto(Department d) {
        return new AdminDepartment(d.getId(), d.getName(), d.getDescription(), d.isActive());
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}