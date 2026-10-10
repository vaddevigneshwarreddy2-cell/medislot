package com.medislot.service;

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
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Runs against your LOCAL development database (not part of "mvn test").
 * Run with:  mvn "-Dtest=SlotConcurrencyIT" test
 * It creates temporary users named conc-test-N@test.local and removes them afterwards.
 */
@SpringBootTest
class SlotConcurrencyIT {

    private static final int THREADS = 10;

    @Autowired AppointmentService appointments;
    @Autowired SlotService slots;
    @Autowired UserRepository users;
    @Autowired PatientRepository patients;
    @Autowired JdbcTemplate jdbc;

    @AfterEach
    void cleanup() {
        String own = "select p.id from patients p join users u on u.id = p.user_id where u.email like 'conc-test-%'";
        jdbc.update("delete from appointment_status_history where appointment_id in "
                + "(select id from appointments where patient_id in (" + own + "))");
        jdbc.update("delete from appointments where patient_id in (" + own + ")");
        jdbc.update("delete from users where email like 'conc-test-%'");
    }

    @Test
    void onlyOneOfManySimultaneousHoldsWinsTheSlot() throws Exception {
        Long doctorId = jdbc.queryForObject(
                "select d.id from doctors d join users u on u.id = d.user_id where u.email = 'doctor@demo.com'",
                Long.class);

        LocalDate date = null;
        Slot slot = null;
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        for (int i = 1; i <= 14 && slot == null; i++) {
            var day = slots.availability(doctorId, today.plusDays(i));
            if (!day.slots().isEmpty()) {
                date = today.plusDays(i);
                slot = day.slots().get(0);
            }
        }
        assumeTrue(slot != null, "No free slot found for the demo doctor");

        List<AuthUser> actors = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            User u = new User();
            u.setEmail("conc-test-" + i + "@test.local");
            u.setFullName("Concurrency Test " + i);
            u.setPasswordHash("not-a-real-hash");
            u.setRole(Role.PATIENT);
            u.setVerified(true);
            u = users.save(u);
            Patient p = new Patient();
            p.setUserId(u.getId());
            patients.save(p);
            actors.add(new AuthUser(u.getId(), u.getEmail(), Role.PATIENT));
        }

        final LocalDate day = date;
        final String time = slot.start();
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<String>> results = new ArrayList<>();
        for (AuthUser actor : actors) {
            results.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                try {
                    appointments.hold(actor, new HoldRequest(doctorId, day, time));
                    return "OK";
                } catch (ApiException e) {
                    return e.getCode();
                }
            }));
        }
        ready.await();
        go.countDown();

        int ok = 0;
        int unavailable = 0;
        for (Future<String> f : results) {
            String r = f.get(30, TimeUnit.SECONDS);
            if ("OK".equals(r)) ok++;
            else if ("SLOT_UNAVAILABLE".equals(r)) unavailable++;
            else fail("Unexpected result: " + r);
        }
        pool.shutdown();

        assertEquals(1, ok, "exactly one hold must succeed");
        assertEquals(THREADS - 1, unavailable);
        Integer held = jdbc.queryForObject("select count(*) from appointments a join patients p on p.id = a.patient_id "
                + "join users u on u.id = p.user_id where u.email like 'conc-test-%' and a.status = 'HELD'",
                Integer.class);
        assertEquals(1, held);
    }
}