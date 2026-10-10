package com.medislot.service;

import com.medislot.entity.Appointment;
import com.medislot.entity.AppointmentStatus;
import com.medislot.event.AppointmentStatusChanged;
import com.medislot.exception.ApiException;
import com.medislot.repository.AppointmentStatusHistoryRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AppointmentStateMachineTest {

    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final AppointmentStateMachine machine =
            new AppointmentStateMachine(mock(AppointmentStatusHistoryRepository.class), events);

    @Test
    void validMoveChangesStatusAndPublishesAnEvent() {
        Appointment a = new Appointment();
        a.setStatus(AppointmentStatus.HELD);
        machine.move(a, AppointmentStatus.CONFIRMED, 5L, "Booked");

        assertEquals(AppointmentStatus.CONFIRMED, a.getStatus());
        ArgumentCaptor<AppointmentStatusChanged> captor = ArgumentCaptor.forClass(AppointmentStatusChanged.class);
        verify(events).publishEvent(captor.capture());
        assertEquals(AppointmentStatus.HELD, captor.getValue().from());
        assertEquals(AppointmentStatus.CONFIRMED, captor.getValue().to());
        assertEquals(5L, captor.getValue().actorUserId());
    }

    @Test
    void invalidMoveIsRejectedAndPublishesNothing() {
        Appointment a = new Appointment();
        a.setStatus(AppointmentStatus.CONFIRMED);
        ApiException ex = assertThrows(ApiException.class, () -> machine.move(a, AppointmentStatus.COMPLETED, 5L, "x"));
        assertEquals("INVALID_STATUS_TRANSITION", ex.getCode());
        assertEquals(AppointmentStatus.CONFIRMED, a.getStatus());
        verify(events, never()).publishEvent(any(Object.class));
    }

    @Test
    void attendanceRequestAndConfirmationAreValidMoves() {
        assertTrue(AppointmentStatus.CONFIRMED.canTransitionTo(AppointmentStatus.ATTENDANCE_PENDING));
        assertTrue(AppointmentStatus.ATTENDANCE_PENDING.canTransitionTo(AppointmentStatus.CONFIRMED));
    }
}