package com.medislot.dto;

import java.time.Instant;

public final class NotificationDtos {

    private NotificationDtos() {}

    public record NotificationView(Long id, String type, String title, String message, String link,
                                   Long appointmentId, boolean read, Instant createdAt) {}

    public record UnreadCount(long count) {}

    public record AttendanceResult(Long appointmentId, String status, Instant confirmedAt) {}
}