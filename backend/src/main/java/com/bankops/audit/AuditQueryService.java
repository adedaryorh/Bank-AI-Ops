package com.bankops.audit;
import com.bankops.common.UserContext;
import com.bankops.common.PageResponse;
import com.bankops.workspace.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.UUID;
import static com.bankops.workspace.WorkspaceQueries.*;
@Service
public class AuditQueryService {
    public record Entry(UUID id,Instant occurredAt,String actorEmail,String action,String targetType,String targetId,String outcome,String correlationId) {}
    private final JdbcTemplate db;private final WorkspaceAccess access;
    public AuditQueryService(JdbcTemplate db,WorkspaceAccess access) { this.db=db;this.access=access; }
    public PageResponse<Entry> list(UserContext user,int page,int size) {
        access.roles(user,"ADMIN","AUDITOR");int offset=offset(page,size);
        long count=db.queryForObject("select count(*) from audit_events",Long.class);
        var rows=db.query("select * from audit_events order by occurred_at desc,id limit ? offset ?",(r,i)->new Entry(r.getObject("id",UUID.class),instant(r,"occurred_at"),
            r.getString("actor_email"),r.getString("action"),r.getString("target_type"),r.getString("target_id"),r.getString("outcome"),r.getString("correlation_id")),size,offset);
        return page(rows,page,size,count);
    }
}
