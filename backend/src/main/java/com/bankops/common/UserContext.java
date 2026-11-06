package com.bankops.common;

import java.util.List;
import java.util.UUID;

/** The authenticated caller as the platform knows them. Never a JPA entity. */
public record UserContext(
    UUID id,
    String subject,
    String email,
    String displayName,
    String departmentCode,
    String departmentName,
    List<String> roles
) {

    public static final String REQUEST_ATTRIBUTE = "bankops.currentUser";

    public boolean hasRole(String role) {
        return roles.contains(role);
    }
}
