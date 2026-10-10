package com.medislot.service;

import com.medislot.dto.StaffDtos.CheckInResult;
import com.medislot.entity.Appointment;
import com.medislot.entity.CheckIn;
import com.medislot.exception.ApiException;
import com.medislot.repository.AppointmentRepository;
import com.medislot.repository.CheckInRepository;
import com.medislot.repository.DoctorRepository;
import com.medislot.repository.StaffQueryRepository;
import com.medislot.repository.StaffRow;
import com.medislot.security.AuthUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static com.medislot.entity.AppointmentStatus.*;

/** CONFIRMED -> CHECKED_IN -> WAITING, with a queue number, a check-in record and an audit entry. */
@Service
public class CheckInService {

    private final AppointmentRepository appts;
    private final DoctorRepository doctors;
    private final CheckInRepository checkIns;
    private final StaffQueryRepository staffQuery;
    private final AppointmentStateMachine machine;
    private final QrCodeService qr;
    private final AuditService audit;
    private final ReceptionScope scope;
    private final ZoneId zone;
    private final int earlyMinutes;

    public CheckInService(AppointmentRepository appts, DoctorRepository doctors, CheckInRepository checkIns,
                          StaffQueryRepository staffQuery, AppointmentStateMachine machine, QrCodeService qr,
                          AuditService audit, ReceptionScope scope,
                          @Value("${APP_TIMEZONE:Asia/Kolkata}") String timezone,
                          @Value("${CHECKIN_EARLY_MINUTES:60}") int earlyMinutes) {
        this.appts = appts;
        this.doctors = doctors;
        this.checkIns = checkIns;
        this.staffQuery = staffQuery;
        this.machine = machine;
        this.qr = qr;
        this.audit = audit;
        this.scope = scope;
        this.zone = ZoneId.of(timezone);
        this.earlyMinutes = earlyMinutes;
    }

    @Transactional
    public CheckInResult checkInByCode(AuthUser actor, String code) {
        Long hospitalId = scope.hospitalId(actor);
        QrCodeService.Parsed parsed = qr.parse(code).orElseThrow(CheckInService::invalidQr);
        Appointment pre = appts.findById(parsed.appointmentId()).orElseThrow(CheckInService::invalidQr);
        if (!qr.valid(parsed, pre.getSlotStart())) throw invalidQr();
        if (!pre.getHospitalId().equals(hospitalId)) {
            throw new ApiException(HttpStatus.CONFLICT, "WRONG_HOSPITAL",
                    "This appointment belongs to a different hospital.");
        }
        return doCheckIn(actor, hospitalId, pre, "QR", true);
    }

    /** Staff chose the appointment from the list (for example the patient forgot the QR code). */
    @Transactional
    public CheckInResult checkInById(AuthUser actor, Long appointmentId) {
        Long hospitalId = scope.hospitalId(actor);
        Appointment pre = appts.findById(appointmentId).filter(a -> a.getHospitalId().equals(hospitalId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "APPOINTMENT_NOT_FOUND", "Appointment not found."));
        return doCheckIn(actor, hospitalId, pre, "MANUAL", false);
    }

    @Transactional
    public CheckInResult checkInWalkIn(AuthUser actor, Long appointmentId) {
        Long hospitalId = scope.hospitalId(actor);
        Appointment pre = appts.findById(appointmentId).filter(a -> a.getHospitalId().equals(hospitalId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "APPOINTMENT_NOT_FOUND", "Appointment not found."));
        return doCheckIn(actor, hospitalId, pre, "WALK_IN", false);
    }

    private CheckInResult doCheckIn(AuthUser actor, Long hospitalId, Appointment pre, String method, boolean enforceEarly) {
        // Lock order everywhere: doctor, then appointment.
        doctors.findByIdForUpdate(pre.getDoctorId());
        Appointment a = appts.findByIdForUpdate(pre.getId()).orElseThrow(CheckInService::invalidQr);

        Instant now = Instant.now();
        CheckInPolicy.validate(a.getStatus(), a.getAppointmentDate(), LocalDate.now(zone), a.getSlotStart(),
                now, earlyMinutes, enforceEarly);

        int number = staffQuery.maxQueueNumber(a.getDoctorId(), a.getAppointmentDate()) + 1;
        machine.move(a, CHECKED_IN, actor.id(), "Checked in (" + method + ")");
        machine.move(a, WAITING, actor.id(), "Added to the queue");
        a.setQueueNumber(number);
        a.setCheckedInAt(now);

        CheckIn c = new CheckIn();
        c.setAppointmentId(a.getId());
        c.setHospitalId(hospitalId);
        c.setCheckedInBy(actor.id());
        c.setMethod(method);
        c.setCheckedInAt(now);
        checkIns.save(c);
        audit.log(actor.id(), actor.email(), "CHECK_IN_PERFORMED", "APPOINTMENT", String.valueOf(a.getId()),
                "method=" + method + ",queue=" + number, null);

        List<StaffRow> rows = staffQuery.search(new StaffQueryRepository.Filter(hospitalId, a.getDoctorId(),
                a.getAppointmentDate(), EnumSet.of(CHECKED_IN, WAITING, IN_CONSULTATION), null, null), 500);
        Map<Long, Integer> pos = QueueService.positions(rows);
        StaffRow me = rows.stream().filter(r -> r.id().equals(a.getId())).findFirst().orElseThrow();
        return new CheckInResult(a.getId(), me.patientName(), me.doctorName(),
                QueueService.toDto(me, zone, false, null).appointmentTime(), number, pos.get(a.getId()), "WAITING");
    }

    private static ApiException invalidQr() {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_QR", "This QR code is not valid.");
    }
}