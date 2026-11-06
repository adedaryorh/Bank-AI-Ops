package com.bankops.common;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.security.oauth2.jwt.Jwt;

/** Small helpers for reading Keycloak claims without leaking claim shapes. */
public final class JwtClaims {

    private static final java.util.Set<String> APPLICATION_ROLES = java.util.Set.of(
        "ADMIN", "OPERATIONS_OFFICER", "COMPLIANCE_ANALYST", "AML_ANALYST", "SUPERVISOR", "AUDITOR");

    private JwtClaims() {}

    //
    @SuppressWarnings("unchecked")
    public static List<String> realmRoles(Jwt jwt) {
        Object claim = jwt.getClaim("realm_access");
        if (claim instanceof Map<?, ?> map) {
            Object roles = map.get("roles");
            if (roles instanceof Collection<?> collection) {
                return collection.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .filter(APPLICATION_ROLES::contains)
                    .distinct()
                    .toList();
            }
        }
        return List.of();
    }

    /** Department code from the {@code department} user attribute. */
    public static String departmentCode(Jwt jwt) {
        Object claim = jwt.getClaim("department");
        return claim instanceof String value && !value.isBlank() ? value : null;
    }

    public static String email(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email != null && !email.isBlank()) {
            return email;
        }
        String username = jwt.getClaimAsString("preferred_username");
        return username != null && !username.isBlank() ? username : jwt.getSubject();
    }

    public static String displayName(Jwt jwt) {
        String name = jwt.getClaimAsString("name");
        if (name != null && !name.isBlank()) {
            return name;
        }
        String given = jwt.getClaimAsString("given_name");
        String family = jwt.getClaimAsString("family_name");
        if (given != null || family != null) {
            return ((given == null ? "" : given) + " " + (family == null ? "" : family)).trim();
        }
        String username = jwt.getClaimAsString("preferred_username");
        return username != null ? username : String.valueOf(jwt.getSubject());
    }
}
