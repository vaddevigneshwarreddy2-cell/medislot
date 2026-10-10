package com.medislot.service;

import com.medislot.dto.StaffDtos.*;
import com.medislot.entity.Appointment;
import com.medislot.entity.Doctor;
import com.medislot.entity.Patient;
import com.medislot.exception.ApiException;
import com.medislot.repository.*;
import com.medislot.security.AuthUser;
import com.medislot.util.SlotGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static com.medislot.entity.AppointmentStatus.*;

/** Waiting queue: ordering, doctor actions (call / start / complete) and patient-facing status. */
@Service
public class QueueService {

    private final StaffQueryRepository staffQuery;
    private final AppointmentRepository appts;
    private final DoctorRepository doctors;
    private final PatientRepository patients;
    private final AppointmentStateMachine machine;
    private final AuditService audit;
    private final DoctorAccess access;
    private final ZoneId zone;

    public QueueService(StaffQueryRepository staffQuery, AppointmentRepository appts, DoctorRepository doctors,
                        PatientRepository patients, AppointmentStateMachine machine, AuditService audit,
                        DoctorAccess access, @Value("${APP_TIMEZONE:Asia/Kolkata}") String timezone) {
        this.staffQuery = staffQuery;
        this.appts = appts;
        this.doctors = doctors;
        this.patients = patients;
        this.machine = machine;
        this.audit = audit;
        this.access = access;
        this.zone = ZoneId.of(timezone);
    }

    // ---------- pure helpers (unit tested) ----------

    /** Waiting patients of ONE doctor: earlier appointment time first, then earlier check-in. */
    public static List<StaffRow> waitingOrder(List<StaffRow> rows) {
        return rows.stream()
                .filter(r -> r.status() == WAITING || r.status() == CHECKED_IN)
                .sorted(Comparator.comparing(StaffRow::slotStart)
                        .thenComparing(StaffRow::queueNumber, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    /** appointmentId -> 1-based position in that doctor's waiting list. */
    public static Map<Long, Integer> positions(List<StaffRow> rows) {
        Map<Long, List<StaffRow>> byDoctor = rows.stream()
                .collect(Collectors.groupingBy(StaffRow::doctorId));
        Map<Long, Integer> out = new HashMap<>();
        byDoctor.values().forEach(list -> {
            List<StaffRow> ordered = waitingOrder(list);
            for (int i = 0; i < ordered.size(); i++) out.put(ordered.get(i).id(), i + 1);
        });
        return out;
    }

    public static StaffAppointment toDto(StaffRow r, ZoneId zone, boolean doctorView, Integer position) {
        return new StaffAppointment(r.id(), r.status().name(), r.date(),
                SlotGenerator.format(r.slotStart().atZone(zone).toLocalTime()),
                SlotGenerator.format(r.slotEnd().atZone(zone).toLocalTime()),
                r.queueNumber(), position, r.calledAt() != null, r.checkedInAt(), r.source(),
                r.patientName(), doctorView ? null : r.patientMobile(), r.doctorId(), r.doctorName(),
                r.specialization(), doctorView ? r.reason() : null);
    }

    // ---------- doctor ----------

    @Transactional(readOnly = true)
    public DoctorQueueResponse doctorQueue(AuthUser actor) {
        Doctor d = access.forSelf(actor);
        LocalDate today = LocalDate.now(zone);
        List<StaffRow> rows = staffQuery.search(new StaffQueryRepository.Filter(d.getHospitalId(), d.getId(), today,
                EnumSet.of(CONFIRMED, ATTENDANCE_PENDING, CHECKED_IN, WAITING, IN_CONSULTATION, COMPLETED),
                null, null), 500);
        Map<Long, Integer> pos = positions(rows);
        int completed = (int) rows.stream().filter(r -> r.status() == COMPLETED).count();
        List<StaffAppointment> entries = rows.stream().filter(r -> r.status() != COMPLETED)
                .map(r -> toDto(r, zone, true, pos.get(r.id()))).toList();
        return new DoctorQueueResponse(today, completed, entries);
    }

    @Transactional
    public StaffAppointment callNext(AuthUser actor) {
        Doctor d = access.forSelf(actor);
        doctors.findByIdForUpdate(d.getId());
        List<StaffRow> waiting = staffQuery.search(new StaffQueryRepository.Filter(d.getHospitalId(), d.getId(),
                LocalDate.now(zone), EnumSet.of(WAITING), null, null), 500);
        StaffRow next = waitingOrder(waiting).stream().filter(r -> r.calledAt() == null).findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "NO_WAITING_PATIENTS",
                        "No patients are waiting."));
        return markCalled(actor, d, next.id());
    }

    @Transactional
    public StaffAppointment call(AuthUser actor, Long appointmentId) {
        Doctor d = access.forSelf(actor);
        doctors.findByIdForUpdate(d.getId());
        return markCalled(actor, d, appointmentId);
    }

    @Transactional
    public StaffAppointment start(AuthUser actor, Long appointmentId) {
        Doctor d = access.forSelf(actor);
        doctors.findByIdForUpdate(d.getId());
        Appointment a = ownedByDoctor(d, appointmentId);
        if (a.getStatus() != WAITING) {
            throw new ApiException(HttpStatus.CONFLICT, "NOT_WAITING", "This patient is not in the waiting list.");
        }
        boolean busy = !staffQuery.search(new StaffQueryRepository.Filter(d.getHospitalId(), d.getId(),
                LocalDate.now(zone), EnumSet.of(IN_CONSULTATION), null, null), 1).isEmpty();
        if (busy) {
            throw new ApiException(HttpStatus.CONFLICT, "CONSULTATION_IN_PROGRESS",
                    "Finish the current consultation first.");
        }
        Instant now = Instant.now();
        machine.move(a, IN_CONSULTATION, actor.id(), "Consultation started");
        a.setConsultationStartedAt(now);
        if (a.getCalledAt() == null) a.setCalledAt(now);
        audit.log(actor.id(), actor.email(), "CONSULTATION_STARTED", "APPOINTMENT",
                String.valueOf(a.getId()), null, null);
        return entry(d.getHospitalId(), a.getId(), true);
    }

    /** Simple completion for now. Phase 7 replaces this with notes, prescription and follow-up. */
    @Transactional
    public StaffAppointment complete(AuthUser actor, Long appointmentId) {
        Doctor d = access.forSelf(actor);
        doctors.findByIdForUpdate(d.getId());
        Appointment a = ownedByDoctor(d, appointmentId);
        machine.move(a, COMPLETED, actor.id(), "Consultation completed");
        a.setCompletedAt(Instant.now());
        audit.log(actor.id(), actor.email(), "CONSULTATION_COMPLETED", "APPOINTMENT",
                String.valueOf(a.getId()), null, null);
        return entry(d.getHospitalId(), a.getId(), true);
    }

    // ---------- reception ----------

    @Transactional(readOnly = true)
    public List<StaffAppointment> receptionQueue(Long hospitalId, Long doctorId) {
        List<StaffRow> rows = staffQuery.search(new StaffQueryRepository.Filter(hospitalId, doctorId,
                LocalDate.now(zone), EnumSet.of(CHECKED_IN, WAITING, IN_CONSULTATION), null, null), 500);
        Map<Long, Integer> pos = positions(rows);
        return rows.stream().map(r -> toDto(r, zone, false, pos.get(r.id()))).toList();
    }

    @Transactional(readOnly = true)
    public StaffAppointment entry(Long hospitalId, Long appointmentId, boolean doctorView) {
        return staffQuery.search(new StaffQueryRepository.Filter(hospitalId, null, null, null, null, appointmentId), 1)
                .stream().findFirst().map(r -> toDto(r, zone, doctorView, null))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "APPOINTMENT_NOT_FOUND",
                        "Appointment not found."));
    }

    // ---------- patient ----------

    @Transactional(readOnly = true)
    public PatientQueueStatus patientStatus(AuthUser actor, Long appointmentId) {
        Patient p = patients.findByUserId(actor.id()).orElseThrow(() ->
                new ApiException(HttpStatus.FORBIDDEN, "NO_PATIENT_PROFILE", "No patient profile is linked to this account."));
        Appointment a = appts.findById(appointmentId).filter(x -> x.getPatientId().equals(p.getId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "APPOINTMENT_NOT_FOUND", "Appointment not found."));

        if (a.getStatus() != CHECKED_IN && a.getStatus() != WAITING && a.getStatus() != IN_CONSULTATION) {
            return new PatientQueueStatus(a.getId(), a.getStatus().name(), null, null, false, false, null);
        }
        Doctor doc = doctors.findById(a.getDoctorId()).orElseThrow();
        List<StaffRow> rows = staffQuery.search(new StaffQueryRepository.Filter(null, a.getDoctorId(),
                a.getAppointmentDate(), EnumSet.of(CHECKED_IN, WAITING, IN_CONSULTATION), null, null), 500);
        boolean busy = rows.stream().anyMatch(r -> r.status() == IN_CONSULTATION);
        List<StaffRow> order = waitingOrder(rows);
        int index = -1;
        for (int i = 0; i < order.size(); i++) if (order.get(i).id().equals(a.getId())) index = i;

        Integer ahead = null;
        Integer estimate = null;
        if (a.getStatus() != IN_CONSULTATION && index >= 0) {
            ahead = index;
            estimate = (index + (busy ? 1 : 0)) * doc.getConsultationDurationMin(); // a rough estimate only
        }
        return new PatientQueueStatus(a.getId(), a.getStatus().name(), a.getQueueNumber(), ahead,
                a.getCalledAt() != null, busy, estimate);
    }

    // ---------- helpers ----------

    private StaffAppointment markCalled(AuthUser actor, Doctor d, Long appointmentId) {
        Appointment a = ownedByDoctor(d, appointmentId);
        if (a.getStatus() != WAITING) {
            throw new ApiException(HttpStatus.CONFLICT, "NOT_WAITING", "This patient is not in the waiting list.");
        }
        a.setCalledAt(Instant.now());
        audit.log(actor.id(), actor.email(), "PATIENT_CALLED", "APPOINTMENT", String.valueOf(a.getId()), null, null);
        return entry(d.getHospitalId(), a.getId(), true);
    }

    private Appointment ownedByDoctor(Doctor d, Long appointmentId) {
        return appts.findByIdForUpdate(appointmentId).filter(a -> a.getDoctorId().equals(d.getId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "APPOINTMENT_NOT_FOUND", "Appointment not found."));
    }
}