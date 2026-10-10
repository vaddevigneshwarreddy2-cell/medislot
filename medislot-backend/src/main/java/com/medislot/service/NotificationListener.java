package com.medislot.service;

import com.medislot.entity.AppointmentStatus;
import com.medislot.entity.Consultation;
import com.medislot.entity.NotificationType;
import com.medislot.event.AppointmentStatusChanged;
import com.medislot.event.LeaveCreated;
import com.medislot.repository.AppointmentInsightRepository;
import com.medislot.repository.ConsultationRepository;
import com.medislot.repository.PrescriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.EnumSet;
import java.util.Objects;

import static com.medislot.entity.AppointmentStatus.*;

/**
 * Turns committed appointment events into notifications. Runs after the booking transaction commits,
 * in its own transaction, and never lets a failure reach the caller.
 */
@Component
public class NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    private final NotificationService notifications;
    private final AppointmentFacts facts;
    private final AppointmentInsightRepository insight;
    private final ConsultationRepository consultations;
    private final PrescriptionRepository prescriptions;
    private final TransactionTemplate newTx;

    public NotificationListener(NotificationService notifications, AppointmentFacts facts,
                                AppointmentInsightRepository insight, ConsultationRepository consultations,
                                PrescriptionRepository prescriptions, PlatformTransactionManager txManager) {
        this.notifications = notifications;
        this.facts = facts;
        this.insight = insight;
        this.consultations = consultations;
        this.prescriptions = prescriptions;
        this.newTx = new TransactionTemplate(txManager);
        this.newTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onStatusChanged(AppointmentStatusChanged e) {
        run(() -> handleStatus(e));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onLeave(LeaveCreated e) {
        run(() -> handleLeave(e));
    }

    private void run(Runnable work) {
        try {
            newTx.executeWithoutResult(st -> work.run());
        } catch (Exception ex) {
            log.warn("Could not create notification: {}", ex.getMessage());
        }
    }

    private void handleStatus(AppointmentStatusChanged e) {
        AppointmentStatus from = e.from();
        AppointmentStatus to = e.to();
        if (from == null) return; // new rows: holds, reschedule targets, walk-ins
        AppointmentFacts.Facts f = facts.load(e.appointmentId());
        if (f == null || f.patientUserId() == null) return;

        Long id = e.appointmentId();
        Long uid = f.patientUserId();
        String link = "/appointments/" + id;
        String key = "st:" + id + ":" + to;

        switch (to) {
            case CONFIRMED -> {
                if (from == HELD) {
                    notifications.send(uid, NotificationType.APPOINTMENT_CONFIRMED, "Appointment confirmed",
                            "Your appointment with " + f.doctorName() + " at " + f.hospitalName()
                                    + " is confirmed for " + f.when() + ".", link, id, key);
                }
            }
            case CANCELLED -> {
                if (from != HELD) { // releasing an unconfirmed hold is not a cancellation
                    boolean byPatient = Objects.equals(e.actorUserId(), uid);
                    String text = byPatient
                            ? "You cancelled your appointment with " + f.doctorName() + " on " + f.when() + "."
                            : "Your appointment with " + f.doctorName() + " on " + f.when()
                                    + " was cancelled by the hospital. You can book another slot.";
                    notifications.send(uid, NotificationType.APPOINTMENT_CANCELLED, "Appointment cancelled",
                            text, "/patient/appointments", id, key);
                }
            }
            case RESCHEDULED -> {
                var next = insight.findFirstByRescheduledFromId(id).map(facts::load).orElse(null);
                String text = next == null
                        ? "Your appointment with " + f.doctorName() + " was rescheduled."
                        : "Your appointment with " + f.doctorName() + " has moved to " + next.when() + ".";
                String target = next == null ? "/patient/appointments" : "/appointments/" + next.appt().getId();
                notifications.send(uid, NotificationType.APPOINTMENT_RESCHEDULED, "Appointment rescheduled",
                        text, target, id, key);
            }
            case WAITING -> {
                if (from == CHECKED_IN) {
                    Integer q = f.appt().getQueueNumber();
                    notifications.send(uid, NotificationType.CHECKED_IN, "You are checked in",
                            "You are checked in for " + f.doctorName() + "."
                                    + (q == null ? "" : " Your queue number is #" + q + "."), link, id, key);
                }
            }
            case COMPLETED -> {
                Consultation c = consultations.findByAppointmentId(id).orElse(null);
                boolean rx = c != null && prescriptions.findByConsultationId(c.getId()).isPresent();
                String followUp = c != null && c.getFollowUpDate() != null
                        ? " A follow-up is suggested for " + c.getFollowUpDate() + "." : "";
                if (rx) {
                    notifications.send(uid, NotificationType.PRESCRIPTION_AVAILABLE, "Prescription available",
                            "Your visit with " + f.doctorName() + " is complete and your prescription is ready."
                                    + followUp, "/patient/prescriptions", id, key);
                } else {
                    notifications.send(uid, NotificationType.VISIT_COMPLETED, "Visit completed",
                            "Your visit with " + f.doctorName() + " is complete." + followUp,
                            "/patient/appointments", id, key);
                }
            }
            case NO_SHOW -> notifications.send(uid, NotificationType.NO_SHOW, "Appointment marked as missed",
                    "Your appointment with " + f.doctorName() + " on " + f.when()
                            + " was marked as a no-show. Please contact the hospital if this is a mistake.",
                    "/patient/appointments", id, key);
            default -> { }
        }
    }

    private void handleLeave(LeaveCreated e) {
        var affected = insight.findByDoctorIdAndAppointmentDateAndStatusIn(e.doctorId(), e.date(),
                EnumSet.of(CONFIRMED, ATTENDANCE_PENDING));
        for (var a : affected) {
            AppointmentFacts.Facts f = facts.load(a);
            if (f.patientUserId() == null) continue;
            notifications.send(f.patientUserId(), NotificationType.DOCTOR_LEAVE, "Your doctor is on leave",
                    f.doctorName() + " will be on leave on " + e.date() + ", the day of your appointment ("
                            + f.when() + "). Please reschedule or cancel it.",
                    "/appointments/" + a.getId(), a.getId(), "leave:" + a.getId() + ":" + e.date());
        }
    }
}