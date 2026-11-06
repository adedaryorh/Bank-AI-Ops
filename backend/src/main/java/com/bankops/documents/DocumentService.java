package com.bankops.documents;
import com.bankops.common.WorkspaceException;

import com.bankops.common.UserContext;
import com.bankops.common.PageResponse;
import com.bankops.workspace.*;
import com.bankops.cases.CaseWorkspace;
import com.bankops.storage.*;
import com.bankops.ai.ContentSafety;
import com.bankops.audit.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;
import java.util.*;
import java.time.Instant;
import static com.bankops.workspace.WorkspaceQueries.*;

@Service
public class DocumentService {
    public record Document(UUID id,UUID caseId,String purpose,String filename,String sha256,long sizeBytes,String status,
        Map<String,String> extractedFields,String errorMessage,Instant createdAt) {}
    public record Download(String filename,byte[] data) {}
    private final JdbcTemplate db;private final WorkspaceAccess access;private final CaseWorkspace cases;
    private final ObjectStorage storage;private final AuditWriter audit;private final ObjectMapper json;
    public DocumentService(JdbcTemplate db,WorkspaceAccess access,CaseWorkspace cases,ObjectStorage storage,AuditWriter audit,ObjectMapper json) {
        this.db=db;this.access=access;this.cases=cases;this.storage=storage;this.audit=audit;this.json=json;
    }
    @SuppressWarnings("unchecked")
    private Document map(java.sql.ResultSet r,int i) throws java.sql.SQLException {
        Map<String,String> fields=json.readValue(r.getString("extracted_fields"),Map.class);
        return new Document(r.getObject("id",UUID.class),r.getObject("case_id",UUID.class),r.getString("purpose"),r.getString("filename"),
            r.getString("sha256"),r.getLong("size_bytes"),r.getString("status"),fields,r.getString("error_message"),instant(r,"created_at"));
    }
    private void readRole(UserContext user) { access.roles(user,"ADMIN","OPERATIONS_OFFICER","COMPLIANCE_ANALYST","AML_ANALYST","SUPERVISOR","AUDITOR"); }
    @Transactional(readOnly=true)
    public PageResponse<Document> list(UserContext user,int page,int size,UUID caseId,String purpose) {
        readRole(user);int offset=offset(page,size);
        if(caseId!=null)cases.readable(user,caseId);
        if(purpose!=null && !List.of("CASE","POLICY").contains(purpose)) throw new IllegalArgumentException("Invalid document purpose.");
        // Published policies are shared internally; case documents remain department-scoped.
        String where=" where (? or purpose='POLICY' or department_code=any(string_to_array(?,','))) and (cast(? as uuid) is null or case_id=?) and (cast(? as text) is null or purpose=?)";
        Object[] args={access.global(user),access.departmentScope(user),caseId,caseId,purpose,purpose};
        long count=db.queryForObject("select count(*) from documents"+where,Long.class,args);
        var rows=db.query("select * from documents"+where+" order by created_at desc,id limit ? offset ?",this::map,
            access.global(user),access.departmentScope(user),caseId,caseId,purpose,purpose,size,offset);
        return page(rows,page,size,count);
    }
    @Transactional
    public Document upload(UserContext user,MultipartFile file,String purpose,UUID caseId,boolean synthetic) {
        access.roles(user,"ADMIN","OPERATIONS_OFFICER","COMPLIANCE_ANALYST","AML_ANALYST","SUPERVISOR");
        if(!synthetic)throw new IllegalArgumentException("Confirm that the document contains synthetic data only.");
        if(!List.of("CASE","POLICY").contains(purpose))throw new IllegalArgumentException("Invalid document purpose.");
        String department=access.department(user);
        if("POLICY".equals(purpose)) {
            access.roles(user,"ADMIN","COMPLIANCE_ANALYST");
            if(caseId!=null)throw new IllegalArgumentException("Policy cannot belong to a case.");
        } else {
            if(caseId==null)throw new IllegalArgumentException("Case ID is required.");
            department=cases.writable(user,caseId).department();
        }
        String filename=file.getOriginalFilename();
        if(filename==null || !filename.matches("[A-Za-z0-9][A-Za-z0-9._ -]{0,175}\\.txt") || filename.contains(".."))
            throw new IllegalArgumentException("Use a simple .txt filename, up to 180 characters.");
        if(file.isEmpty() || file.getSize()>1048576)throw new IllegalArgumentException("Upload a text document between 1 byte and 1 MiB.");
        byte[] bytes;
        try { bytes=file.getBytes(); } catch(java.io.IOException ex) { throw new IllegalArgumentException("Unable to read upload."); }
        ContentSafety.text(bytes);
        UUID id=UUID.randomUUID();String key=id+"/source.txt";
        storage.put(key,bytes);
        // MinIO is outside the database transaction. Rollback cleanup removes an uploaded orphan.
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
            new org.springframework.transaction.support.TransactionSynchronization() {
                @Override public void afterCompletion(int status) {
                    if(status!=STATUS_COMMITTED)try { storage.delete(key); } catch(RuntimeException ignored) { /* orphan can be reconciled by key */ }
                }
            });
        db.update("insert into documents(id,case_id,purpose,filename,object_key,sha256,size_bytes,owner_subject,department_code) values (?,?,?,?,?,?,?,?,?)",
            id,caseId,purpose,filename,key,MinioObjectStorage.sha256(bytes),bytes.length,user.subject(),department);
        db.update("insert into processing_jobs(id,document_id) values (?,?)",UUID.randomUUID(),id);
        audit.append(AuditRecord.of(user.subject(),user.email(),"DOCUMENT_UPLOADED","DOCUMENT",id.toString(),Map.of("purpose",purpose)));
        return db.query("select * from documents where id=?",this::map,id).getFirst();
    }
    @Transactional
    public Download download(UserContext user,UUID id) {
        readRole(user);
        var rows=db.queryForList("select * from documents where id=?",id);
        if(rows.isEmpty())throw WorkspaceException.missing();var row=rows.getFirst();
        if(!"POLICY".equals(row.get("purpose"))) access.department(user,(String)row.get("department_code"));
        byte[] data=storage.get((String)row.get("object_key"));
        if(!MinioObjectStorage.sha256(data).equals(row.get("sha256"))) throw new WorkspaceException(org.springframework.http.HttpStatus.BAD_GATEWAY,"Stored document checksum mismatch.");
        audit.append(AuditRecord.of(user.subject(),user.email(),"DOCUMENT_DOWNLOADED","DOCUMENT",id.toString(),Map.of()));
        return new Download((String)row.get("filename"),data);
    }
}
