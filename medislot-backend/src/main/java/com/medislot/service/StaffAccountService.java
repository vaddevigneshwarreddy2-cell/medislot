package com.medislot.service;

import com.medislot.dto.AccountDtos.CreateAccountRequest;
import com.medislot.dto.AccountDtos.HospitalRow;
import com.medislot.dto.AccountDtos.StaffMember;
import com.medislot.entity.HospitalStaff;
import com.medislot.entity.Role;
import com.medislot.entity.User;
import com.medislot.exception.ApiException;
import com.medislot.repository.HospitalRepository;
import com.medislot.repository.HospitalStaffRepository;
import com.medislot.repository.StaffMemberQueryRepository;
import com.medislot.repository.UserRepository;
import com.medislot.security.AuthUser;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/** Receptionist and hospital-admin accounts. Callers decide the role and hospital; this class never trusts the client. */
@Service
@Transactional
public class StaffAccountService {

    private final UserRepository users;
    private final HospitalStaffRepository staff;
    private final HospitalRepository hospitals;
    private final StaffMemberQueryRepository query;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    public StaffAccountService(UserRepository users, HospitalStaffRepository staff, HospitalRepository hospitals,
                               StaffMemberQueryRepository query, PasswordEncoder encoder, AuditService audit) {
        this.users = users;
        this.staff = staff;
        this.hospitals = hospitals;
        this.query = query;
        this.encoder = encoder;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<StaffMember> list(Long hospitalId, Role role) {
        return query.find(hospitalId, role.name()).stream()
                .map(r -> new StaffMember(r.id(), r.fullName(), r.email(), r.mobile(), r.staffRole(),
                        Boolean.TRUE.equals(r.active()) && Boolean.TRUE.equals(r.enabled()))).toList();
    }

    @Transactional(readOnly = true)
    public List<HospitalRow> allHospitals() {
        return hospitals.findAll(Sort.by("name")).stream()
                .map(h -> new HospitalRow(h.getId(), h.getName(), h.getCity(), h.getPhone(), h.getEmail(), h.isActive()))
                .toList();
    }

    public StaffMember create(AuthUser actor, Long hospitalId, Role role, CreateAccountRequest r) {
        hospitals.findById(hospitalId).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "HOSPITAL_NOT_FOUND", "Hospital not found."));
        String email = r.email().trim().toLowerCase(Locale.ROOT);
        String mobile = (r.mobile() == null || r.mobile().isBlank()) ? null : r.mobile().trim();
        if (users.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", "An account with this email already exists.");
        }
        if (mobile != null && users.existsByMobile(mobile)) {
            throw new ApiException(HttpStatus.CONFLICT, "MOBILE_ALREADY_REGISTERED", "An account with this mobile number already exists.");
        }
        User u = new User();
        u.setEmail(email);
        u.setMobile(mobile);
        u.setFullName(r.fullName().trim());
        u.setPasswordHash(encoder.encode(r.password()));
        u.setRole(role);
        u.setVerified(true);
        HospitalStaff s = new HospitalStaff();
        try {
            u = users.save(u);
            s.setUserId(u.getId());
            s.setHospitalId(hospitalId);
            s.setStaffRole(role.name());
            s = staff.save(s);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_REGISTERED", "An account with these details already exists.");
        }
        audit.log(actor.id(), actor.email(), "STAFF_CREATED", "STAFF", String.valueOf(s.getId()),
                "role=" + role + ",hospitalId=" + hospitalId, null);
        return new StaffMember(s.getId(), u.getFullName(), u.getEmail(), u.getMobile(), role.name(), true);
    }

    public StaffMember setActive(AuthUser actor, Long hospitalId, Role role, Long staffId, boolean active) {
        HospitalStaff s = staff.findById(staffId)
                .filter(x -> x.getHospitalId().equals(hospitalId) && role.name().equals(x.getStaffRole()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "STAFF_NOT_FOUND", "Staff member not found."));
        if (!active && s.getUserId().equals(actor.id())) {
            throw new ApiException(HttpStatus.CONFLICT, "CANNOT_DISABLE_SELF", "You cannot disable your own account.");
        }
        User u = users.findById(s.getUserId()).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "STAFF_NOT_FOUND", "Staff member not found."));
        s.setActive(active);
        u.setEnabled(active);
        audit.log(actor.id(), actor.email(), active ? "STAFF_ENABLED" : "STAFF_DISABLED", "STAFF",
                String.valueOf(s.getId()), "role=" + role, null);
        return new StaffMember(s.getId(), u.getFullName(), u.getEmail(), u.getMobile(), role.name(), active);
    }
}