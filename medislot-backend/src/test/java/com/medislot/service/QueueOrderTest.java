package com.medislot.service;

import com.medislot.entity.AppointmentStatus;
import com.medislot.repository.StaffRow;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class QueueOrderTest {

    private StaffRow row(long id, AppointmentStatus status, String slot, Integer queue, long doctorId) {
        Instant start = Instant.parse(slot);
        return new StaffRow(id, status, LocalDate.of(2026, 10, 12), start, start.plusSeconds(1800), queue,
                null, null, "ONLINE", null, "P" + id, "9876543210", doctorId, "Dr", "Gen", 30);
    }

    @Test
    void earlierAppointmentTimeComesFirst_thenCheckInOrder() {
        List<StaffRow> rows = List.of(
                row(1, AppointmentStatus.WAITING, "2026-10-12T05:00:00Z", 1, 7),
                row(2, AppointmentStatus.WAITING, "2026-10-12T04:30:00Z", 2, 7),
                row(3, AppointmentStatus.WAITING, "2026-10-12T04:30:00Z", 3, 7));
        List<StaffRow> ordered = QueueService.waitingOrder(rows);
        assertEquals(List.of(2L, 3L, 1L), ordered.stream().map(StaffRow::id).toList());
    }

    @Test
    void inConsultationAndConfirmedAreNotInTheWaitingOrder() {
        List<StaffRow> rows = List.of(
                row(1, AppointmentStatus.IN_CONSULTATION, "2026-10-12T04:00:00Z", 1, 7),
                row(2, AppointmentStatus.CONFIRMED, "2026-10-12T04:30:00Z", null, 7),
                row(3, AppointmentStatus.WAITING, "2026-10-12T05:00:00Z", 2, 7));
        assertEquals(1, QueueService.waitingOrder(rows).size());
    }

    @Test
    void positionsAreCountedPerDoctor() {
        List<StaffRow> rows = List.of(
                row(1, AppointmentStatus.WAITING, "2026-10-12T04:30:00Z", 1, 7),
                row(2, AppointmentStatus.WAITING, "2026-10-12T04:30:00Z", 1, 8),
                row(3, AppointmentStatus.WAITING, "2026-10-12T05:00:00Z", 2, 7));
        Map<Long, Integer> pos = QueueService.positions(rows);
        assertEquals(1, pos.get(1L));
        assertEquals(1, pos.get(2L));
        assertEquals(2, pos.get(3L));
    }
}