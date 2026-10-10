package com.medislot.service;

import com.medislot.entity.Doctor;
import com.medislot.exception.ApiException;
import com.medislot.repository.DoctorRepository;
import com.medislot.security.AuthUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** The single place that decides which doctor record the caller may touch. */
@Component
public class DoctorAccess {

    private final DoctorRepository doctors;
    private final AdminScope scope;

    public DoctorAccess(DoctorRepository doctors, AdminScope scope) {
        this.doctors = doctors;
        this.scope = scope;
    }

    /** A doctor of the admin's own hospital; anything else looks like "not found". */
    public Doctor forAdmin(AuthUser actor, Long doctorId) {
        Long hospitalId = scope.hospitalId(actor);
        return doctors.findById(doctorId)
                .filter(d -> d.getHospitalId().equals(hospitalId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "DOCTOR_NOT_FOUND", "Doctor not found."));
    }

    /** The caller's own active doctor profile. */
    public Doctor forSelf(AuthUser actor) {
        return doctors.findByUserId(actor.id())
                .filter(Doctor::isActive)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "NO_DOCTOR_PROFILE",
                        "No active doctor profile is linked to this account."));
    }

    /** doctorId == null means "myself" (doctor endpoints); otherwise admin access. */
    public Doctor resolve(AuthUser actor, Long doctorId) {
        return doctorId == null ? forSelf(actor) : forAdmin(actor, doctorId);
    }
}