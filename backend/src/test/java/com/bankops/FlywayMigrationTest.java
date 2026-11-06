package com.bankops;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/** Flyway applies on a clean database and the Phase 1 schema exists. */
@SpringBootTest
class FlywayMigrationTest extends IntegrationTestSupport {

    @Autowired
    JdbcTemplate jdbc;

    @BeforeAll
    static void require() {
        requireDatabase();
    }

    @Test
    void schemaAndSeedDataExist() {
        List<String> tables = jdbc.queryForList("""
            select table_name from information_schema.tables
            where table_schema = 'public' and table_type = 'BASE TABLE'
            """, String.class);

        assertThat(tables).contains("departments", "users", "user_roles", "audit_events");
        assertThat(tables).contains("flyway_schema_history");

        Integer departments = jdbc.queryForObject("select count(*) from departments", Integer.class);
        assertThat(departments).isEqualTo(5);

        Integer migration = jdbc.queryForObject("""
            select count(*) from flyway_schema_history where success = true
            """, Integer.class);
        assertThat(migration).isGreaterThanOrEqualTo(1);
    }

    @Test
    void appendOnlyTriggerExists() {
        Map<String, Object> trigger = jdbc.queryForMap("""
            select tgname, tgtype from pg_trigger
            where tgrelid = 'audit_events'::regclass and not tgisinternal
              and tgname = 'audit_events_append_only'
            """);
        assertThat(trigger.get("tgname")).isEqualTo("audit_events_append_only");
    }
}
