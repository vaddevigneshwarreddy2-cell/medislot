package com.medislot.service;

import com.medislot.dto.DoctorDtos.*;
import com.medislot.entity.Department;
import com.medislot.entity.Doctor;
import com.medislot.entity.Role;
import com.medislot.entity.User;
import com.medislot.exception.ApiException;
import com.medislot.repository.*;
import com.medislot.security.AuthUser;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/** Doctor accounts and profiles: admin management plus a doctor editing their own profile. */
@Service
@Transactional
public class DoctorManagementService {

    private final UserRepository users;
    private final DoctorRepository doctors;
    private final DepartmentRepository departments;
    private final DoctorQueryRepository query;
    private final DoctorAccess access;
    private final AdminScope scope;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    public DoctorManagementService(UserRepository users, DoctorRepository doctors, DepartmentRepository departments,
                                   DoctorQueryRepository query, DoctorAccess access, AdminScope scope,
                                   PasswordEncoder encoder, AuditService audit) {
        this.users = users;
        this.doctors = doctors;
        this.departments = departments;
        this.query = query;
        this.access = access;
        this.scope = scope;
        this.encoder = encoder;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<AdminDoctor> list(AuthUser actor) {
        Long hospitalId = scope.hospitalId(actor);
        var filter = new DoctorQueryRepository.Filter(null, hospitalId, null, null, null, null, false);
        return query.search(filter, 500).stream().map(this::toAdmin).toList();
    }

    public AdminDoctor create(AuthUser actor, CreateDoctorRequest r) {
        Long hospitalId = scope.hospitalId(actor);
        department(hospitalId, r.departmentId());

        String email = r.email().trim().toLowerCase(Locale.ROOT);
        String mobile = blankToNull(r.mobile());
        if (users.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED",
                    "An account with this email already exists.");
        }
        if (mobile != null && users.existsByMobile(mobile)) {
            throw new ApiException(HttpStatus.CONFLICT, "MOBILE_ALREADY_REGISTERED",
                    "An account with this mobile number already exists.");
        }

        User user = new User();
        user.setEmail(email);
        user.setMobile(mobile);
        user.setFullName(r.fullName().trim());
        user.setPasswordHash(encoder.encode(r.password()));
        user.setRole(Role.DOCTOR);
        user.setVerified(true); // created by a hospital admin, so no self-verification step
        Doctor d = new Doctor();
        try {
            user = users.save(user);
            d.setUserId(user.getId());
            d.setHospitalId(hospitalId);
            apply(d, r.departmentId(), r.specialization(), r.qualification(), r.experienceYears(),
                    r.consultationDurationMin(), r.bio());
            d = doctors.save(d);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_REGISTERED", "An account with these details already exists.");
        }
        audit.log(actor.id(), actor.email(), "DOCTOR_CREATED", "DOCTOR", String.valueOf(d.getId()), null, null);
        return adminRow(d.getId());
    }

    public AdminDoctor update(AuthUser actor, Long doctorId, UpdateDoctorRequest r) {
        Doctor d = access.forAdmin(actor, doctorId);
        department(d.getHospitalId(), r.departmentId());
        apply(d, r.departmentId(), r.specialization(), r.qualification(), r.experienceYears(),
                r.consultationDurationMin(), r.bio());
        audit.log(actor.id(), actor.email(), "DOCTOR_UPDATED", "DOCTOR", String.valueOf(d.getId()), null, null);
        return adminRow(d.getId());
    }

    public AdminDoctor setActive(AuthUser actor, Long doctorId, boolean active) {
        Doctor d = access.forAdmin(actor, doctorId);
        d.setActive(active);
        audit.log(actor.id(), actor.email(), active ? "DOCTOR_ENABLED" : "DOCTOR_DISABLED",
                "DOCTOR", String.valueOf(d.getId()), null, null);
        return adminRow(d.getId());
    }

    @Transactional(readOnly = true)
    public AdminDoctor ownProfile(AuthUser actor) {
        return adminRow(access.forSelf(actor).getId());
    }

    public AdminDoctor updateOwnProfile(AuthUser actor, SelfProfileRequest r) {
        Doctor d = access.forSelf(actor);
        d.setQualification(blankToNull(r.qualification()));
        d.setExperienceYears(r.experienceYears());
        d.setBio(blankToNull(r.bio()));
        audit.log(actor.id(), actor.email(), "DOCTOR_PROFILE_UPDATED", "DOCTOR", String.valueOf(d.getId()), null, null);
        return adminRow(d.getId());
    }

    // ---------- helpers ----------

    private void apply(Doctor d, Long departmentId, String specialization, String qualification,
                       Integer years, Integer minutes, String bio) {
        d.setDepartmentId(departmentId);
        d.setSpecialization(specialization.trim());
        d.setQualification(blankToNull(qualification));
        d.setExperienceYears(years);
        d.setConsultationDurationMin(minutes == null ? 30 : minutes);
        d.setBio(blankToNull(bio));
    }

    private Department department(Long hospitalId, Long departmentId) {
        return departments.findById(departmentId)
                .filter(dep -> dep.getHospitalId().equals(hospitalId) && dep.isActive())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DEPARTMENT",
                        "Choose an active department of your hospital."));
    }

    private AdminDoctor adminRow(Long doctorId) {
        return query.findById(doctorId, false).map(this::toAdmin).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "DOCTOR_NOT_FOUND", "Doctor not found."));
    }

    private AdminDoctor toAdmin(DoctorRow r) {
        return new AdminDoctor(r.id(), r.fullName(), r.email(), r.specialization(), r.qualification(),
                r.experienceYears(), r.consultationDurationMin(), r.bio(), Boolean.TRUE.equals(r.active()),
                r.hospitalId(), r.departmentId(), r.departmentName());
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}