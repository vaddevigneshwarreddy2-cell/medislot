package com.medislot.service;

import com.medislot.exception.ApiException;
import com.medislot.repository.HospitalStaffRepository;
import com.medislot.security.AuthUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Resolves which hospital a HOSPITAL_ADMIN manages. SUPER_ADMIN has none on purpose. */
@Component
public class AdminScope {

    private final HospitalStaffRepository staff;

    public AdminScope(HospitalStaffRepository staff) {
        this.staff = staff;
    }

    public Long hospitalId(AuthUser actor) {
        return staff.findByUserIdAndActiveTrue(actor.id())
                .filter(s -> "HOSPITAL_ADMIN".equals(s.getStaffRole()))
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "NO_HOSPITAL_ASSIGNED",
                        "Your account is not assigned to a hospital."))
                .getHospitalId();
    }
}