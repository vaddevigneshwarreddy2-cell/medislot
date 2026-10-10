package com.medislot.service;

import com.medislot.entity.Appointment;
import com.medislot.entity.AppointmentStatus;
import com.medislot.entity.AppointmentStatusHistory;
import com.medislot.exception.ApiException;
import com.medislot.repository.AppointmentStatusHistoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * The only place that changes an appointment's status. Every move is validated against the
 * transition table and recorded in the history. Call it inside a transaction.
 */
@Component
public class AppointmentStateMachine {

    private final AppointmentStatusHistoryRepository history;

    public AppointmentStateMachine(AppointmentStatusHistoryRepository history) {
        this.history = history;
    }

    public void move(Appointment a, AppointmentStatus to, Long actorUserId, String note) {
        AppointmentStatus from = a.getStatus();
        if (!from.canTransitionTo(to)) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_STATUS_TRANSITION",
                    "This appointment cannot move from " + from + " to " + to + ".");
        }
        a.setStatus(to);
        if (to != AppointmentStatus.HELD) {
            a.setHoldExpiresAt(null);
        }
        write(a.getId(), from.name(), to.name(), actorUserId, note);
    }

    /** Records the creation of an already-saved appointment (no previous status). */
    public void recordInitial(Appointment a, Long actorUserId, String note) {
        write(a.getId(), null, a.getStatus().name(), actorUserId, note);
    }

    private void write(Long appointmentId, String from, String to, Long actor, String note) {
        AppointmentStatusHistory h = new AppointmentStatusHistory();
        h.setAppointmentId(appointmentId);
        h.setFromStatus(from);
        h.setToStatus(to);
        h.setChangedBy(actor);
        h.setNote(note);
        history.save(h);
    }
}