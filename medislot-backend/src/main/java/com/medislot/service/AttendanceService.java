package com.medislot.service;

import com.medislot.dto.NotificationDtos.AttendanceResult;
import com.medislot.entity.Appointment;
import com.medislot.entity.Patient;
import com.medislot.exception.ApiException;
import com.medislot.repository.AppointmentRepository;
import com.medislot.repository.PatientRepository;
import com.medislot.security.AuthUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static com.medislot.entity.AppointmentStatus.ATTENDANCE_PENDING;
import static com.medislot.entity.AppointmentStatus.CONFIRMED;

/** The patient confirms they will attend. Lock order matches the other patient flows: patient, then appointment. */
@Service
public class AttendanceService {

    private final PatientRepository patients;
    private final AppointmentRepository appts;
    private final AppointmentStateMachine machine;
    private final AuditService audit;

    public AttendanceService(PatientRepository patients, AppointmentRepository appts,
                             AppointmentStateMachine machine, AuditService audit) {
        this.patients = patients;
        this.appts = appts;
        this.machine = machine;
        this.audit = audit;
    }

    @Transactional
    public AttendanceResult confirm(AuthUser actor, Long appointmentId) {
        Patient p = patients.findByUserId(actor.id()).orElseThrow(() ->
                new ApiException(HttpStatus.FORBIDDEN, "NO_PATIENT_PROFILE", "No patient profile is linked to this account."));
        patients.findByIdForUpdate(p.getId());
        Appointment a = appts.findByIdForUpdate(appointmentId).filter(x -> x.getPatientId().equals(p.getId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "APPOINTMENT_NOT_FOUND", "Appointment not found."));

        Instant now = Instant.now();
        if (a.getSlotEnd().isBefore(now)) {
            throw new ApiException(HttpStatus.CONFLICT, "APPOINTMENT_PASSED", "This appointment time has passed.");
        }
        if (a.getStatus() == ATTENDANCE_PENDING) {
            machine.move(a, CONFIRMED, actor.id(), "Attendance confirmed by patient");
        } else if (a.getStatus() != CONFIRMED) {
            throw new ApiException(HttpStatus.CONFLICT, "CANNOT_CONFIRM_ATTENDANCE",
                    "Attendance can only be confirmed for an upcoming appointment.");
        }
        if (a.getAttendanceConfirmedAt() == null) {
            a.setAttendanceConfirmedAt(now);
            audit.log(actor.id(), actor.email(), "ATTENDANCE_CONFIRMED", "APPOINTMENT",
                    String.valueOf(a.getId()), null, null);
        }
        return new AttendanceResult(a.getId(), a.getStatus().name(), a.getAttendanceConfirmedAt());
    }
}