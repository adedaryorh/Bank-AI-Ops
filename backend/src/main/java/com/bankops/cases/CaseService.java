package com.bankops.cases;
import com.bankops.common.WorkspaceException;

import com.bankops.audit.*;
import com.bankops.common.PageResponse;
import com.bankops.common.UserContext;
import com.bankops.workspace.*;
import jakarta.validation.constraints.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.Instant;
import static com.bankops.workspace.WorkspaceQueries.*;

@Service
@Transactional
public class CaseService implements CaseWorkspace {
    public record Create(@NotBlank @Size(max=180) String title, @NotNull @Pattern(regexp="OPERATIONS|COMPLIANCE") String kind,
        @NotBlank @Size(max=10000) String description, @NotBlank @Pattern(regexp="SYN-[A-Za-z0-9-]{1,76}") String reference,
        @AssertTrue boolean synthetic) {}
    public record Change(@Min(0) long version, @NotBlank @Size(max=4000) String note) {}
    public record Review(@Min(0) long version, @NotBlank @Pattern(regexp="CLOSE|RETURN") String decision,
        @NotBlank @Size(min=10,max=4000) String reason) {}
    public record Item(UUID id,String title,String kind,String description,String reference,String department,
        String creator,String status,long version,Instant createdAt,Instant updatedAt) {}
    public record Event(UUID id,String actor,String action,String note,Instant createdAt) {}
    public record Detail(Item item,List<Event> events) {}
    private final JdbcTemplate db; private final WorkspaceAccess access; private final AuditWriter audit;
    public CaseService(JdbcTemplate db,WorkspaceAccess access,AuditWriter audit) { this.db=db;this.access=access;this.audit=audit; }
    private Item map(java.sql.ResultSet r,int i) throws java.sql.SQLException {
        return new Item(r.getObject("id",UUID.class),r.getString("title"),r.getString("kind"),r.getString("description"),
            r.getString("synthetic_reference"),r.getString("department_code"),r.getString("creator_subject"),r.getString("status"),
            r.getLong("version"),instant(r,"created_at"),instant(r,"updated_at"));
    }
    private Item find(UserContext user,UUID id,boolean lock) {
        var rows=db.query("select * from operation_cases where id=?"+(lock?" for update":""),this::map,id);
        if(rows.isEmpty()) throw WorkspaceException.missing();
        access.department(user,rows.getFirst().department());return rows.getFirst();
    }
    @Override public Scope readable(UserContext user,UUID id) {
        access.roles(user,"ADMIN","OPERATIONS_OFFICER","COMPLIANCE_ANALYST","AML_ANALYST","SUPERVISOR","AUDITOR");
        Item item=find(user,id,false);return new Scope(id,item.department(),item.status());
    }
    @Override public Scope writable(UserContext user,UUID id) {
        access.roles(user,"ADMIN","OPERATIONS_OFFICER","COMPLIANCE_ANALYST","AML_ANALYST","SUPERVISOR");
        Item item=find(user,id,true);
        if(!List.of("OPEN","RETURNED").contains(item.status())) throw WorkspaceException.conflict("Only open or returned cases may be edited.");
        return new Scope(id,item.department(),item.status());
    }
    public PageResponse<Item> list(UserContext user,int page,int size,boolean reviewQueue) {
        readableRole(user);int offset=offset(page,size);
        String where=" where (? or department_code=any(string_to_array(?,','))) and (?=false or status='IN_REVIEW')";
        var args=new Object[]{access.global(user),access.departmentScope(user),reviewQueue};
        long count=db.queryForObject("select count(*) from operation_cases"+where,Long.class,args);
        var rows=db.query("select * from operation_cases"+where+" order by created_at desc,id limit ? offset ?",this::map,
            access.global(user),access.departmentScope(user),reviewQueue,size,offset);
        return page(rows,page,size,count);
    }
    private void readableRole(UserContext user) {
        access.roles(user,"ADMIN","OPERATIONS_OFFICER","COMPLIANCE_ANALYST","AML_ANALYST","SUPERVISOR","AUDITOR");
    }
    public Detail detail(UserContext user,UUID id) {
        readableRole(user);Item item=find(user,id,false);
        var events=db.query("select * from case_events where case_id=? order by created_at,id",(r,i)->new Event(
            r.getObject("id",UUID.class),r.getString("actor_subject"),r.getString("action"),r.getString("note"),instant(r,"created_at")),id);
        return new Detail(item,events);
    }
    public Item create(UserContext user,Create input) {
        access.roles(user,"ADMIN","OPERATIONS_OFFICER","COMPLIANCE_ANALYST");
        if(!input.synthetic() || !List.of("OPERATIONS","COMPLIANCE").contains(input.kind())) throw new IllegalArgumentException("Synthetic operations or compliance case required.");
        if("COMPLIANCE".equals(input.kind())) access.roles(user,"ADMIN","COMPLIANCE_ANALYST");
        UUID id=open(user,input.title(),input.kind(),input.description(),input.reference());return find(user,id,false);
    }
    private UUID open(UserContext user,String title,String kind,String description,String reference) {
        UUID id=UUID.randomUUID();
        db.update("insert into operation_cases(id,title,kind,description,synthetic_reference,department_code,creator_subject) values (?,?,?,?,?,?,?)",
            id,title,kind,description,reference,access.department(user),user.subject());
        event(user,id,"CREATED","Synthetic case created.");return id;
    }
    @Override public UUID openAml(UserContext user,String title,String description,String reference) {
        access.roles(user,"ADMIN","AML_ANALYST");return open(user,title,"AML",description,reference);
    }
    public Detail note(UserContext user,UUID id,Change input) {
        writable(user,id);Item item=find(user,id,true);version(item,input.version());
        db.update("update operation_cases set version=version+1,updated_at=now() where id=?",id);
        event(user,id,"NOTE_ADDED",input.note());return detail(user,id);
    }
    public Detail submit(UserContext user,UUID id,Change input) {
        access.roles(user,"ADMIN","OPERATIONS_OFFICER","COMPLIANCE_ANALYST","AML_ANALYST");
        writable(user,id);Item item=find(user,id,true);version(item,input.version());
        db.update("update operation_cases set status='IN_REVIEW',version=version+1,updated_at=now() where id=?",id);
        event(user,id,"SUBMITTED",input.note());return detail(user,id);
    }
    public Detail review(UserContext user,UUID id,Review input) {
        access.roles(user,"ADMIN","SUPERVISOR");Item item=find(user,id,true);version(item,input.version());
        if(!"IN_REVIEW".equals(item.status())) throw WorkspaceException.conflict("Case is not awaiting review.");
        String submitter=db.queryForObject("select actor_subject from case_events where case_id=? and action='SUBMITTED' order by created_at desc,id desc limit 1",String.class,id);
        if(user.subject().equals(item.creator()) || user.subject().equals(submitter))
            throw new org.springframework.security.access.AccessDeniedException("Independent reviewer required");
        if(!List.of("CLOSE","RETURN").contains(input.decision())) throw new IllegalArgumentException("Invalid decision.");
        db.update("update operation_cases set status=?,version=version+1,updated_at=now() where id=?",
            "CLOSE".equals(input.decision())?"CLOSED":"RETURNED",id);
        event(user,id,"REVIEW_"+input.decision(),input.reason());return detail(user,id);
    }
    private void version(Item item,long version) { if(item.version()!=version) throw WorkspaceException.conflict("Case changed. Reload before saving."); }
    private void event(UserContext user,UUID id,String action,String note) {
        db.update("insert into case_events(id,case_id,actor_subject,action,note) values (?,?,?,?,?)",UUID.randomUUID(),id,user.subject(),action,note);
        audit.append(AuditRecord.of(user.subject(),user.email(),"CASE_"+action,"CASE",id.toString(),Map.of()));
    }
}
