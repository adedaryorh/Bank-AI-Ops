package com.bankops;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Authorization is enforced by the backend. Each protected endpoint is checked
 * three ways: unauthenticated, wrong role, right role.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityAuthorizationTest extends IntegrationTestSupport {

    @Autowired
    MockMvc mvc;

    @BeforeAll
    static void require() {
        requireDatabase();
    }

    private static MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder builder,
                                                    String subject, String department, List<String> roles) {
        return builder.with(jwt()
            .jwt(token -> token
                .subject(subject)
                .claim("email", subject)
                .claim("name", "Test " + subject)
                .claim("department", department)
                .claim("realm_access", Map.of("roles", roles)))
            .authorities(roles.stream().map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role)).toList()));
    }

    @Test
    void meIsUnauthorizedWithoutToken() throws Exception {
        mvc.perform(get("/api/v1/auth/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void meReturnsProvisionedUserForAnyRole() throws Exception {
        String subject = "ops-" + java.util.UUID.randomUUID() + "@bankops.local";
        mvc.perform(as(get("/api/v1/auth/me"), subject, "OPS", List.of("OPERATIONS_OFFICER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.subject").value(subject))
            .andExpect(jsonPath("$.department").value("OPS"))
            .andExpect(jsonPath("$.roles[0]").value("OPERATIONS_OFFICER"));
    }

    @Test
    void adminEndpointRejectsWrongRole() throws Exception {
        mvc.perform(as(get("/api/v1/admin/users"), "auditor@bankops.local", "AUDIT", List.of("AUDITOR")))
            .andExpect(status().isForbidden())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void adminEndpointRejectsAnonymous() throws Exception {
        mvc.perform(get("/api/v1/admin/users"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void adminEndpointAllowsAdmin() throws Exception {
        mvc.perform(as(get("/api/v1/admin/users?page=0&size=5"), "admin@bankops.local", "OPS",
                List.of("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.size").value(5))
            .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void dashboardIsRoleAwareForAuditor() throws Exception {
        mvc.perform(as(get("/api/v1/dashboard/summary"), "auditor2@bankops.local", "AUDIT",
                List.of("AUDITOR")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.metrics[0].available").value(true))
            .andExpect(jsonPath("$.widgets[0].key").value("audit.trail"))
            .andExpect(jsonPath("$.widgets[0].available").value(true))
            .andExpect(jsonPath("$.notice").value(org.hamcrest.Matchers.containsString("Synthetic data")));
    }

    @Test
    void dashboardEnablesCaseWorkspace() throws Exception {
        mvc.perform(as(get("/api/v1/dashboard/summary"), "ops2@bankops.local", "OPS",
                List.of("OPERATIONS_OFFICER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.widgets[0].key").value("operations.cases"))
            .andExpect(jsonPath("$.widgets[0].available").value(true));
    }

    @Test
    void correlationIdIsEchoed() throws Exception {
        mvc.perform(as(get("/api/v1/auth/me"), "corr@bankops.local", "OPS", List.of("OPERATIONS_OFFICER"))
                .header("X-Correlation-ID", "test-correlation-123"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Correlation-ID", "test-correlation-123"));
    }
    @Test
    void dashboardRejectsAnonymousAndUsersWithoutApplicationRoles() throws Exception {
        mvc.perform(get("/api/v1/dashboard/summary")).andExpect(status().isUnauthorized());
        mvc.perform(as(get("/api/v1/dashboard/summary"), "no-role", "OPS", List.of()))
            .andExpect(status().isForbidden());
        mvc.perform(as(get("/api/v1/auth/me"), "no-role", "OPS", List.of()))
            .andExpect(status().isForbidden());
    }

    @Test
    void operationsCannotReadOtherUsersAuditDetails() throws Exception {
        mvc.perform(as(get("/api/v1/dashboard/summary"), "ops-private", "OPS",
            List.of("OPERATIONS_OFFICER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recentAudit").isEmpty());
    }

    @Test
    void invalidPaginationReturnsBadRequest() throws Exception {
        for (String query : List.of("page=-1", "size=0", "size=101", "page=invalid")) {
            mvc.perform(as(get("/api/v1/admin/users?" + query), "admin-validation", "OPS", List.of("ADMIN")))
                .andExpect(status().isBadRequest());
        }
    }

}
