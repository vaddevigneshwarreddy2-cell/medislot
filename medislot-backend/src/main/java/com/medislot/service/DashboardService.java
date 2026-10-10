package com.medislot.service;

import com.medislot.dto.DashboardDtos.*;
import com.medislot.dto.StaffDtos.StaffAppointment;
import com.medislot.entity.Appointment;
import com.medislot.entity.AppointmentStatus;
import com.medislot.entity.Doctor;
import com.medislot.entity.Patient;
import com.medislot.exception.ApiException;
import com.medislot.repository.*;
import com.medislot.security.AuthUser;
import com.medislot.util.SlotGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static com.medislot.entity.AppointmentStatus.*;

/** Counts for each role's home screen. Charts and waiting-time reports come with Phase 9. */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final PatientRepository patients;
    private final AppointmentRepository appts;
    private final AppointmentInsightRepository insight;
    private final NotificationRepository notifications;
    private final ClinicalQueryRepository clinical;
    private final DoctorQueryRepository doctorQuery;
    private final HospitalRepository hospitals;
    private final DoctorAccess access;
    private final AdminScope adminScope;
    private final ReceptionScope receptionScope;
    private final StaffQueryRepository staffQuery;
    private final DashboardQueryRepository dashQuery;
    private final ZoneId zone;
    private final int graceMinutes;

    public DashboardService(PatientRepository patients, AppointmentRepository appts,
                            AppointmentInsightRepository insight, NotificationRepository notifications,
                            ClinicalQueryRepository clinical, DoctorQueryRepository doctorQuery,
                            HospitalRepository hospitals, DoctorAccess access, AdminScope adminScope,
                            ReceptionScope receptionScope, StaffQueryRepository staffQuery,
                            DashboardQueryRepository dashQuery,
                            @Value("${APP_TIMEZONE:Asia/Kolkata}") String timezone,
                            @Value("${ATTENTION_GRACE_MINUTES:30}") int graceMinutes) {
        this.patients = patients;
        this.appts = appts;
        this.insight = insight;
        this.notifications = notifications;
        this.clinical = clinical;
        this.doctorQuery = doctorQuery;
        this.hospitals = hospitals;
        this.access = access;
        this.adminScope = adminScope;
        this.receptionScope = receptionScope;
        this.staffQuery = staffQuery;
        this.dashQuery = dashQuery;
        this.zone = ZoneId.of(timezone);
        this.graceMinutes = graceMinutes;
    }

    // ---------- patient ----------

    public PatientDashboard patient(AuthUser actor) {
        Patient p = patients.findByUserId(actor.id()).orElseThrow(() ->
                new ApiException(HttpStatus.FORBIDDEN, "NO_PATIENT_PROFILE", "No patient profile is linked to this account."));
        Instant now = Instant.now();
        NextAppointment next = appts.findUpcoming(p.getId(), ACTIVE_BOOKED, now, PageRequest.of(0, 1)).stream()
                .findFirst().map(this::next).orElse(null);
        return new PatientDashboard(
                appts.countActive(p.getId(), ACTIVE_BOOKED, now),
                insight.countByPatientIdAndStatus(p.getId(), COMPLETED),
                insight.countByPatientIdAndStatus(p.getId(), CANCELLED),
                insight.countByPatientIdAndStatus(p.getId(), NO_SHOW),
                clinical.followUps(p.getId(), LocalDate.now(zone)).size(),
                notifications.countByUserIdAndReadAtIsNull(actor.id()), next);
    }

    private NextAppointment next(Appointment a) {
        String doctor = doctorQuery.findById(a.getDoctorId(), false).map(DoctorRow::fullName).orElse("");
        String hospital = hospitals.findById(a.getHospitalId()).map(h -> h.getName()).orElse("");
        return new NextAppointment(a.getId(), doctor, hospital, a.getAppointmentDate(),
                SlotGenerator.format(a.getSlotStart().atZone(zone).toLocalTime()), a.getStatus().name());
    }

    // ---------- doctor ----------

    public DoctorDashboard doctor(AuthUser actor) {
        Doctor d = access.forSelf(actor);
        LocalDate today = LocalDate.now(zone);
        List<StaffRow> rows = staffQuery.search(new StaffQueryRepository.Filter(d.getHospitalId(), d.getId(), today,
                EnumSet.complementOf(EnumSet.of(HELD, EXPIRED, RESCHEDULED)), null, null), 500);
        return new DoctorDashboard(today, rows.size(),
                count(rows, CONFIRMED, ATTENDANCE_PENDING), count(rows, CHECKED_IN, WAITING),
                count(rows, IN_CONSULTATION), count(rows, COMPLETED), count(rows, NO_SHOW),
                insight.countFuture(d.getId(), today, ACTIVE_BOOKED));
    }

    private static long count(List<StaffRow> rows, AppointmentStatus... statuses) {
        Set<AppointmentStatus> set = EnumSet.copyOf(List.of(statuses));
        return rows.stream().filter(r -> set.contains(r.status())).count();
    }

    // ---------- reception: appointments that passed without a check-in ----------

    public List<StaffAppointment> attention(AuthUser actor) {
        Long hospitalId = receptionScope.hospitalId(actor);
        LocalDate today = LocalDate.now(zone);
        Instant cutoff = Instant.now().minus(graceMinutes, ChronoUnit.MINUTES);
        List<StaffRow> rows = new ArrayList<>();
        for (LocalDate day : List.of(today.minusDays(1), today)) {
            rows.addAll(staffQuery.search(new StaffQueryRepository.Filter(hospitalId, null, day,
                    EnumSet.of(CONFIRMED, ATTENDANCE_PENDING), null, null), 500));
        }
        return rows.stream().filter(r -> r.slotEnd().isBefore(cutoff))
                .map(r -> QueueService.toDto(r, zone, false, null)).toList();
    }

    // ---------- hospital admin ----------

    public AdminDashboard admin(AuthUser actor, String period) {
        Long hospitalId = adminScope.hospitalId(actor);
        LocalDate to = LocalDate.now(zone);
        String p = period == null ? "today" : period;
        LocalDate from = switch (p) {
            case "today" -> to;
            case "week" -> to.minusDays(6);
            case "month" -> to.minusDays(29);
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PERIOD",
                    "period must be today, week or month.");
        };

        List<CountStat> departments = aggregate(dashQuery.byDepartment(hospitalId, from, to));
        List<CountStat> doctors = aggregate(dashQuery.byDoctor(hospitalId, from, to));
        long total = departments.stream().mapToLong(CountStat::booked).sum();
        long completed = departments.stream().mapToLong(CountStat::completed).sum();
        long cancelled = departments.stream().mapToLong(CountStat::cancelled).sum();
        long noShow = departments.stream().mapToLong(CountStat::noShow).sum();
        double rate = total == 0 ? 0.0 : Math.round(cancelled * 1000.0 / total) / 10.0;
        int activeDoctors = doctorQuery.search(new DoctorQueryRepository.Filter(null, hospitalId, null, null,
                null, null, true), 500).size();
        return new AdminDashboard(p, from, to, total, completed, cancelled, noShow,
                dashQuery.checkedIn(hospitalId, from, to), rate, activeDoctors, doctors, departments);
    }

    private List<CountStat> aggregate(List<Object[]> rows) {
        Map<Long, long[]> sums = new LinkedHashMap<>();
        Map<Long, String> names = new HashMap<>();
        for (Object[] r : rows) {
            Long id = (Long) r[0];
            names.put(id, (String) r[1]);
            AppointmentStatus status = (AppointmentStatus) r[2];
            long n = ((Number) r[3]).longValue();
            long[] s = sums.computeIfAbsent(id, k -> new long[4]);
            s[0] += n;
            if (status == COMPLETED) s[1] += n;
            else if (status == CANCELLED) s[2] += n;
            else if (status == NO_SHOW) s[3] += n;
        }
        return sums.entrySet().stream()
                .map(e -> new CountStat(names.get(e.getKey()), e.getValue()[0], e.getValue()[1],
                        e.getValue()[2], e.getValue()[3]))
                .sorted(Comparator.comparingLong(CountStat::booked).reversed()).toList();
    }
}