package com.medislot.service;

import com.medislot.dto.DoctorDtos.DoctorSummary;
import com.medislot.dto.DoctorDtos.Slot;
import com.medislot.dto.StaffDtos.*;
import com.medislot.entity.Appointment;
import com.medislot.entity.AppointmentStatus;
import com.medislot.entity.Patient;
import com.medislot.exception.ApiException;
import com.medislot.repository.*;
import com.medislot.security.AuthUser;
import com.medislot.util.LikeUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import static com.medislot.entity.AppointmentStatus.*;

/** Front-desk operations. Everything is limited to the receptionist's own hospital. */
@Service
public class ReceptionService {

    private final ReceptionScope scope;
    private final StaffQueryRepository staffQuery;
    private final DoctorQueryRepository doctorQuery;
    private final DoctorRepository doctors;
    private final QueueService queue;
    private final CheckInService checkIn;
    private final WalkInPatientService walkInPatients;
    private final AppointmentService appointmentService;
    private final SlotService slots;
    private final AppointmentRepository appts;
    private final PatientRepository patients;
    private final AppointmentStateMachine machine;
    private final AuditService audit;
    private final ZoneId zone;

    public ReceptionService(ReceptionScope scope, StaffQueryRepository staffQuery, DoctorQueryRepository doctorQuery,
                            DoctorRepository doctors, QueueService queue, CheckInService checkIn,
                            WalkInPatientService walkInPatients, AppointmentService appointmentService,
                            SlotService slots, AppointmentRepository appts, PatientRepository patients,
                            AppointmentStateMachine machine, AuditService audit,
                            @Value("${APP_TIMEZONE:Asia/Kolkata}") String timezone) {
        this.scope = scope;
        this.staffQuery = staffQuery;
        this.doctorQuery = doctorQuery;
        this.doctors = doctors;
        this.queue = queue;
        this.checkIn = checkIn;
        this.walkInPatients = walkInPatients;
        this.appointmentService = appointmentService;
        this.slots = slots;
        this.appts = appts;
        this.patients = patients;
        this.machine = machine;
        this.audit = audit;
        this.zone = ZoneId.of(timezone);
    }

    @Transactional(readOnly = true)
    public List<DoctorSummary> doctors(AuthUser actor) {
        Long hospitalId = scope.hospitalId(actor);
        return doctorQuery.search(new DoctorQueryRepository.Filter(null, hospitalId, null, null, null, null, true), 200)
                .stream().map(r -> new DoctorSummary(r.id(), r.fullName(), r.specialization(), r.qualification(),
                        r.experienceYears(), r.consultationDurationMin(), r.hospitalId(), r.hospitalName(),
                        r.hospitalCity(), r.departmentId(), r.departmentName(), null)).toList();
    }

    @Transactional(readOnly = true)
    public List<StaffAppointment> list(AuthUser actor, LocalDate date, Long doctorId, String q, String status) {
        Long hospitalId = scope.hospitalId(actor);
        Collection<AppointmentStatus> statuses = EnumSet.complementOf(EnumSet.of(HELD, EXPIRED));
        if (status != null && !status.isBlank()) statuses = EnumSet.of(parseStatus(status));

        Long appointmentId = null;
        String name = null;
        if (q != null && !q.isBlank()) {
            String t = q.trim();
            if (t.matches("\\d{1,18}")) appointmentId = Long.valueOf(t);
            else name = LikeUtil.contains(t);
        }
        LocalDate day = appointmentId != null ? null : (date == null ? LocalDate.now(zone) : date);
        List<StaffRow> rows = staffQuery.search(
                new StaffQueryRepository.Filter(hospitalId, doctorId, day, statuses, name, appointmentId), 500);
        Map<Long, Integer> pos = QueueService.positions(rows);
        return rows.stream().map(r -> QueueService.toDto(r, zone, false, pos.get(r.id()))).toList();
    }

    public List<StaffAppointment> queue(AuthUser actor, Long doctorId) {
        return queue.receptionQueue(scope.hospitalId(actor), doctorId);
    }

    /**
     * Not one big transaction on purpose: the booking and the check-in each commit on their own, so if the
     * check-in were refused the appointment would still exist and could be checked in from the list.
     */
    public StaffAppointment walkIn(AuthUser actor, WalkInRequest r) {
        Long hospitalId = scope.hospitalId(actor);
        doctors.findById(r.doctorId()).filter(d -> d.getHospitalId().equals(hospitalId) && d.isActive())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "DOCTOR_NOT_FOUND", "Doctor not found."));

        LocalDate today = LocalDate.now(zone);
        String time = r.time();
        if (time == null || time.isBlank()) {
            List<Slot> free = slots.availability(r.doctorId(), today).slots();
            if (free.isEmpty()) {
                throw new ApiException(HttpStatus.CONFLICT, "NO_SLOT_AVAILABLE",
                        "No free slot is left today for this doctor.");
            }
            time = free.get(0).start();
        }
        Long patientId = walkInPatients.findOrCreate(r.fullName(), r.mobile(), r.email());
        Long appointmentId = appointmentService.createWalkIn(actor, hospitalId, patientId, r.doctorId(),
                today, time, r.reason());
        checkIn.checkInWalkIn(actor, appointmentId);
        return queue.entry(hospitalId, appointmentId, false);
    }

    public com.medislot.dto.StaffDtos.CheckInResult checkInByCode(AuthUser actor, String code) {
        return checkIn.checkInByCode(actor, code);
    }

    public com.medislot.dto.StaffDtos.CheckInResult checkInById(AuthUser actor, Long appointmentId) {
        return checkIn.checkInById(actor, appointmentId);
    }

    /** Staff may cancel or mark a no-show. The state machine decides whether the move is valid. */
    @Transactional
    public StaffAppointment changeStatus(AuthUser actor, Long id, StatusChangeRequest r) {
        Long hospitalId = scope.hospitalId(actor);
        AppointmentStatus target = parseStatus(r.status());
        if (target != CANCELLED && target != NO_SHOW) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "STATUS_NOT_ALLOWED",
                    "Staff can only mark an appointment as CANCELLED or NO_SHOW.");
        }
        Appointment pre = appts.findById(id).filter(a -> a.getHospitalId().equals(hospitalId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "APPOINTMENT_NOT_FOUND", "Appointment not found."));
        // Lock order: patient, then appointment (same as patient cancel).
        Patient patient = patients.findByIdForUpdate(pre.getPatientId()).orElseThrow();
        Appointment a = appts.findByIdForUpdate(id).orElseThrow();

        Instant now = Instant.now();
        if (target == NO_SHOW && now.isBefore(a.getSlotStart())) {
            throw new ApiException(HttpStatus.CONFLICT, "TOO_EARLY_FOR_NO_SHOW",
                    "A no-show can only be recorded after the appointment time.");
        }
        String note = (r.note() == null || r.note().isBlank()) ? "Updated by staff" : r.note().trim();
        machine.move(a, target, actor.id(), note);
        if (target == CANCELLED) {
            a.setCancelledAt(now);
            a.setCancelledBy(actor.id());
            a.setCancelReason(note);
        } else {
            patient.setNoShowCount(patient.getNoShowCount() + 1);
        }
        audit.log(actor.id(), actor.email(), target == CANCELLED ? "APPOINTMENT_CANCELLED" : "NO_SHOW_RECORDED",
                "APPOINTMENT", String.valueOf(a.getId()), "by=staff", null);
        return queue.entry(hospitalId, a.getId(), false);
    }

    private AppointmentStatus parseStatus(String s) {
        try {
            return AppointmentStatus.valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_STATUS", "Unknown status: " + s);
        }
    }
}