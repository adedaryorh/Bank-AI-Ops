package com.bankops.documents;

import com.bankops.ai.*;
import com.bankops.storage.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import java.util.*;

@Service
public class DocumentJobs {
    public record Job(UUID id,UUID documentId,UUID token,int attempts,String key,String owner,String purpose,String sha256) {}
    private final JdbcTemplate db;private final ObjectMapper json;private final AiExecutionService executions;
    public DocumentJobs(JdbcTemplate db,ObjectMapper json,AiExecutionService executions) { this.db=db;this.json=json;this.executions=executions; }
    @Transactional
    public Optional<Job> claim() {
        db.update("update processing_jobs set status='FAILED',last_error='Processing lease expired after final attempt.' where status='RUNNING' and locked_until<now() and attempts>=3");
        db.update("update documents set status='FAILED',error_message='Processing failed after three attempts.' where id in (select document_id from processing_jobs where status='FAILED') and status not in ('READY','BLOCKED')");
        UUID token=UUID.randomUUID();
        var jobs=db.query("""
            update processing_jobs set status='RUNNING',attempts=attempts+1,locked_until=now()+interval '2 minutes',lease_token=?
            where id=(select id from processing_jobs where attempts<3 and available_at<=now()
                and (status='QUEUED' or (status='RUNNING' and locked_until<now()))
                order by created_at for update skip locked limit 1)
            returning id,document_id,attempts
            """,(r,i)->new Object[]{r.getObject("id",UUID.class),r.getObject("document_id",UUID.class),r.getInt("attempts")},token);
        if(jobs.isEmpty())return Optional.empty();Object[] job=jobs.getFirst();UUID doc=(UUID)job[1];
        db.update("update documents set status='PROCESSING',error_message=null where id=?",doc);
        return Optional.of(db.query("select * from documents where id=?",(r,i)->new Job((UUID)job[0],doc,token,(Integer)job[2],r.getString("object_key"),
            r.getString("owner_subject"),r.getString("purpose"),r.getString("sha256")),doc).getFirst());
    }
    @Transactional
    public void complete(Job job,String text,long duration) {
        // Fencing token prevents a worker whose lease expired from overwriting a newer result.
        if(!owns(job))return;
        boolean blocked=ContentSafety.unsafe(text);
        Map<String,String> fields=blocked?Map.of():DeterministicExtraction.extract(text);
        if(!blocked && "POLICY".equals(job.purpose())) {
            db.update("delete from knowledge_chunks where document_id=?",job.documentId());
            int ordinal=0;
            for(String chunk:DeterministicExtraction.chunks(text))
                db.update("insert into knowledge_chunks(id,document_id,ordinal,content) values (?,?,?,?)",UUID.randomUUID(),job.documentId(),ordinal++,chunk);
        }
        db.update("update documents set status=?,extracted_fields=cast(? as jsonb),error_message=? where id=?",
            blocked?"BLOCKED":"READY",json.writeValueAsString(fields),blocked?"Instruction-like content detected; excluded from policy retrieval.":null,job.documentId());
        db.update("update processing_jobs set status='DONE',locked_until=null,last_error=null where id=? and lease_token=?",job.id(),job.token());
        executions.record(job.owner(),"DOCUMENT_EXTRACTION","extract-fields-v1","deterministic-rules-v1",blocked?"BLOCKED":"SUCCESS",duration,null);
    }
    @Transactional
    public void failed(Job job) {
        if(!owns(job))return;
        boolean terminal=job.attempts()>=3;
        db.update("update processing_jobs set status=?,available_at=now()+interval '15 seconds',locked_until=null,last_error='Document processing unavailable.' where id=? and lease_token=?",
            terminal?"FAILED":"QUEUED",job.id(),job.token());
        db.update("update documents set status=?,error_message=? where id=?",terminal?"FAILED":"QUEUED",terminal?"Processing failed after three attempts. Check object storage.":null,job.documentId());
        executions.record(job.owner(),"DOCUMENT_EXTRACTION","extract-fields-v1","deterministic-rules-v1","FAILED",0,null);
    }
    private boolean owns(Job job) {
        var tokens=db.queryForList("select lease_token from processing_jobs where id=? and status='RUNNING' and locked_until>now() for update",UUID.class,job.id());
        return !tokens.isEmpty() && job.token().equals(tokens.getFirst());
    }
}
