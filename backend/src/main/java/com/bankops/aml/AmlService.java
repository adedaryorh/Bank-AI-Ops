package com.bankops.aml;
import com.bankops.common.WorkspaceException;
import com.bankops.common.UserContext;
import com.bankops.common.PageResponse;
import com.bankops.workspace.*;
import com.bankops.cases.CaseWorkspace;
import com.bankops.audit.*;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;
import static com.bankops.workspace.WorkspaceQueries.*;
@Service
@Transactional
public class AmlService {
    public record Alert(UUID id,UUID caseId,String account,String rule,String rationale,String status,Instant createdAt) {}
    public record Simulation(UUID id,int transactionCount,int alertCount) {}
    private final JdbcTemplate db;private final WorkspaceAccess access;private final CaseWorkspace cases;private final AuditWriter audit;
    public AmlService(JdbcTemplate db,WorkspaceAccess access,CaseWorkspace cases,AuditWriter audit) { this.db=db;this.access=access;this.cases=cases;this.audit=audit; }
    public Simulation simulate(UserContext user) {
        access.roles(user,"ADMIN","AML_ANALYST");String department=access.department(user);
        // Bound accidental repeated clicks without allowing untrusted input to create arbitrary transactions.
        if(db.queryForObject("select count(*) from aml_simulations where actor_subject=? and created_at>now()-interval '1 minute'",Long.class,user.subject())>=5)
            throw new WorkspaceException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,"Wait a minute before generating another synthetic simulation.");
        UUID id=UUID.randomUUID();db.update("insert into aml_simulations(id,actor_subject,department_code) values (?,?,?)",id,user.subject(),department);
        var transactions=AmlRules.demo(Instant.now());
        for(var t:transactions)db.update("insert into synthetic_transactions(id,simulation_id,account_reference,amount,currency,occurred_at) values (?,?,?,?,?,?)",
            UUID.randomUUID(),id,t.account(),t.amount(),t.currency(),java.sql.Timestamp.from(t.at()));
        var findings=AmlRules.evaluate(transactions);
        for(var f:findings) {
            UUID caseId=cases.openAml(user,"Review "+f.account(),f.rationale(),"SYN-"+id.toString().substring(0,8));
            db.update("insert into aml_alerts(id,simulation_id,case_id,account_reference,rule_code,rationale) values (?,?,?,?,?,?)",
                UUID.randomUUID(),id,caseId,f.account(),f.code(),f.rationale());
        }
        audit.append(AuditRecord.of(user.subject(),user.email(),"AML_SYNTHETIC_SIMULATION","AML_SIMULATION",id.toString(),Map.of("transactions",transactions.size(),"alerts",findings.size())));
        return new Simulation(id,transactions.size(),findings.size());
    }
    public record TransactionEvidence(UUID id,UUID simulationId,String account,java.math.BigDecimal amount,String currency,Instant occurredAt) {}
    public java.util.List<TransactionEvidence> evidence(UserContext user,UUID caseId) {
        access.roles(user,"ADMIN","AML_ANALYST","SUPERVISOR","AUDITOR");cases.readable(user,caseId);
        return db.query("""
            select t.* from synthetic_transactions t join aml_alerts a
              on a.simulation_id=t.simulation_id and a.account_reference=t.account_reference
            where a.case_id=? order by t.occurred_at,t.id
            """,(r,i)->new TransactionEvidence(r.getObject("id",UUID.class),r.getObject("simulation_id",UUID.class),r.getString("account_reference"),
                r.getBigDecimal("amount"),r.getString("currency"),instant(r,"occurred_at")),caseId);
    }
    public PageResponse<Alert> list(UserContext user,int page,int size) {
        access.roles(user,"ADMIN","AML_ANALYST","SUPERVISOR","AUDITOR");int offset=offset(page,size);
        String where=" from aml_alerts a join operation_cases c on c.id=a.case_id where (? or c.department_code=any(string_to_array(?,',')))";
        long count=db.queryForObject("select count(*)"+where,Long.class,access.global(user),access.departmentScope(user));
        var rows=db.query("select a.*,c.status"+where+" order by a.created_at desc,a.id limit ? offset ?",(r,i)->new Alert(r.getObject("id",UUID.class),r.getObject("case_id",UUID.class),
            r.getString("account_reference"),r.getString("rule_code"),r.getString("rationale"),r.getString("status"),instant(r,"created_at")),access.global(user),access.departmentScope(user),size,offset);
        return page(rows,page,size,count);
    }
}
