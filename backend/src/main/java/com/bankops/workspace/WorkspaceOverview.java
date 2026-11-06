package com.bankops.workspace;
import com.bankops.common.UserContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.Map;
@Service
public class WorkspaceOverview {
    private final JdbcTemplate db;private final WorkspaceAccess access;
    public WorkspaceOverview(JdbcTemplate db,WorkspaceAccess access) { this.db=db;this.access=access; }
    public Map<String,Long> counts(UserContext user) {
        boolean all=access.global(user);String scope=access.departmentScope(user);
        long open=db.queryForObject("select count(*) from operation_cases where status<>'CLOSED' and (? or department_code=any(string_to_array(?,',')))",Long.class,all,scope);
        long reviews=db.queryForObject("select count(*) from operation_cases where status='IN_REVIEW' and (? or department_code=any(string_to_array(?,',')))",Long.class,all,scope);
        long docs=db.queryForObject("select count(*) from documents where status='READY' and (? or purpose='POLICY' or department_code=any(string_to_array(?,',')))",Long.class,all,scope);
        return Map.of("cases.open",open,"reviews.pending",reviews,"documents.ready",docs);
    }
}
