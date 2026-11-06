package com.bankops.common;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import static org.assertj.core.api.Assertions.assertThat;

class JwtClaimsTest {
    @Test
    void ignoresKeycloakBuiltinRolesAndMalformedEntries() {
        Jwt jwt = Jwt.withTokenValue("test").header("alg", "none").subject("subject")
            .claim("realm_access", Map.of("roles", List.of("offline_access", "ADMIN", 7, "ADMIN", "AUDITOR")))
            .build();
        assertThat(JwtClaims.realmRoles(jwt)).containsExactly("ADMIN", "AUDITOR");
    }

    @Test
    void missingProfileFieldsHaveStableFallbacks() {
        Jwt jwt = Jwt.withTokenValue("test").header("alg", "none").subject("subject").build();
        assertThat(JwtClaims.email(jwt)).isEqualTo("subject");
        assertThat(JwtClaims.displayName(jwt)).isEqualTo("subject");
        assertThat(JwtClaims.realmRoles(jwt)).isEmpty();
        assertThat(JwtClaims.departmentCode(jwt)).isNull();
    }

    @Test
    void malformedRoleClaimGrantsNothing() {
        Jwt jwt = Jwt.withTokenValue("test").header("alg", "none").subject("subject")
            .claim("realm_access", Map.of("roles", "ADMIN")).build();
        assertThat(JwtClaims.realmRoles(jwt)).isEmpty();
    }
}
