package com.bankops.policy;

import com.bankops.ai.*;
import com.bankops.common.UserContext;
import com.bankops.workspace.*;
import com.bankops.audit.*;
import jakarta.validation.constraints.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class PolicyService {
    public record Question(@NotBlank @Size(max=1500) String question) {}
    public record Citation(UUID chunkId,UUID documentId,String filename,String quote) {}
    public record Answer(UUID id,String answer,boolean abstained,List<Citation> citations,UUID executionId,String model) {}
    private final JdbcTemplate db;private final WorkspaceAccess access;private final EvidenceSelector model;
    private final AiExecutionService executions;private final AuditWriter audit;
    public PolicyService(JdbcTemplate db,WorkspaceAccess access,EvidenceSelector model,AiExecutionService executions,AuditWriter audit) {
        this.db=db;this.access=access;this.model=model;this.executions=executions;this.audit=audit;
    }
    @Transactional
    public Answer ask(UserContext user,Question input) {
        access.roles(user,"ADMIN","OPERATIONS_OFFICER","COMPLIANCE_ANALYST","AML_ANALYST","SUPERVISOR","AUDITOR");
        long start=System.nanoTime();List<EvidenceSelector.Candidate> selected=List.of();String status="ABSTAINED";Long tokens=null;
        if(ContentSafety.unsafe(input.question()))status="BLOCKED";
        else {
            var terms=Arrays.stream(input.question().toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+"))
                .filter(t->t.length()>2 && !Set.of("the","what","which","are","for","and","with","how","does","die","der","das","und","ist","ein").contains(t))
                .distinct().limit(20).toList();
            if(!terms.isEmpty()) {
                var candidates=db.query("""
                    select c.id,c.document_id,d.filename,c.content from knowledge_chunks c
                    join documents d on d.id=c.document_id
                    where d.purpose='POLICY' and d.status='READY'
                      and c.search_vector @@ websearch_to_tsquery('simple',?)
                    order by ts_rank(c.search_vector,websearch_to_tsquery('simple',?)) desc,c.id limit 5
                    """,(r,i)->new EvidenceSelector.Candidate(r.getObject("id",UUID.class),r.getObject("document_id",UUID.class),r.getString("filename"),r.getString("content")),
                    String.join(" OR ",terms),String.join(" OR ",terms));
                candidates=candidates.stream().filter(c->!ContentSafety.unsafe(c.content())).toList();
                if(!candidates.isEmpty()) {
                    try {
                        var result=model.select(input.question(),candidates);tokens=result.tokens();
                        selected=AnswerValidation.validate(result.chunkIds(),candidates);
                        status=selected.isEmpty()?"ABSTAINED":"SUCCESS";
                    } catch(RuntimeException ex) { status="INVALID_OR_UNAVAILABLE";selected=List.of(); }
                }
            }
        }
        UUID execution=executions.record(user.subject(),"POLICY_EVIDENCE_SELECTION","policy-evidence-v1",model.model(),status,(System.nanoTime()-start)/1000000,tokens);
        boolean abstained=selected.isEmpty();String answer=abstained?
            "No supported answer is available from the indexed synthetic policies. Ask a compliance officer; no decision has been made.":
            "The following excerpts from stored synthetic policies match your question. Review the source before applying a policy; no decision has been made.";
        UUID id=UUID.randomUUID();db.update("insert into policy_answers(id,actor_subject,question,answer,abstained,execution_id) values (?,?,?,?,?,?)",
            id,user.subject(),input.question(),answer,abstained,execution);
        for(var chunk:selected) db.update("insert into answer_citations(answer_id,chunk_id) values (?,?)",id,chunk.id());
        audit.append(AuditRecord.of(user.subject(),user.email(),"POLICY_QUESTION_ANSWERED","POLICY_ANSWER",id.toString(),Map.of("abstained",abstained)));
        return new Answer(id,answer,abstained,selected.stream().map(c->new Citation(c.id(),c.documentId(),c.filename(),c.content())).toList(),execution,model.model());
    }
}
