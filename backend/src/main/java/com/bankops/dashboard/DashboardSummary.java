package com.bankops.dashboard;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.bankops.identity.MeResponse;

/**
 * Dashboard payload. A widget that has no implementation yet is returned with
 * {@code available=false} and a reason — never with a zero or a placeholder value.
 */
public record DashboardSummary(
    Instant generatedAt,
    MeResponse user,
    List<Metric> metrics,
    List<Widget> widgets,
    List<AuditEntry> recentAudit,
    String notice
) {

    public record Metric(String key, String label, Long value, boolean available, String reason) {
    }

    public record Widget(String key, String label, String module, boolean available, String reason) {
    }

    public record AuditEntry(UUID id, Instant occurredAt, String action, String actorEmail, String outcome) {
    }
}
