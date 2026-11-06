package com.bankops.cases;
import com.bankops.common.UserContext;
import java.util.UUID;

public interface CaseWorkspace {
    record Scope(UUID id, String department, String status) {}
    Scope readable(UserContext user, UUID id);
    Scope writable(UserContext user, UUID id);
    UUID openAml(UserContext user, String title, String description, String reference);
}
