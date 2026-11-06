package com.bankops.identity;

import com.bankops.common.UserContext;

import java.util.List;
import java.util.UUID;

/** API representation of the caller. */
public record MeResponse(
    UUID id,
    String subject,
    String email,
    String displayName,
    String department,
    String departmentName,
    List<String> roles
) {

    public static MeResponse from(UserContext user) {
        return new MeResponse(user.id(), user.subject(), user.email(), user.displayName(),
            user.departmentCode(), user.departmentName(), user.roles());
    }
}
