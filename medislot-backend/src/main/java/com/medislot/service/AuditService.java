package com.medislot.service;

import com.medislot.entity.AuditLog;
import com.medislot.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    /** Never pass passwords, tokens, OTP codes or medical data in metadata. */
    public void log(Long actorUserId, String actorEmail, String action,
                    String resourceType, String resourceId, String metadata, String ip) {
        AuditLog entry = new AuditLog();
        entry.setActorUserId(actorUserId);
        entry.setActorEmail(actorEmail);
        entry.setAction(action);
        entry.setResourceType(resourceType);
        entry.setResourceId(resourceId);
        entry.setMetadata(metadata);
        entry.setIpAddress(ip);
        repository.save(entry);
    }
}