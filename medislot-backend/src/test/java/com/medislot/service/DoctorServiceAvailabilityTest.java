package com.medislot.service;

import com.medislot.dto.DoctorDtos.AvailabilityResponse;
import com.medislot.entity.DoctorAvailability;
import com.medislot.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DoctorServiceAvailabilityTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    private final DoctorQueryRepository query = mock(DoctorQueryRepository.class);
    private final DoctorAvailabilityRepository availability = mock(DoctorAvailabilityRepository.class);
    private final DoctorLeaveRepository leave = mock(DoctorLeaveRepository.class);
    private final HospitalHolidayRepository holidays = mock(HospitalHolidayRepository.class);
    private final DoctorService service = new DoctorService(query, availability, leave, holidays,
            mock(HospitalRepository.class), mock(HospitalService.class), "Asia/Kolkata", 30);

    private final LocalDate tomorrow = LocalDate.now(ZONE).plusDays(1);

    @BeforeEach
    void setUp() {
        when(query.findById(1L, true)).thenReturn(Optional.of(new DoctorRow(1L, "Dr A", "a@b.com",
                "Cardiologist", "MD", 10, 30, null, true, 7L, "H", "Hyderabad", 3L, "Cardiology")));
    }

    private void works(String start, String end) {
        DoctorAvailability a = new DoctorAvailability();
        a.setDoctorId(1L);
        a.setDayOfWeek(tomorrow.getDayOfWeek().getValue());
        a.setStartTime(LocalTime.parse(start));
        a.setEndTime(LocalTime.parse(end));
        when(availability.findByDoctorIdAndDayOfWeekOrderByStartTimeAsc(1L, tomorrow.getDayOfWeek().getValue()))
                .thenReturn(List.of(a));
    }

    @Test
    void workingDayProducesSlots() {
        works("10:00", "13:00");
        AvailabilityResponse r = service.availability(1L, tomorrow);
        assertNull(r.unavailableReason());
        assertEquals(6, r.slots().size());
        assertEquals("10:00", r.slots().get(0).start());
        assertEquals("13:00", r.slots().get(5).end());
    }

    @Test
    void leaveRemovesAllSlots() {
        works("10:00", "13:00");
        when(leave.existsByDoctorIdAndLeaveDate(1L, tomorrow)).thenReturn(true);
        AvailabilityResponse r = service.availability(1L, tomorrow);
        assertEquals("DOCTOR_ON_LEAVE", r.unavailableReason());
        assertTrue(r.slots().isEmpty());
    }

    @Test
    void hospitalHolidayRemovesAllSlots() {
        works("10:00", "13:00");
        when(holidays.existsByHospitalIdAndHolidayDate(7L, tomorrow)).thenReturn(true);
        assertEquals("HOSPITAL_HOLIDAY", service.availability(1L, tomorrow).unavailableReason());
    }

    @Test
    void dayWithoutWorkingHoursIsNotAWorkingDay() {
        assertEquals("NOT_WORKING_DAY", service.availability(1L, tomorrow).unavailableReason());
    }

    @Test
    void pastDatesAreUnavailable() {
        assertEquals("PAST_DATE", service.availability(1L, LocalDate.now(ZONE).minusDays(1)).unavailableReason());
    }

    @Test
    void datesBeyondTheBookingWindowAreUnavailable() {
        assertEquals("TOO_FAR", service.availability(1L, LocalDate.now(ZONE).plusDays(31)).unavailableReason());
    }
}