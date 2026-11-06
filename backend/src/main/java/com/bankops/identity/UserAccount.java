package com.bankops.identity;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "users")
public class UserAccount {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, unique = true)
    private String subject;

    @Column(nullable = false)
    private String email;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role", nullable = false)
    private Set<String> roles = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    protected UserAccount() {
    }

    public UserAccount(String subject, String email, String displayName, Department department,
                       Set<String> roles, Instant createdAt) {
        this.subject = subject;
        this.email = email;
        this.displayName = displayName;
        this.department = department;
        this.roles = new LinkedHashSet<>(roles);
        this.createdAt = createdAt;
        this.lastSeenAt = createdAt;
    }

    public void seenAt(Instant instant) {
        this.lastSeenAt = instant;
    }

    public void updateProfile(String email, String displayName, Department department) {
        this.email = email;
        this.displayName = displayName;
        this.department = department;
    }

    public boolean replaceRoles(Set<String> newRoles) {
        if (roles.equals(newRoles)) {
            return false;
        }
        roles.clear();
        roles.addAll(newRoles);
        return true;
    }

    public UUID getId() {
        return id;
    }

    public String getSubject() {
        return subject;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Department getDepartment() {
        return department;
    }

    public Set<String> getRoles() {
        return Set.copyOf(roles);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }
}
