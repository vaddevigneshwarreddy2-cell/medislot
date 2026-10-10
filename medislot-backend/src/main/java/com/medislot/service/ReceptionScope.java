package com.medislot.service;

import com.medislot.exception.ApiException;
import com.medislot.repository.HospitalStaffRepository;
import com.medislot.security.AuthUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Which hospital a RECEPTIONIST works at. */
@Component
public class ReceptionScope {

    private final HospitalStaffRepository staff;

    public ReceptionScope(HospitalStaffRepository staff) {
        this.staff = staff;
    }

    public Long hospitalId(AuthUser actor) {
        return staff.findByUserIdAndActiveTrue(actor.id())
                .filter(s -> "RECEPTIONIST".equals(s.getStaffRole()))
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "NO_HOSPITAL_ASSIGNED",
                        "Your account is not assigned to a hospital."))
                .getHospitalId();
    }
}