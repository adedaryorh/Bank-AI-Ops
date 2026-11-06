package com.bankops.dashboard;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.bankops.audit.AuditWriter;
import com.bankops.config.BankOpsProperties;
import com.bankops.common.UserContext;
import com.bankops.identity.IdentityQuery;
import com.bankops.identity.MeResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final com.bankops.workspace.WorkspaceOverview overview;
    private final IdentityQuery identity;
    private final AuditWriter.Query audit;
    private final BankOpsProperties properties;

    public DashboardService(IdentityQuery identity, AuditWriter.Query audit, BankOpsProperties properties, com.bankops.workspace.WorkspaceOverview overview) {
        this.overview=overview;
        this.identity = identity;
        this.audit = audit;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public DashboardSummary summary(UserContext user) {
        Instant now = Instant.now();

        List<DashboardSummary.Metric> metrics = new ArrayList<>(List.of(
            new DashboardSummary.Metric("users.total", "Provisioned users", identity.countUsers(), true, null),
            new DashboardSummary.Metric("departments.total", "Departments", identity.countDepartments(), true, null),
            new DashboardSummary.Metric("audit.events.24h", "Audit events (last 24 h)",
                audit.countSince(now.minus(Duration.ofHours(24))), true, null)));
        var measured=overview.counts(user);
        metrics.add(new DashboardSummary.Metric("cases.open","Open cases in scope",measured.get("cases.open"),true,null));
        metrics.add(new DashboardSummary.Metric("reviews.pending","Cases awaiting human review",measured.get("reviews.pending"),true,null));
        metrics.add(new DashboardSummary.Metric("documents.ready","Processed documents in scope",measured.get("documents.ready"),true,null));

        List<DashboardSummary.AuditEntry> recent = (user.hasRole("ADMIN") || user.hasRole("AUDITOR"))
            ? audit.recent(5).stream()
            .map(entry -> new DashboardSummary.AuditEntry(entry.id(), entry.occurredAt(), entry.action(),
                entry.actorEmail(), entry.outcome()))
            .toList() : List.of();

        return new DashboardSummary(
            now,
            MeResponse.from(user),
            metrics,
            widgetsFor(user),
            recent,
            properties.notice());
    }

    private List<DashboardSummary.Widget> widgetsFor(UserContext user) {
        Map<String, DashboardSummary.Widget> widgets = new LinkedHashMap<>();

        if (user.hasRole("ADMIN")) {
            widgets.put("identity.users", available("identity.users", "User administration", "Identity"));
            widgets.put("audit.trail", available("audit.trail", "Audit trail", "Audit"));
        }
        if (user.hasRole("AUDITOR")) {
            widgets.put("audit.trail", available("audit.trail", "Audit trail (read-only)", "Audit"));
        }
        if (user.hasRole("OPERATIONS_OFFICER")) {
            widgets.put("operations.cases", available("operations.cases", "Case workspace", "Banking Operations"));
        }
        if (user.hasRole("COMPLIANCE_ANALYST")) {
            widgets.put("compliance.policies", available("compliance.policies", "Policy evidence", "Compliance"));
        }
        if (user.hasRole("AML_ANALYST")) {
            widgets.put("aml.alerts", available("aml.alerts", "Synthetic AML investigations", "AML"));
        }
        if (user.hasRole("SUPERVISOR")) {
            widgets.put("reviews.queue", available("reviews.queue", "Human review queue", "Supervisor"));
        }

        if (widgets.isEmpty()) {
            widgets.put("access.none", unavailable("access.none", "No module enabled", "Platform",
                "No dashboard module is enabled for this account."));
        }
        return new ArrayList<>(widgets.values());
    }

    private DashboardSummary.Widget available(String key, String label, String module) {
        return new DashboardSummary.Widget(key, label, module, true, null);
    }

    private DashboardSummary.Widget unavailable(String key, String label, String module, String reason) {
        return new DashboardSummary.Widget(key, label, module, false, reason);
    }
}
