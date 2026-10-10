package com.medislot.service;

import com.medislot.entity.Appointment;
import com.medislot.entity.Consultation;
import com.medislot.entity.NotificationType;
import com.medislot.entity.Patient;
import com.medislot.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;

import static com.medislot.entity.AppointmentStatus.*;

/**
 * Attendance requests, short-notice reminders and follow-up reminders. Each appointment is handled in its
 * own transaction under a row lock and re-checked, so a repeat run (or a second instance) cannot double-send.
 * Nothing here marks a no-show or cancels anything.
 */
@Service
public class ReminderService {

    private static final Logger log = LoggerFactory.getLogger(ReminderService.class);

    private final AppointmentInsightRepository insight;
    private final AppointmentRepository appts;
    private final AppointmentStateMachine machine;
    private final NotificationService notifications;
    private final AppointmentFacts facts;
    private final ConsultationRepository consultations;
    private final PatientRepository patients;
    private final DoctorQueryRepository doctorQuery;
    private final TransactionTemplate tx;
    private final ZoneId zone;
    private final int attendanceHours;
    private final int reminderHours;
    private final int followUpDays;

    public ReminderService(AppointmentInsightRepository insight, AppointmentRepository appts,
                           AppointmentStateMachine machine, NotificationService notifications,
                           AppointmentFacts facts, ConsultationRepository consultations,
                           PatientRepository patients, DoctorQueryRepository doctorQuery,
                           PlatformTransactionManager txManager,
                           @Value("${APP_TIMEZONE:Asia/Kolkata}") String timezone,
                           @Value("${ATTENDANCE_REQUEST_HOURS:24}") int attendanceHours,
                           @Value("${REMINDER_HOURS:2}") int reminderHours,
                           @Value("${FOLLOWUP_REMINDER_DAYS:2}") int followUpDays) {
        this.insight = insight;
        this.appts = appts;
        this.machine = machine;
        this.notifications = notifications;
        this.facts = facts;
        this.consultations = consultations;
        this.patients = patients;
        this.doctorQuery = doctorQuery;
        this.tx = new TransactionTemplate(txManager);
        this.zone = ZoneId.of(timezone);
        this.attendanceHours = attendanceHours;
        this.reminderHours = reminderHours;
        this.followUpDays = followUpDays;
    }

    public void runOnce() {
        attendanceRequests();
        finalReminders();
        followUpReminders();
    }

    // ---------- attendance request (CONFIRMED -> ATTENDANCE_PENDING) ----------

    void attendanceRequests() {
        Instant now = Instant.now();
        for (Long id : insight.attendanceCandidates(CONFIRMED, now, now.plus(attendanceHours, ChronoUnit.HOURS),
                PageRequest.of(0, 200))) {
            try {
                tx.executeWithoutResult(st -> requestAttendance(id));
            } catch (Exception e) {
                log.warn("Attendance request failed for appointment {}: {}", id, e.getMessage());
            }
        }
    }

    private void requestAttendance(Long id) {
        Appointment a = appts.findByIdForUpdate(id).orElse(null);
        if (a == null || a.getStatus() != CONFIRMED || a.getAttendanceRequestedAt() != null) return;
        a.setAttendanceRequestedAt(Instant.now()); // also stops late bookings being re-checked every run
        if (!ReminderPolicy.bookedEarlyEnough(a.getCreatedAt(), a.getSlotStart(), attendanceHours)) return;

        machine.move(a, ATTENDANCE_PENDING, null, "Attendance confirmation requested");
        AppointmentFacts.Facts f = facts.load(a);
        notifications.send(f.patientUserId(), NotificationType.ATTENDANCE_REQUEST, "Please confirm your attendance",
                "Your appointment with " + f.doctorName() + " is on " + f.when()
                        + ". Confirm that you will attend, or cancel to free the slot.",
                "/appointments/" + id, id, "attend:" + id);
    }

    // ---------- short-notice reminder ----------

    void finalReminders() {
        Instant now = Instant.now();
        for (Long id : insight.reminderCandidates(EnumSet.of(CONFIRMED, ATTENDANCE_PENDING), now,
                now.plus(reminderHours, ChronoUnit.HOURS), PageRequest.of(0, 200))) {
            try {
                tx.executeWithoutResult(st -> remind(id));
            } catch (Exception e) {
                log.warn("Reminder failed for appointment {}: {}", id, e.getMessage());
            }
        }
    }

    private void remind(Long id) {
        Appointment a = appts.findByIdForUpdate(id).orElse(null);
        if (a == null || a.getReminderSentAt() != null
                || (a.getStatus() != CONFIRMED && a.getStatus() != ATTENDANCE_PENDING)) return;
        a.setReminderSentAt(Instant.now());
        if (!ReminderPolicy.bookedEarlyEnough(a.getCreatedAt(), a.getSlotStart(), reminderHours)) return;

        AppointmentFacts.Facts f = facts.load(a);
        notifications.send(f.patientUserId(), NotificationType.APPOINTMENT_REMINDER, "Appointment reminder",
                "Your appointment with " + f.doctorName() + " at " + f.hospitalName() + " is on " + f.when()
                        + ". Have your QR code ready.",
                "/appointments/" + id, id, "remind:" + id);
    }

    // ---------- follow-up reminder ----------

    void followUpReminders() {
        LocalDate today = LocalDate.now(zone);
        for (Consultation c : consultations.findFollowUpsBetween(today, today.plusDays(followUpDays))) {
            try {
                tx.executeWithoutResult(st -> followUp(c));
            } catch (Exception e) {
                log.warn("Follow-up reminder failed for consultation {}: {}", c.getId(), e.getMessage());
            }
        }
    }

    private void followUp(Consultation c) {
        Patient p = patients.findById(c.getPatientId()).orElse(null);
        DoctorRow d = doctorQuery.findById(c.getDoctorId(), false).orElse(null);
        if (p == null || d == null) return;
        notifications.send(p.getUserId(), NotificationType.FOLLOW_UP_REMINDER, "Follow-up reminder",
                "Your follow-up with " + d.fullName() + " is suggested on " + c.getFollowUpDate()
                        + ". Book a slot to confirm it.",
                "/doctors/" + c.getDoctorId(), c.getAppointmentId(), "followup:" + c.getId());
    }
}