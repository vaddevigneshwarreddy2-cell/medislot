package com.medislot.service;

import com.medislot.config.AppProperties;
import com.medislot.dto.AppointmentDtos.AppointmentView;
import com.medislot.dto.AppointmentDtos.ConfirmRequest;
import com.medislot.dto.AppointmentDtos.HoldRequest;
import com.medislot.dto.DoctorDtos.Slot;
import com.medislot.entity.Patient;
import com.medislot.entity.Role;
import com.medislot.entity.User;
import com.medislot.exception.ApiException;
import com.medislot.repository.PatientRepository;
import com.medislot.repository.UserRepository;
import com.medislot.security.AuthUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Runs against your LOCAL development database (not part of "mvn test").
 * Run with:  mvn "-Dtest=BookingRulesIT" test
 * Creates temporary users it-test-*@test.local and removes them afterwards.
 */
@SpringBootTest
class BookingRulesIT {

    private record Day(LocalDate date, List<Slot> slots) {}

    @Autowired AppointmentService appointments;
    @Autowired SlotService slots;
    @Autowired UserRepository users;
    @Autowired PatientRepository patients;
    @Autowired JdbcTemplate jdbc;
    @Autowired AppProperties props;

    @AfterEach
    void cleanup() {
        String own = "select p.id from patients p join users u on u.id = p.user_id where u.email like 'it-test-%'";
        jdbc.update("delete from appointment_status_history where appointment_id in "
                + "(select id from appointments where patient_id in (" + own + "))");
        jdbc.update("delete from appointments where patient_id in (" + own + ")");
        jdbc.update("delete from users where email like 'it-test-%'");
    }

    private Long doctorId() {
        return jdbc.queryForObject(
                "select d.id from doctors d join users u on u.id = d.user_id where u.email = 'doctor@demo.com'",
                Long.class);
    }

    private AuthUser patient() {
        User u = new User();
        u.setEmail("it-test-" + System.nanoTime() + "@test.local");
        u.setFullName("IT Test");
        u.setPasswordHash("not-a-real-hash");
        u.setRole(Role.PATIENT);
        u.setVerified(true);
        u = users.save(u);
        Patient p = new Patient();
        p.setUserId(u.getId());
        patients.save(p);
        return new AuthUser(u.getId(), u.getEmail(), Role.PATIENT);
    }

    /** Future days (from tomorrow) on which the doctor has at least minSlots free slots. */
    private List<Day> days(Long doctorId, int minSlots, int count) {
        List<Day> found = new ArrayList<>();
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        for (int i = 1; i <= 28 && found.size() < count; i++) {
            var a = slots.availability(doctorId, today.plusDays(i));
            if (a.slots().size() >= minSlots) found.add(new Day(today.plusDays(i), a.slots()));
        }
        assumeTrue(found.size() >= count, "Not enough free days for the demo doctor");
        return found;
    }

    private AppointmentView book(AuthUser who, Long doctor, LocalDate date, String time) {
        AppointmentView held = appointments.hold(who, new HoldRequest(doctor, date, time));
        return appointments.confirm(who, new ConfirmRequest(held.appointmentId(), null));
    }

    @Test
    void anExpiredHoldFreesTheSlotForSomeoneElse() {
        Long doc = doctorId();
        Day day = days(doc, 1, 1).get(0);
        String time = day.slots().get(0).start();
        AuthUser a = patient();
        AuthUser b = patient();

        AppointmentView held = appointments.hold(a, new HoldRequest(doc, day.date(), time));
        jdbc.update("update appointments set hold_expires_at = now() - interval '1 minute' where id = ?",
                held.appointmentId());

        AppointmentView second = appointments.hold(b, new HoldRequest(doc, day.date(), time));
        assertEquals("HELD", second.status());
        assertEquals("EXPIRED", jdbc.queryForObject("select status from appointments where id = ?",
                String.class, held.appointmentId()));
        ApiException ex = assertThrows(ApiException.class,
                () -> appointments.confirm(a, new ConfirmRequest(held.appointmentId(), null)));
        assertEquals("HOLD_EXPIRED", ex.getCode());
    }

    @Test
    void secondBookingWithTheSameDoctorOnTheSameDayIsRejected() {
        Long doc = doctorId();
        Day day = days(doc, 2, 1).get(0);
        AuthUser a = patient();
        book(a, doc, day.date(), day.slots().get(0).start());
        ApiException ex = assertThrows(ApiException.class,
                () -> appointments.hold(a, new HoldRequest(doc, day.date(), day.slots().get(1).start())));
        assertEquals("DUPLICATE_BOOKING", ex.getCode());
    }

    @Test
    void aCancelledAppointmentFreesItsSlot() {
        Long doc = doctorId();
        Day day = days(doc, 1, 1).get(0);
        String time = day.slots().get(0).start();
        AuthUser a = patient();
        AuthUser b = patient();
        AppointmentView booked = book(a, doc, day.date(), time);
        assertEquals("SLOT_UNAVAILABLE", assertThrows(ApiException.class,
                () -> appointments.hold(b, new HoldRequest(doc, day.date(), time))).getCode());

        assertEquals("CANCELLED", appointments.cancel(a, booked.appointmentId(), "test").status());
        assertEquals("HELD", appointments.hold(b, new HoldRequest(doc, day.date(), time)).status());
    }

    @Test
    void theActiveAppointmentLimitIsEnforced() {
        Long doc = doctorId();
        int max = props.booking().maxActiveAppointments();
        List<Day> ds = days(doc, 1, max + 1);
        AuthUser a = patient();
        for (int i = 0; i < max; i++) book(a, doc, ds.get(i).date(), ds.get(i).slots().get(0).start());
        Day extra = ds.get(max);
        ApiException ex = assertThrows(ApiException.class,
                () -> appointments.hold(a, new HoldRequest(doc, extra.date(), extra.slots().get(0).start())));
        assertEquals("MAX_ACTIVE_APPOINTMENTS_REACHED", ex.getCode());
    }
}