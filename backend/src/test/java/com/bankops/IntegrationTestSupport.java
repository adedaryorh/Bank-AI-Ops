package com.bankops;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Resolves a real PostgreSQL for integration tests.
 *
 * <p>Order of preference: {@code TEST_DATABASE_URL} from the environment (used in
 * this workspace, where the Docker daemon is unavailable), otherwise a Testcontainers
 * pgvector container. If neither is possible the test suite fails explicitly; a green build must
 * not hide unexecuted integration tests.
 */
public abstract class IntegrationTestSupport {

    private static final String ENV_URL = System.getenv("TEST_DATABASE_URL");
    private static final String ENV_USER = System.getenv().getOrDefault("TEST_DATABASE_USER", "bankops");
    private static final String ENV_PASSWORD = System.getenv().getOrDefault("TEST_DATABASE_PASSWORD", "bankops-local");

    private static PostgreSQLContainer<?> container;
    private static String jdbcUrl;
    private static String username;
    private static String password;
    private static String unavailableReason;

    static {
        if (ENV_URL != null && !ENV_URL.isBlank()) {
            jdbcUrl = ENV_URL;
            username = ENV_USER;
            password = ENV_PASSWORD;
        } else {
            try {
                if (DockerClientFactory.instance().isDockerAvailable()) {
                    container = new PostgreSQLContainer<>(org.testcontainers.utility.DockerImageName
                        .parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"))
                        .withDatabaseName("bankops").withUsername("bankops").withPassword("bankops");
                    container.start();
                    jdbcUrl = container.getJdbcUrl();
                    username = container.getUsername();
                    password = container.getPassword();
                } else {
                    unavailableReason = "Docker is not available and TEST_DATABASE_URL is not set";
                }
            } catch (RuntimeException ex) {
                unavailableReason = "Testcontainers could not start: " + ex.getMessage();
            }
        }
    }

    public static boolean databaseAvailable() {
        return jdbcUrl != null;
    }

    public static String unavailableReason() {
        return unavailableReason;
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        if (jdbcUrl != null) {
            registry.add("spring.datasource.url", () -> jdbcUrl);
            registry.add("spring.datasource.username", () -> username);
            registry.add("spring.datasource.password", () -> password);
        }
        // Tests never contact Keycloak: the decoder is never invoked because the
        // requests carry no bearer token, and security is exercised with jwt().
        registry.add("bankops.jobs.enabled", () -> "false");
        registry.add("bankops.security.issuer-uri", () -> "http://localhost:8180/realms/bankops");
    }

    public static void requireDatabase() {
        org.junit.jupiter.api.Assertions.assertTrue(databaseAvailable(),
            "Integration database unavailable: " + unavailableReason);
    }
}
