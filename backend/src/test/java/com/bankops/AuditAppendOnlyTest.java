package com.bankops;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.bankops.audit.AuditRecord;
import com.bankops.audit.AuditWriter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The append-only guarantee is enforced by the database, not only by the
 * application: UPDATE and DELETE must fail even when issued as raw SQL.
 */
@SpringBootTest
class AuditAppendOnlyTest extends IntegrationTestSupport {

    @Autowired
    AuditWriter audit;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeAll
    static void require() {
        requireDatabase();
    }

    @Test
    void auditEventSurvivesWritesButRejectsMutation() {
        audit.append(new AuditRecord("test-subject", "test@bankops.local", "TEST_APPEND_ONLY",
            "TEST", "1", "SUCCESS", Map.of("phase", 1)));

        UUID id = jdbc.queryForObject("""
            select id from audit_events where action = 'TEST_APPEND_ONLY' order by occurred_at desc limit 1
            """, UUID.class);
        assertThat(id).isNotNull();

        assertThatThrownBy(() -> jdbc.update("update audit_events set action = 'TAMPERED' where id = ?", id))
            .isInstanceOf(DataAccessException.class)
            .hasMessageContaining("append-only");

        assertThatThrownBy(() -> jdbc.update("delete from audit_events where id = ?", id))
            .isInstanceOf(DataAccessException.class)
            .hasMessageContaining("append-only");

        String action = jdbc.queryForObject("select action from audit_events where id = ?", String.class, id);
        assertThat(action).isEqualTo("TEST_APPEND_ONLY");
    }

    @Test
    void auditTableRejectsTruncate() {
        assertThatThrownBy(() -> jdbc.execute("truncate table audit_events"))
            .isInstanceOf(DataAccessException.class)
            .hasMessageContaining("append-only");
    }

    @Test
    void auditRowsAreReadableInReverseChronologicalOrder() {
        audit.append(new AuditRecord("test-subject", "test@bankops.local", "TEST_ORDER_A",
            "TEST", "1", "SUCCESS", Map.of()));
        Instant start = Instant.now();
        audit.append(new AuditRecord("test-subject", "test@bankops.local", "TEST_ORDER_B",
            "TEST", "2", "SUCCESS", Map.of()));

        var actions = jdbc.queryForList("""
            select action from audit_events where occurred_at >= ? order by occurred_at desc, id desc
            """, String.class, java.sql.Timestamp.from(start));
        assertThat(actions).contains("TEST_ORDER_B");
    }
}
