package com.medislot.service;

import com.medislot.dto.DoctorDtos.*;
import com.medislot.dto.HospitalDtos;
import com.medislot.entity.DoctorAvailability;
import com.medislot.exception.ApiException;
import com.medislot.repository.*;
import com.medislot.util.LikeUtil;
import com.medislot.util.SlotGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/** Public doctor discovery and slot computation. */
@Service
@Transactional(readOnly = true)
public class DoctorService {

    private static final int MAX_RESULTS = 100;

    private final DoctorQueryRepository query;
    private final DoctorAvailabilityRepository availability;
    private final DoctorLeaveRepository leave;
    private final HospitalHolidayRepository holidays;
    private final HospitalRepository hospitals;
    private final HospitalService hospitalService;
    private final ZoneId zone;
    private final int windowDays;

    public DoctorService(DoctorQueryRepository query, DoctorAvailabilityRepository availability,
                         DoctorLeaveRepository leave, HospitalHolidayRepository holidays,
                         HospitalRepository hospitals, HospitalService hospitalService,
                         @Value("${APP_TIMEZONE:Asia/Kolkata}") String timezone,
                         @Value("${BOOKING_WINDOW_DAYS:30}") int windowDays) {
        this.query = query;
        this.availability = availability;
        this.leave = leave;
        this.holidays = holidays;
        this.hospitals = hospitals;
        this.hospitalService = hospitalService;
        this.zone = ZoneId.of(timezone);
        this.windowDays = windowDays;
    }

    public DoctorList search(String name, String specialization, Long hospitalId, Long departmentId,
                             Double latitude, Double longitude, Double radiusKm) {
        Map<Long, Double> distances = null;
        if (latitude != null || longitude != null) {
            if (latitude == null || longitude == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_COORDINATES",
                        "Provide both latitude and longitude.");
            }
            // Reuses the Phase 3 validation, default radius and distance sorting.
            distances = hospitalService.nearby(latitude, longitude, radiusKm).hospitals().stream()
                    .collect(Collectors.toMap(HospitalDtos.Summary::id, HospitalDtos.Summary::distanceKm, (a, b) -> a));
            if (distances.isEmpty()) return new DoctorList(0, List.of());
        }

        var filter = new DoctorQueryRepository.Filter(null, hospitalId,
                distances == null ? null : distances.keySet(), departmentId,
                LikeUtil.contains(specialization), LikeUtil.contains(name), true);

        final Map<Long, Double> dist = distances;
        List<DoctorSummary> result = new ArrayList<>(query.search(filter, MAX_RESULTS).stream()
                .map(r -> summary(r, dist == null ? null : dist.get(r.hospitalId()))).toList());
        if (dist != null) {
            result.sort(Comparator.comparing(DoctorSummary::distanceKm, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(DoctorSummary::fullName));
        }
        return new DoctorList(result.size(), result);
    }

    public DoctorList forHospital(Long hospitalId, Long departmentId) {
        hospitals.findByIdAndActiveTrue(hospitalId).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "HOSPITAL_NOT_FOUND", "Hospital not found."));
        return search(null, null, hospitalId, departmentId, null, null, null);
    }

    public DoctorDetail get(Long id) {
        DoctorRow r = find(id);
        List<ScheduleWindow> schedule = availability.findByDoctorIdOrderByDayOfWeekAscStartTimeAsc(id).stream()
                .map(a -> new ScheduleWindow(a.getDayOfWeek(), SlotGenerator.format(a.getStartTime()),
                        SlotGenerator.format(a.getEndTime()))).toList();
        return new DoctorDetail(r.id(), r.fullName(), r.specialization(), r.qualification(), r.experienceYears(),
                r.consultationDurationMin(), r.bio(), r.hospitalId(), r.hospitalName(), r.hospitalCity(),
                r.departmentId(), r.departmentName(), schedule);
    }

    public AvailabilityResponse availability(Long id, LocalDate date) {
        return day(find(id), date);
    }

    public List<DaySummary> upcoming(Long id, int days) {
        if (days < 1 || days > 31) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DAYS", "days must be between 1 and 31.");
        }
        DoctorRow r = find(id);
        LocalDate today = LocalDate.now(zone);
        List<DaySummary> out = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            AvailabilityResponse a = day(r, today.plusDays(i));
            out.add(new DaySummary(a.date(), a.slots().size(), a.unavailableReason()));
        }
        return out;
    }

    // ---------- helpers ----------

    private DoctorRow find(Long id) {
        return query.findById(id, true).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "DOCTOR_NOT_FOUND", "Doctor not found."));
    }

    private DoctorSummary summary(DoctorRow r, Double distance) {
        return new DoctorSummary(r.id(), r.fullName(), r.specialization(), r.qualification(), r.experienceYears(),
                r.consultationDurationMin(), r.hospitalId(), r.hospitalName(), r.hospitalCity(),
                r.departmentId(), r.departmentName(), distance);
    }

    private AvailabilityResponse day(DoctorRow r, LocalDate date) {
        int duration = r.consultationDurationMin();
        LocalDate today = LocalDate.now(zone);
        String reason = null;
        List<Slot> slots = List.of();

        if (date.isBefore(today)) {
            reason = "PAST_DATE";
        } else if (date.isAfter(today.plusDays(windowDays))) {
            reason = "TOO_FAR";
        } else if (leave.existsByDoctorIdAndLeaveDate(r.id(), date)) {
            reason = "DOCTOR_ON_LEAVE";
        } else if (holidays.existsByHospitalIdAndHolidayDate(r.hospitalId(), date)) {
            reason = "HOSPITAL_HOLIDAY";
        } else {
            List<DoctorAvailability> windows =
                    availability.findByDoctorIdAndDayOfWeekOrderByStartTimeAsc(r.id(), date.getDayOfWeek().getValue());
            if (windows.isEmpty()) {
                reason = "NOT_WORKING_DAY";
            } else {
                LocalTime now = LocalTime.now(zone);
                List<Slot> list = new ArrayList<>();
                for (DoctorAvailability w : windows) {
                    for (LocalTime s : SlotGenerator.startTimes(w.getStartTime(), w.getEndTime(), duration)) {
                        if (date.equals(today) && !s.isAfter(now)) continue; // already past today
                        list.add(new Slot(SlotGenerator.format(s), SlotGenerator.format(s.plusMinutes(duration))));
                    }
                }
                slots = list;
                if (slots.isEmpty()) reason = "NO_SLOTS_LEFT";
            }
        }
        return new AvailabilityResponse(r.id(), date, duration, slots, reason);
    }
}