package com.bankops.workspace;

import com.bankops.common.UserContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import java.util.Arrays;

@Component
public class WorkspaceAccess {
    private final org.springframework.jdbc.core.JdbcTemplate db;
    public WorkspaceAccess(org.springframework.jdbc.core.JdbcTemplate db) { this.db=db; }
    public java.util.Set<String> departments(UserContext user) {
        java.util.Set<String> result=new java.util.HashSet<>();
        if(user.departmentCode()!=null)result.add(user.departmentCode());
        if(user.hasRole("SUPERVISOR") && user.departmentCode()!=null)
            result.addAll(db.queryForList("select resource_department from department_review_scopes where supervisor_department=?",String.class,user.departmentCode()));
        return java.util.Set.copyOf(result);
    }
    public String departmentScope(UserContext user) { return String.join(",",departments(user)); }

    public void roles(UserContext user, String... roles) {
        if (user == null || Arrays.stream(roles).noneMatch(user::hasRole))
            throw new AccessDeniedException("Role required");
    }
    public boolean global(UserContext user) { return user.hasRole("ADMIN") || user.hasRole("AUDITOR"); }
    public void department(UserContext user, String department) {
        if (!global(user) && !departments(user).contains(department))
            throw new AccessDeniedException("Resource belongs to another department");
    }
    public String department(UserContext user) {
        if (user.departmentCode() == null) throw new AccessDeniedException("A department is required");
        return user.departmentCode();
    }
}
