package com.bankops.ai;
import com.bankops.common.PageResponse;
import com.bankops.common.UserContext;
import com.bankops.workspace.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.Instant;
import java.util.UUID;
import static com.bankops.workspace.WorkspaceQueries.*;
@Service
public class AiExecutionService {
    public record Execution(UUID id,String operation,String promptVersion,String model,String status,long durationMs,Long tokenUsage,Instant createdAt) {}
    private final JdbcTemplate db;private final WorkspaceAccess access;
    public AiExecutionService(JdbcTemplate db,WorkspaceAccess access) { this.db=db;this.access=access; }
    @Transactional
    public UUID record(String actor,String operation,String prompt,String model,String status,long duration,Long tokens) {
        UUID id=UUID.randomUUID();db.update("insert into ai_executions(id,actor_subject,operation,prompt_version,model,status,duration_ms,token_usage) values (?,?,?,?,?,?,?,?)",
            id,actor,operation,prompt,model,status,Math.max(0,duration),tokens);return id;
    }
    public PageResponse<Execution> list(UserContext user,int page,int size) {
        access.roles(user,"ADMIN","AUDITOR","COMPLIANCE_ANALYST","OPERATIONS_OFFICER","AML_ANALYST","SUPERVISOR");
        int offset=offset(page,size);boolean all=access.global(user);
        long count=db.queryForObject("select count(*) from ai_executions where (? or actor_subject=?)",Long.class,all,user.subject());
        var rows=db.query("select * from ai_executions where (? or actor_subject=?) order by created_at desc,id limit ? offset ?",(r,i)->new Execution(
            r.getObject("id",UUID.class),r.getString("operation"),r.getString("prompt_version"),r.getString("model"),r.getString("status"),r.getLong("duration_ms"),
            (Long)r.getObject("token_usage"),instant(r,"created_at")),all,user.subject(),size,offset);
        return page(rows,page,size,count);
    }
}
