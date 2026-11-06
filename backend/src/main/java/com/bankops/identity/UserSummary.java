package com.bankops.identity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserSummary(
    UUID id,
    String subject,
    String email,
    String displayName,
    String department,
    List<String> roles,
    Instant createdAt,
    Instant lastSeenAt
) {

    static UserSummary from(UserAccount account) {
        Department department = account.getDepartment();
        return new UserSummary(
            account.getId(),
            account.getSubject(),
            account.getEmail(),
            account.getDisplayName(),
            department == null ? null : department.getCode(),
            account.getRoles().stream().sorted().toList(),
            account.getCreatedAt(),
            account.getLastSeenAt());
    }
}
