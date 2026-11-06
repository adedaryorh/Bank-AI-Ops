package com.bankops.audit;

import java.util.Map;


public record AuditRecord(
    String actorSubject,
    String actorEmail,
    String action,
    String targetType,
    String targetId,
    String outcome,
    Map<String, Object> details
) {

    public static AuditRecord of(String actorSubject, String actorEmail, String action,
                                 String targetType, String targetId, Map<String, Object> details) {
        return new AuditRecord(actorSubject, actorEmail, action, targetType, targetId, "SUCCESS",
            details == null ? Map.of() : details);
    }
}
