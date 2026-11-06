package com.bankops;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import com.bankops.identity.UserProvisioningService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ProvisioningConcurrencyTest extends IntegrationTestSupport {
    @Autowired UserProvisioningService provisioning;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;
    @BeforeAll static void require() { requireDatabase(); }

    private Jwt token(String subject) {
        return Jwt.withTokenValue("test").header("alg", "none").subject(subject)
            .claim("realm_access", Map.of("roles", List.of("OPERATIONS_OFFICER", "offline_access"))).build();
    }

    @Test
    void concurrentFirstRequestsCreateOneAccountAndOneAudit() throws Exception {
        String subject = UUID.randomUUID().toString();
        var jwt = token(subject);
        try (var executor = Executors.newFixedThreadPool(6)) {
            List<Callable<UUID>> tasks = java.util.stream.IntStream.range(0, 6)
                .mapToObj(i -> (Callable<UUID>) () -> provisioning.provision(jwt).id()).toList();
            var futures = executor.invokeAll(tasks, 20, TimeUnit.SECONDS);
            UUID id = futures.getFirst().get();
            for (var future : futures) assertThat(future.get()).isEqualTo(id);
        }
        assertThat(jdbc.queryForObject("select count(*) from users where subject = ?", Long.class, subject)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from audit_events where actor_subject = ? and action = 'USER_PROVISIONED'",
            Long.class, subject)).isEqualTo(1);
    }

    @Test
    void accountAndAuditRollbackTogether() {
        String subject = UUID.randomUUID().toString();
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            provisioning.provision(token(subject));
            status.setRollbackOnly();
        });
        assertThat(jdbc.queryForObject("select count(*) from users where subject = ?", Long.class, subject)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from audit_events where actor_subject = ?", Long.class, subject)).isZero();
    }
}
