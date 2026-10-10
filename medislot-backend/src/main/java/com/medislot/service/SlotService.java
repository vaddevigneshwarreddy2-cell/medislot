package com.medislot.service;

import com.medislot.dto.DoctorDtos.AvailabilityResponse;
import com.medislot.dto.DoctorDtos.DaySummary;
import com.medislot.dto.DoctorDtos.Slot;
import com.medislot.entity.AppointmentStatus;
import com.medislot.repository.AppointmentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Public availability with already-booked and currently-held slots removed. */
@Service
@Transactional(readOnly = true)
public class SlotService {

    private final DoctorService doctors;
    private final AppointmentRepository appts;
    private final ZoneId zone;

    public SlotService(DoctorService doctors, AppointmentRepository appts,
                       @Value("${APP_TIMEZONE:Asia/Kolkata}") String timezone) {
        this.doctors = doctors;
        this.appts = appts;
        this.zone = ZoneId.of(timezone);
    }

    public AvailabilityResponse availability(Long doctorId, LocalDate date) {
        AvailabilityResponse base = doctors.availability(doctorId, date);
        if (base.slots().isEmpty()) return base;

        Instant from = date.atStartOfDay(zone).toInstant();
        Instant to = date.plusDays(1).atStartOfDay(zone).toInstant();
        Set<Instant> taken = new HashSet<>(appts.findTakenStarts(doctorId, from, to,
                AppointmentStatus.BOOKED_OR_DONE, AppointmentStatus.HELD, Instant.now()));

        List<Slot> free = base.slots().stream()
                .filter(s -> !taken.contains(ZonedDateTime.of(date, LocalTime.parse(s.start()), zone).toInstant()))
                .toList();
        return new AvailabilityResponse(base.doctorId(), base.date(), base.durationMinutes(), free,
                free.isEmpty() ? "FULLY_BOOKED" : null);
    }

    public List<DaySummary> upcoming(Long doctorId, int days) {
        return doctors.upcoming(doctorId, days).stream().map(d -> {
            if (d.slotCount() == 0) return d;
            AvailabilityResponse a = availability(doctorId, d.date());
            return new DaySummary(d.date(), a.slots().size(), a.unavailableReason());
        }).toList();
    }
}