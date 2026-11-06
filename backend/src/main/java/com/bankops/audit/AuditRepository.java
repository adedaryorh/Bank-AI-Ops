package com.bankops.audit;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditRepository extends JpaRepository<AuditEvent, UUID> {

    long countByOccurredAtAfter(Instant since);

    List<AuditEvent> findByOrderByOccurredAtDesc(Limit limit);

    @Query("select count(a) from AuditEvent a where a.occurredAt >= :from")
    long countSince(@Param("from") Instant from);
}
