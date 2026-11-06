package com.bankops.identity;

import com.bankops.common.UserContext;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.bankops.audit.AuditRecord;
import com.bankops.audit.AuditWriter;
import com.bankops.common.JwtClaims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.persistence.EntityManager;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class UserProvisioningService implements IdentityQuery {

    private static final Logger log = LoggerFactory.getLogger(UserProvisioningService.class);

    private final EntityManager entityManager;
    private final UserRepository users;
    private final DepartmentRepository departments;
    private final AuditWriter audit;

    public UserProvisioningService(UserRepository users, DepartmentRepository departments, AuditWriter audit, EntityManager entityManager) {
        this.entityManager = entityManager;
        this.users = users;
        this.departments = departments;
        this.audit = audit;
    }

    @Override
    @Transactional(readOnly = true)
    public long countUsers() {
        return users.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long countDepartments() {
        return departments.count();
    }

    /**
     * Creates or refreshes the local user for a validated token. The provisioning
     * itself is audited exactly once per creation and once per role change.
     */
    @Transactional
    public UserContext provision(Jwt jwt) {
        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new IllegalStateException("Token has no subject");
        }
        // Serialize first calls for this subject across API instances. PostgreSQL
        // releases this lock on transaction completion, including rollback.
        entityManager.createNativeQuery(
            "select 1 from pg_advisory_xact_lock(hashtextextended(?1, 0))")
            .setParameter(1, subject).getSingleResult();
        Instant now = Instant.now();
        Set<String> roles = new LinkedHashSet<>(JwtClaims.realmRoles(jwt));
        Department department = resolveDepartment(JwtClaims.departmentCode(jwt));

        Optional<UserAccount> existing = users.findBySubject(subject);
        if (existing.isEmpty()) {
            UserAccount account = new UserAccount(subject, JwtClaims.email(jwt), JwtClaims.displayName(jwt),
                department, roles, now);
            UserAccount saved = users.saveAndFlush(account);
            audit.append(AuditRecord.of(subject, account.getEmail(), "USER_PROVISIONED", "USER",
                saved.getId().toString(), Map.of("roles", roles.stream().sorted().toList())));
            return toUserContext(saved);
        }

        UserAccount account = existing.get();
        account.seenAt(now);
        account.updateProfile(JwtClaims.email(jwt), JwtClaims.displayName(jwt), department);
        boolean rolesChanged = account.replaceRoles(roles);
        if (rolesChanged) {
            audit.append(AuditRecord.of(subject, account.getEmail(), "USER_ROLES_CHANGED", "USER",
                account.getId().toString(), Map.of("roles", roles.stream().sorted().toList())));
        }
        return toUserContext(account);
    }

    private Department resolveDepartment(String code) {
        if (code == null) {
            return null;
        }
        return departments.findByCode(code).orElseGet(() -> {
            log.warn("Token references unknown department code '{}'; user gets no department", code);
            return null;
        });
    }

    private UserContext toUserContext(UserAccount account) {
        List<String> roles = account.getRoles().stream().sorted().toList();
        Department department = account.getDepartment();
        return new UserContext(
            account.getId(),
            account.getSubject(),
            account.getEmail(),
            account.getDisplayName(),
            department == null ? null : department.getCode(),
            department == null ? null : department.getName(),
            roles);
    }
}
