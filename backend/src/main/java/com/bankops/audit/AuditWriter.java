package com.bankops.audit;

import java.time.Instant;
import java.util.List;
import java.util.UUID;


public interface AuditWriter {

    void append(AuditRecord record);

    interface Query {

        long countSince(Instant since);

        List<Entry> recent(int limit);
    }

    record Entry(UUID id, Instant occurredAt, String action, String actorEmail, String outcome) {
    }
}
