package com.medislot.service;

import com.medislot.entity.Patient;
import com.medislot.entity.Role;
import com.medislot.entity.User;
import com.medislot.exception.ApiException;
import com.medislot.repository.PatientRepository;
import com.medislot.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

/** Finds a patient by mobile number or creates a login-less patient record for the front desk. */
@Service
public class WalkInPatientService {

    private final UserRepository users;
    private final PatientRepository patients;
    private final PasswordEncoder encoder;

    public WalkInPatientService(UserRepository users, PatientRepository patients, PasswordEncoder encoder) {
        this.users = users;
        this.patients = patients;
        this.encoder = encoder;
    }

    @Transactional
    public Long findOrCreate(String fullName, String mobile, String email) {
        String m = mobile.trim();
        User existing = users.findByMobile(m).orElse(null);
        if (existing != null) {
            if (existing.getRole() != Role.PATIENT) {
                throw new ApiException(HttpStatus.CONFLICT, "MOBILE_IN_USE", "This mobile number belongs to a staff account.");
            }
            return patients.findByUserId(existing.getId()).orElseGet(() -> newPatient(existing.getId())).getId();
        }

        String mail = (email == null || email.isBlank()) ? null : email.trim().toLowerCase(Locale.ROOT);
        if (mail != null && users.existsByEmail(mail)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED",
                    "An account with this email already exists. Search for the patient by mobile number.");
        }
        User u = new User();
        u.setEmail(mail != null ? mail : "walkin-" + UUID.randomUUID() + "@walkin.medislot.local");
        u.setMobile(m);
        u.setFullName(fullName.trim());
        u.setPasswordHash(encoder.encode(UUID.randomUUID() + "-" + UUID.randomUUID())); // nobody knows it
        u.setRole(Role.PATIENT);
        u.setVerified(false); // cannot log in until the patient claims the account (later phase)
        try {
            u = users.save(u);
            return newPatient(u.getId()).getId();
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_REGISTERED", "A patient with these details already exists.");
        }
    }

    private Patient newPatient(Long userId) {
        Patient p = new Patient();
        p.setUserId(userId);
        return patients.save(p);
    }
}