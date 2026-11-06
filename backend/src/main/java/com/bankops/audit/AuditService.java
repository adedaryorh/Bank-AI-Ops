package com.bankops.audit;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService implements AuditWriter, AuditWriter.Query {

    private final AuditRepository repository;

    public AuditService(AuditRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void append(AuditRecord record) {
        AuditEvent event = new AuditEvent(
            Instant.now(),
            record.actorSubject(),
            record.actorEmail(),
            record.action(),
            record.targetType(),
            record.targetId(),
            record.outcome() == null ? "SUCCESS" : record.outcome(),
            record.details() == null ? java.util.Map.of() : record.details(),
            MDC.get("correlationId"));
        repository.save(event);
    }

    @Override
    @Transactional(readOnly = true)
    public long countSince(Instant since) {
        return repository.countByOccurredAtAfter(since);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Entry> recent(int limit) {
        int bounded = Math.max(1, Math.min(limit, 50));
        return repository.findByOrderByOccurredAtDesc(Limit.of(bounded)).stream()
            .map(event -> new Entry(event.getId(), event.getOccurredAt(), event.getAction(),
                event.getActorEmail(), event.getOutcome()))
            .toList();
    }

}
