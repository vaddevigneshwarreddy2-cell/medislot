package com.medislot.service.impl;

import com.medislot.dto.HospitalDtos.*;
import com.medislot.entity.Hospital;
import com.medislot.exception.ApiException;
import com.medislot.mapper.HospitalMapper;
import com.medislot.repository.HospitalRepository;
import com.medislot.repository.HospitalStaffRepository;
import com.medislot.security.AuthUser;
import com.medislot.service.AuditService;
import com.medislot.service.HospitalManagementService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.util.Locale;

@Service
@Transactional
public class HospitalManagementServiceImpl implements HospitalManagementService {

    private final HospitalRepository hospitals;
    private final HospitalStaffRepository staff;
    private final HospitalMapper mapper;
    private final AuditService audit;

    public HospitalManagementServiceImpl(HospitalRepository hospitals, HospitalStaffRepository staff,
                                         HospitalMapper mapper, AuditService audit) {
        this.hospitals = hospitals;
        this.staff = staff;
        this.mapper = mapper;
        this.audit = audit;
    }

    @Override
    @Transactional(readOnly = true)
    public Detail getOwn(AuthUser actor) {
        return mapper.toDetail(find(ownHospitalId(actor)), null);
    }

    @Override
    public Detail updateOwnProfile(AuthUser actor, ProfileRequest request) {
        return updateProfile(actor, ownHospitalId(actor), request);
    }

    @Override
    public Detail updateOwnLocation(AuthUser actor, LocationRequest request) {
        return updateLocation(actor, ownHospitalId(actor), request);
    }

    @Override
    public Detail create(AuthUser actor, CreateRequest request) {
        Hospital h = new Hospital();
        applyProfile(h, request.profile());
        applyLocation(h, request.location());
        h = hospitals.save(h);
        audit.log(actor.id(), actor.email(), "HOSPITAL_CREATED", "HOSPITAL", String.valueOf(h.getId()), null, null);
        return mapper.toDetail(h, null);
    }

    @Override
    public Detail updateProfile(AuthUser actor, Long hospitalId, ProfileRequest request) {
        Hospital h = find(hospitalId);
        applyProfile(h, request);
        audit.log(actor.id(), actor.email(), "HOSPITAL_UPDATED", "HOSPITAL", String.valueOf(h.getId()), null, null);
        return mapper.toDetail(h, null);
    }

    @Override
    public Detail updateLocation(AuthUser actor, Long hospitalId, LocationRequest request) {
        Hospital h = find(hospitalId);
        applyLocation(h, request);
        audit.log(actor.id(), actor.email(), "HOSPITAL_LOCATION_UPDATED", "HOSPITAL",
                String.valueOf(h.getId()), "lat=" + h.getLatitude() + ",lng=" + h.getLongitude(), null);
        return mapper.toDetail(h, null);
    }

    @Override
    public Detail setActive(AuthUser actor, Long hospitalId, boolean active) {
        Hospital h = find(hospitalId);
        h.setActive(active);
        audit.log(actor.id(), actor.email(), active ? "HOSPITAL_ENABLED" : "HOSPITAL_DISABLED",
                "HOSPITAL", String.valueOf(h.getId()), null, null);
        return mapper.toDetail(h, null);
    }

    // ---------- helpers ----------

    private Long ownHospitalId(AuthUser actor) {
        return staff.findByUserIdAndActiveTrue(actor.id())
                .filter(s -> "HOSPITAL_ADMIN".equals(s.getStaffRole()))
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "NO_HOSPITAL_ASSIGNED",
                        "Your account is not assigned to a hospital."))
                .getHospitalId();
    }

    private Hospital find(Long id) {
        return hospitals.findById(id).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "HOSPITAL_NOT_FOUND", "Hospital not found."));
    }

    private void applyProfile(Hospital h, ProfileRequest r) {
        h.setName(r.name().trim());
        h.setPhone(blankToNull(r.phone()));
        String email = blankToNull(r.email());
        h.setEmail(email == null ? null : email.toLowerCase(Locale.ROOT));
    }

    private void applyLocation(Hospital h, LocationRequest r) {
        h.setAddressLine(r.addressLine().trim());
        h.setCity(r.city().trim());
        h.setState(r.state().trim());
        h.setPostalCode(blankToNull(r.postalCode()));
        if (r.country() != null && !r.country().isBlank()) {
            h.setCountry(r.country().trim());
        }
        h.setLatitude(r.latitude().setScale(7, RoundingMode.HALF_UP));
        h.setLongitude(r.longitude().setScale(7, RoundingMode.HALF_UP));
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}