package com.medislot.dto;

import java.time.Instant;

public final class AuditDtos {

    private AuditDtos() {}

    public record AuditView(Long id, Long actorUserId, String actorEmail, String action, String resourceType,
                            String resourceId, String metadata, String ipAddress, Instant createdAt) {}
}