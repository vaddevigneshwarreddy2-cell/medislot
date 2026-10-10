package com.medislot.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "actor_email", length = 255)
    private String actorEmail;

    @Column(nullable = false, length = 60)
    private String action;

    @Column(name = "resource_type", length = 60)
    private String resourceType;

    @Column(name = "resource_id", length = 60)
    private String resourceId;

    @Column(columnDefinition = "text")
    private String metadata;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setActorUserId(Long actorUserId) { this.actorUserId = actorUserId; }
    public void setActorEmail(String actorEmail) { this.actorEmail = actorEmail; }
    public void setAction(String action) { this.action = action; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
}