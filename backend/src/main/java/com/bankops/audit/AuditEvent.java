package com.bankops.audit;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;


@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "actor_subject", nullable = false, updatable = false)
    private String actorSubject;

    @Column(name = "actor_email", updatable = false)
    private String actorEmail;

    @Column(nullable = false, updatable = false)
    private String action;

    @Column(name = "target_type", updatable = false)
    private String targetType;

    @Column(name = "target_id", updatable = false)
    private String targetId;

    @Column(nullable = false, updatable = false)
    private String outcome;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, updatable = false)
    private Map<String, Object> details;

    @Column(name = "correlation_id", updatable = false)
    private String correlationId;

    protected AuditEvent() {
    }

    public AuditEvent(Instant occurredAt, String actorSubject, String actorEmail, String action,
                      String targetType, String targetId, String outcome,
                      Map<String, Object> details, String correlationId) {
        this.occurredAt = occurredAt;
        this.actorSubject = actorSubject;
        this.actorEmail = actorEmail;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.outcome = outcome;
        this.details = details;
        this.correlationId = correlationId;
    }

    public UUID getId() {
        return id;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getActorSubject() {
        return actorSubject;
    }

    public String getActorEmail() {
        return actorEmail;
    }

    public String getAction() {
        return action;
    }

    public String getTargetType() {
        return targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getOutcome() {
        return outcome;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public String getCorrelationId() {
        return correlationId;
    }
}
