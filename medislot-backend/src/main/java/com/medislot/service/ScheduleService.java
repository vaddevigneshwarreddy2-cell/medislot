package com.medislot.service;

import com.medislot.dto.DoctorDtos.*;
import com.medislot.entity.Doctor;
import com.medislot.entity.DoctorAvailability;
import com.medislot.entity.DoctorLeave;
import com.medislot.entity.HospitalHoliday;
import com.medislot.exception.ApiException;
import com.medislot.repository.DoctorAvailabilityRepository;
import com.medislot.repository.DoctorLeaveRepository;
import com.medislot.repository.HospitalHolidayRepository;
import com.medislot.security.AuthUser;
import com.medislot.util.SlotGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Working hours, leave and hospital holidays.
 * For the doctor methods, doctorId == null means "the calling doctor" and a value means "an admin
 * acting on a doctor of their own hospital"; DoctorAccess enforces both.
 */
@Service
@Transactional
public class ScheduleService {

    private final DoctorAccess access;
    private final AdminScope scope;
    private final DoctorAvailabilityRepository availability;
    private final DoctorLeaveRepository leave;
    private final HospitalHolidayRepository holidays;
    private final AuditService audit;
    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.context.ApplicationEventPublisher events;

    private final ZoneId zone;

    public ScheduleService(DoctorAccess access, AdminScope scope, DoctorAvailabilityRepository availability,
                           DoctorLeaveRepository leave, HospitalHolidayRepository holidays, AuditService audit,
                           @Value("${APP_TIMEZONE:Asia/Kolkata}") String timezone) {
        this.access = access;
        this.scope = scope;
        this.availability = availability;
        this.leave = leave;
        this.holidays = holidays;
        this.audit = audit;
        this.zone = ZoneId.of(timezone);
    }

    // ---------- weekly working hours ----------

    @Transactional(readOnly = true)
    public List<ScheduleWindow> getSchedule(AuthUser actor, Long doctorId) {
        return windows(access.resolve(actor, doctorId).getId());
    }

    public List<ScheduleWindow> replaceSchedule(AuthUser actor, Long doctorId, ScheduleRequest request) {
        Doctor d = access.resolve(actor, doctorId);
        List<ScheduleRules.Window> parsed = request.windows().stream()
                .map(w -> new ScheduleRules.Window(w.dayOfWeek(), LocalTime.parse(w.start()), LocalTime.parse(w.end())))
                .toList();
        ScheduleRules.validate(parsed, d.getConsultationDurationMin());

        availability.deleteAllForDoctor(d.getId());
        availability.saveAll(parsed.stream().map(w -> {
            DoctorAvailability a = new DoctorAvailability();
            a.setDoctorId(d.getId());
            a.setDayOfWeek(w.dayOfWeek());
            a.setStartTime(w.start());
            a.setEndTime(w.end());
            return a;
        }).toList());
        // Phase 5: appointments outside the new hours will be flagged here.
        audit.log(actor.id(), actor.email(), "AVAILABILITY_CHANGED", "DOCTOR", String.valueOf(d.getId()),
                parsed.size() + " windows", null);
        return windows(d.getId());
    }

    // ---------- leave ----------

    @Transactional(readOnly = true)
    public List<LeaveResponse> listLeave(AuthUser actor, Long doctorId) {
        Doctor d = access.resolve(actor, doctorId);
        return leave.findByDoctorIdAndLeaveDateGreaterThanEqualOrderByLeaveDateAsc(d.getId(), LocalDate.now(zone))
                .stream().map(l -> new LeaveResponse(l.getId(), l.getLeaveDate(), l.getReason())).toList();
    }

    public LeaveResponse addLeave(AuthUser actor, Long doctorId, LeaveRequest r) {
        Doctor d = access.resolve(actor, doctorId);
        if (r.leaveDate().isBefore(LocalDate.now(zone))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "LEAVE_IN_PAST", "Leave cannot be set for a past date.");
        }
        if (leave.existsByDoctorIdAndLeaveDate(d.getId(), r.leaveDate())) {
            throw new ApiException(HttpStatus.CONFLICT, "LEAVE_EXISTS", "Leave is already set for that date.");
        }
        DoctorLeave l = new DoctorLeave();
        l.setDoctorId(d.getId());
        l.setLeaveDate(r.leaveDate());
        l.setReason(blankToNull(r.reason()));
        l.setCreatedBy(actor.id());
        l = leave.save(l);
        events.publishEvent(new com.medislot.event.LeaveCreated(d.getId(), l.getLeaveDate())); // patients are told after the transaction commits
        audit.log(actor.id(), actor.email(), "LEAVE_CREATED", "DOCTOR", String.valueOf(d.getId()),
                "date=" + l.getLeaveDate(), null);
        return new LeaveResponse(l.getId(), l.getLeaveDate(), l.getReason());
    }

    public void deleteLeave(AuthUser actor, Long doctorId, Long leaveId) {
        Doctor d = access.resolve(actor, doctorId);
        DoctorLeave l = leave.findById(leaveId).filter(x -> x.getDoctorId().equals(d.getId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "LEAVE_NOT_FOUND", "Leave entry not found."));
        leave.delete(l);
        audit.log(actor.id(), actor.email(), "LEAVE_DELETED", "DOCTOR", String.valueOf(d.getId()),
                "date=" + l.getLeaveDate(), null);
    }

    // ---------- hospital holidays (admin only) ----------

    @Transactional(readOnly = true)
    public List<HolidayResponse> listHolidays(AuthUser actor) {
        Long hospitalId = scope.hospitalId(actor);
        return holidays.findByHospitalIdAndHolidayDateGreaterThanEqualOrderByHolidayDateAsc(hospitalId, LocalDate.now(zone))
                .stream().map(h -> new HolidayResponse(h.getId(), h.getHolidayDate(), h.getDescription())).toList();
    }

    public HolidayResponse addHoliday(AuthUser actor, HolidayRequest r) {
        Long hospitalId = scope.hospitalId(actor);
        if (r.holidayDate().isBefore(LocalDate.now(zone))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "HOLIDAY_IN_PAST", "A holiday cannot be set for a past date.");
        }
        if (holidays.existsByHospitalIdAndHolidayDate(hospitalId, r.holidayDate())) {
            throw new ApiException(HttpStatus.CONFLICT, "HOLIDAY_EXISTS", "A holiday is already set for that date.");
        }
        HospitalHoliday h = new HospitalHoliday();
        h.setHospitalId(hospitalId);
        h.setHolidayDate(r.holidayDate());
        h.setDescription(r.description().trim());
        h = holidays.save(h);
        audit.log(actor.id(), actor.email(), "HOLIDAY_CREATED", "HOSPITAL", String.valueOf(hospitalId),
                "date=" + h.getHolidayDate(), null);
        return new HolidayResponse(h.getId(), h.getHolidayDate(), h.getDescription());
    }

    public void deleteHoliday(AuthUser actor, Long holidayId) {
        Long hospitalId = scope.hospitalId(actor);
        HospitalHoliday h = holidays.findById(holidayId).filter(x -> x.getHospitalId().equals(hospitalId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "HOLIDAY_NOT_FOUND", "Holiday not found."));
        holidays.delete(h);
        audit.log(actor.id(), actor.email(), "HOLIDAY_DELETED", "HOSPITAL", String.valueOf(hospitalId),
                "date=" + h.getHolidayDate(), null);
    }

    // ---------- helpers ----------

    private List<ScheduleWindow> windows(Long doctorId) {
        return availability.findByDoctorIdOrderByDayOfWeekAscStartTimeAsc(doctorId).stream()
                .map(a -> new ScheduleWindow(a.getDayOfWeek(), SlotGenerator.format(a.getStartTime()),
                        SlotGenerator.format(a.getEndTime()))).toList();
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}