package com.medislot.service;

import com.medislot.config.AppProperties;
import com.medislot.dto.AppointmentDtos.*;
import com.medislot.dto.DoctorDtos.AvailabilityResponse;
import com.medislot.dto.DoctorDtos.Slot;
import com.medislot.entity.Appointment;
import com.medislot.entity.AppointmentStatus;
import com.medislot.entity.Doctor;
import com.medislot.entity.Hospital;
import com.medislot.entity.Patient;
import com.medislot.exception.ApiException;
import com.medislot.repository.*;
import com.medislot.security.AuthUser;
import com.medislot.util.SlotGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static com.medislot.entity.AppointmentStatus.*;

/**
 * Slot holds, confirmation, cancellation and rescheduling.
 * Correctness comes from the database: the partial unique index on (doctor_id, slot_start) decides
 * who wins a slot; row locks serialise status changes and one patient's own concurrent requests.
 */
@Service
public class AppointmentService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);

    private record Resolved(Doctor doctor, LocalDate date, Instant start, Instant end) {}

    private final AppointmentRepository appts;
    private final PatientRepository patients;
    private final DoctorRepository doctors;
    private final DoctorQueryRepository doctorQuery;
    private final HospitalRepository hospitals;
    private final DoctorService doctorService;
    private final AppointmentStateMachine machine;
    private final AuditService audit;
    private final AppProperties props;
    private final TransactionTemplate tx;
    private final ZoneId zone;
    private final int holdMinutes;
    private final int cutoffHours;
    private final int maxReschedules;

    public AppointmentService(AppointmentRepository appts, PatientRepository patients, DoctorRepository doctors,
                              DoctorQueryRepository doctorQuery, HospitalRepository hospitals,
                              DoctorService doctorService, AppointmentStateMachine machine, AuditService audit,
                              AppProperties props, PlatformTransactionManager txManager,
                              @Value("${APP_TIMEZONE:Asia/Kolkata}") String timezone,
                              @Value("${HOLD_MINUTES:5}") int holdMinutes,
                              @Value("${CANCELLATION_CUTOFF_HOURS:2}") int cutoffHours,
                              @Value("${MAX_RESCHEDULES:2}") int maxReschedules) {
        this.appts = appts;
        this.patients = patients;
        this.doctors = doctors;
        this.doctorQuery = doctorQuery;
        this.hospitals = hospitals;
        this.doctorService = doctorService;
        this.machine = machine;
        this.audit = audit;
        this.props = props;
        this.tx = new TransactionTemplate(txManager);
        this.zone = ZoneId.of(timezone);
        this.holdMinutes = holdMinutes;
        this.cutoffHours = cutoffHours;
        this.maxReschedules = maxReschedules;
    }

    // ================= hold =================

    public AppointmentView hold(AuthUser actor, HoldRequest r) {
        Patient patient = patientOf(actor);
        Resolved slot = resolve(r.doctorId(), r.date(), r.time());
        try {
            Long id = tx.execute(st -> doHold(actor, patient.getId(), slot));
            return toView(appts.findById(id).orElseThrow());
        } catch (DataIntegrityViolationException e) { // lost the race: the unique index refused us
            throw slotUnavailable();
        }
    }

    private Long doHold(AuthUser actor, Long patientId, Resolved s) {
        Instant now = Instant.now();
        lockPatient(patientId);

        // A patient holds one slot at a time: release any earlier hold.
        for (Appointment h : appts.findHoldsOfPatient(patientId, HELD)) {
            machine.move(h, EXPIRED, actor.id(), "Replaced by a new hold");
        }
        releaseStaleHolds(s, now);
        appts.flush(); // updates must reach the database BEFORE the insert below

        enforceRules(patientId, s.doctor().getId(), s.date(), s.start(), s.end(), 0L, true);

        Appointment a = newAppointment(patientId, s);
        a.setStatus(HELD);
        a.setHoldExpiresAt(now.plus(holdMinutes, ChronoUnit.MINUTES));
        a = appts.saveAndFlush(a);
        machine.recordInitial(a, actor.id(), "Slot held");
        return a.getId();
    }

    // ================= confirm =================

    public AppointmentView confirm(AuthUser actor, ConfirmRequest r) {
        Patient patient = patientOf(actor);
        Long id = tx.execute(st -> doConfirm(actor, patient.getId(), r));
        return toView(appts.findById(id).orElseThrow());
    }

    private Long doConfirm(AuthUser actor, Long patientId, ConfirmRequest r) {
        Instant now = Instant.now();
        lockPatient(patientId);
        Appointment a = ownedForUpdate(patientId, r.appointmentId());

        if (a.getStatus() == CONFIRMED) return a.getId(); // idempotent
        if (a.getStatus() == EXPIRED) throw holdExpired();
        if (a.getStatus() != HELD) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_STATUS_TRANSITION",
                    "This appointment can no longer be confirmed.");
        }
        if (a.getHoldExpiresAt() == null || a.getHoldExpiresAt().isBefore(now)) throw holdExpired();

        enforceRules(patientId, a.getDoctorId(), a.getAppointmentDate(), a.getSlotStart(), a.getSlotEnd(),
                a.getId(), true);

        machine.move(a, CONFIRMED, actor.id(), "Booked online");
        a.setReason(blankToNull(r.reason()));
        audit.log(actor.id(), actor.email(), "APPOINTMENT_CREATED", "APPOINTMENT",
                String.valueOf(a.getId()), "doctorId=" + a.getDoctorId() + ",date=" + a.getAppointmentDate(), null);
        return a.getId();
    }

    // ================= cancel =================

    public AppointmentView cancel(AuthUser actor, Long id, String reason) {
        Patient patient = patientOf(actor);
        tx.executeWithoutResult(st -> {
            Instant now = Instant.now();
            Appointment a = ownedForUpdate(patient.getId(), id);
            if (a.getStatus() == CANCELLED) return; // idempotent
            if (a.getStatus() != HELD) {
                if (a.getStatus() != CONFIRMED && a.getStatus() != ATTENDANCE_PENDING) {
                    throw new ApiException(HttpStatus.CONFLICT, "CANNOT_CANCEL",
                            "This appointment can no longer be cancelled online.");
                }
                if (!BookingPolicy.withinChangeWindow(a.getSlotStart(), now, cutoffHours)) {
                    throw new ApiException(HttpStatus.CONFLICT, "CANCELLATION_WINDOW_CLOSED",
                            "Appointments can be cancelled up to " + cutoffHours
                                    + " hours before the start. Please contact the hospital.");
                }
            }
            machine.move(a, CANCELLED, actor.id(), "Cancelled by patient");
            a.setCancelledAt(now);
            a.setCancelledBy(actor.id());
            a.setCancelReason(blankToNull(reason));
            audit.log(actor.id(), actor.email(), "APPOINTMENT_CANCELLED", "APPOINTMENT",
                    String.valueOf(a.getId()), null, null);
        });
        return toView(appts.findById(id).orElseThrow());
    }

    // ================= reschedule =================

    public AppointmentView reschedule(AuthUser actor, Long id, RescheduleRequest r) {
        Patient patient = patientOf(actor);
        Appointment pre = appts.findById(id).filter(a -> a.getPatientId().equals(patient.getId()))
                .orElseThrow(AppointmentService::notFound);
        Resolved slot = resolve(pre.getDoctorId(), r.date(), r.time());
        try {
            Long newId = tx.execute(st -> doReschedule(actor, patient.getId(), id, slot));
            return toView(appts.findById(newId).orElseThrow());
        } catch (DataIntegrityViolationException e) {
            throw slotUnavailable(); // the whole transaction rolled back; the original is untouched
        }
    }

    private Long doReschedule(AuthUser actor, Long patientId, Long id, Resolved s) {
        Instant now = Instant.now();
        lockPatient(patientId);
        Appointment old = ownedForUpdate(patientId, id);

        if (old.getStatus() != CONFIRMED && old.getStatus() != ATTENDANCE_PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "CANNOT_RESCHEDULE",
                    "This appointment can no longer be rescheduled.");
        }
        if (!BookingPolicy.withinChangeWindow(old.getSlotStart(), now, cutoffHours)) {
            throw new ApiException(HttpStatus.CONFLICT, "RESCHEDULE_WINDOW_CLOSED",
                    "Appointments can be rescheduled up to " + cutoffHours + " hours before the start.");
        }
        if (old.getRescheduleCount() >= maxReschedules) {
            throw new ApiException(HttpStatus.CONFLICT, "MAX_RESCHEDULES_REACHED",
                    "This appointment has already been rescheduled " + maxReschedules + " times.");
        }
        if (old.getSlotStart().equals(s.start())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SAME_SLOT", "Choose a different time slot.");
        }

        releaseStaleHolds(s, now);
        appts.flush();
        enforceRules(patientId, s.doctor().getId(), s.date(), s.start(), s.end(), old.getId(), false);

        Appointment n = newAppointment(patientId, s);
        n.setStatus(CONFIRMED);
        n.setReason(old.getReason());
        n.setSource(old.getSource());
        n.setRescheduledFromId(old.getId());
        n.setRescheduleCount(old.getRescheduleCount() + 1);
        n = appts.saveAndFlush(n);
        machine.recordInitial(n, actor.id(), "Rescheduled from #" + old.getId());
        machine.move(old, RESCHEDULED, actor.id(), "Rescheduled to #" + n.getId());

        audit.log(actor.id(), actor.email(), "APPOINTMENT_RESCHEDULED", "APPOINTMENT",
                String.valueOf(n.getId()), "from=" + old.getId(), null);
        return n.getId();
    }

    // ================= reads =================

    @Transactional(readOnly = true)
    public AppointmentView get(AuthUser actor, Long id) {
        Patient patient = patientOf(actor);
        return toView(appts.findById(id).filter(a -> a.getPatientId().equals(patient.getId()))
                .orElseThrow(AppointmentService::notFound));
    }

    @Transactional(readOnly = true)
    public List<AppointmentView> mine(AuthUser actor, String scope) {
        Patient patient = patientOf(actor);
        Instant now = Instant.now();
        List<Appointment> list;
        if ("upcoming".equals(scope)) {
            list = appts.findUpcoming(patient.getId(), ACTIVE_BOOKED, now, PageRequest.of(0, 100));
        } else if ("history".equals(scope)) {
            list = appts.findHistory(patient.getId(), EnumSet.of(HELD, EXPIRED), ACTIVE_BOOKED, now,
                    PageRequest.of(0, 100));
        } else {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_SCOPE", "scope must be upcoming or history.");
        }
        Map<Long, DoctorRow> docs = new HashMap<>();
        Map<Long, Hospital> hosp = new HashMap<>();
        return list.stream().map(a -> toView(a, docs, hosp)).toList();
    }

    // ================= hold expiry job =================

    @Transactional
    public int expireStaleHolds() {
        List<Appointment> stale = appts.findExpiredHolds(HELD, Instant.now(), PageRequest.of(0, 200));
        for (Appointment a : stale) {
            machine.move(a, EXPIRED, null, "Hold expired");
        }
        if (!stale.isEmpty()) log.info("Expired {} stale slot holds", stale.size());
        return stale.size();
    }

    // ================= helpers =================

    private Patient patientOf(AuthUser actor) {
        return patients.findByUserId(actor.id()).orElseThrow(() ->
                new ApiException(HttpStatus.FORBIDDEN, "NO_PATIENT_PROFILE", "No patient profile is linked to this account."));
    }

    private void lockPatient(Long patientId) {
        patients.findByIdForUpdate(patientId).orElseThrow(AppointmentService::notFound);
    }

    private Appointment ownedForUpdate(Long patientId, Long appointmentId) {
        return appts.findByIdForUpdate(appointmentId).filter(a -> a.getPatientId().equals(patientId))
                .orElseThrow(AppointmentService::notFound);
    }

    /** The requested time must be one of the slots the schedule really generates for that date. */
    private Resolved resolve(Long doctorId, LocalDate date, String time) {
        AvailabilityResponse av = doctorService.availability(doctorId, date); // 404 if doctor unknown/inactive
        Slot match = av.slots().stream().filter(sl -> sl.start().equals(time)).findFirst()
                .orElseThrow(AppointmentService::slotUnavailable);
        Doctor d = doctors.findById(doctorId).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "DOCTOR_NOT_FOUND", "Doctor not found."));
        Instant start = ZonedDateTime.of(date, LocalTime.parse(match.start()), zone).toInstant();
        Instant end = ZonedDateTime.of(date, LocalTime.parse(match.end()), zone).toInstant();
        return new Resolved(d, date, start, end);
    }

    private void releaseStaleHolds(Resolved s, Instant now) {
        for (Appointment stale : appts.findStaleHoldsForSlot(s.doctor().getId(), s.start(), HELD, now)) {
            machine.move(stale, EXPIRED, null, "Hold expired");
        }
    }

    private void enforceRules(Long patientId, Long doctorId, LocalDate date, Instant start, Instant end,
                              Long excludeId, boolean checkMax) {
        int max = props.booking().maxActiveAppointments();
        if (checkMax && appts.countActive(patientId, ACTIVE_BOOKED, Instant.now()) >= max) {
            throw new ApiException(HttpStatus.CONFLICT, "MAX_ACTIVE_APPOINTMENTS_REACHED",
                    "You already have " + max + " active appointments. Cancel one before booking another.");
        }
        if (appts.countSameDoctorDay(patientId, excludeId, ACTIVE_BOOKED, doctorId, date) > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "DUPLICATE_BOOKING",
                    "You already have an appointment with this doctor on that date.");
        }
        if (appts.countOverlapping(patientId, excludeId, ACTIVE_BOOKED, start, end) > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "OVERLAPPING_APPOINTMENT",
                    "You already have an appointment at that time.");
        }
    }

    private Appointment newAppointment(Long patientId, Resolved s) {
        Appointment a = new Appointment();
        a.setPatientId(patientId);
        a.setDoctorId(s.doctor().getId());
        a.setHospitalId(s.doctor().getHospitalId());
        a.setDepartmentId(s.doctor().getDepartmentId());
        a.setAppointmentDate(s.date());
        a.setSlotStart(s.start());
        a.setSlotEnd(s.end());
        return a;
    }

    private AppointmentView toView(Appointment a) {
        return toView(a, new HashMap<>(), new HashMap<>());
    }

    private AppointmentView toView(Appointment a, Map<Long, DoctorRow> docs, Map<Long, Hospital> hosp) {
        DoctorRow d = docs.computeIfAbsent(a.getDoctorId(),
                id -> doctorQuery.findById(id, false).orElse(null));
        Hospital h = hosp.computeIfAbsent(a.getHospitalId(), id -> hospitals.findById(id).orElse(null));
        HospitalInfo info = h == null ? null : new HospitalInfo(h.getId(), h.getName(),
                h.getAddressLine() + ", " + h.getCity(), h.getLatitude(), h.getLongitude());

        boolean changeable = (a.getStatus() == CONFIRMED || a.getStatus() == ATTENDANCE_PENDING)
                && BookingPolicy.withinChangeWindow(a.getSlotStart(), Instant.now(), cutoffHours);

        return new AppointmentView(a.getId(), a.getStatus().name(), a.getAppointmentDate(),
                SlotGenerator.format(a.getSlotStart().atZone(zone).toLocalTime()),
                SlotGenerator.format(a.getSlotEnd().atZone(zone).toLocalTime()),
                a.getStatus() == HELD ? a.getHoldExpiresAt() : null,
                d == null ? null : d.fullName(), d == null ? null : d.specialization(),
                d == null ? null : d.departmentName(), info, a.getRescheduleCount(),
                a.getRescheduledFromId(), a.getReason(), changeable,
                changeable && a.getRescheduleCount() < maxReschedules);
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "APPOINTMENT_NOT_FOUND", "Appointment not found.");
    }

    private static ApiException slotUnavailable() {
        return new ApiException(HttpStatus.CONFLICT, "SLOT_UNAVAILABLE",
                "The selected appointment slot is no longer available.");
    }

    private static ApiException holdExpired() {
        return new ApiException(HttpStatus.GONE, "HOLD_EXPIRED",
                "Your hold on this slot has expired. Please choose a slot again.");
    }
}